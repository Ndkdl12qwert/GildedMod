package com.gildedmod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CP（CallProtection）—— 启动期自检。
 *
 * <h3>三层</h3>
 * <ul>
 *     <li><b>C1 完整性</b>：把 agent 在 transform 时看到的<b>运行时字节</b>哈希，
 *         与构建时写进 jar 的清单比对 → 发现"我们的类在加载时被别人改过"</li>
 *     <li><b>C2 自愈</b>：对 {@code com.gildedmod.**} 做 {@code RetransformClasses}，
 *         从 jar 里的原始字节重建 → 抹掉别人的改动（{@link LiuCore#nativeSelfHeal()}）</li>
 *     <li><b>C3 生效</b>：反射检查我们的 Mixin 注入方法是否真的长在目标类上
 *         → 不再"编译过了但注入没生效"却毫无提示</li>
 * </ul>
 *
 * <h3>为什么值得做</h3>
 * 对面（pig2mod 这类）既有 {@code ClassFileTransformer} 又有 {@code ILaunchPluginService}
 * 和 Mixin 插件，我们的类在理论上是可以被改的。三层里 C2 是唯一能"修回来"的，
 * C1 负责让篡改可见，C3 负责让注入失败可见。
 */
public final class CP {

    private static boolean done = false;

    private CP() {
    }

    /** 只跑一次（幂等） */
    public static void startup() {
        if (done) {
            return;
        }
        done = true;

        int healed = -99;
        try {
            healed = LiuCore.nativeSelfHeal();
        } catch (Throwable t) {
            System.out.println("[CP] 自愈调用失败: " + t);
        }

        int mismatched = verifyIntegrity();
        int missing = verifyInjections();

        System.out.println("[CP] ===== CallProtection 启动自检 =====");
        System.out.println("[CP] C2 自愈: " + healed + " 个类从原始字节重建"
                + (healed > 0 ? "  ✓" : "  （0 或失败，见上方 [CP] 日志）"));
        System.out.println("[CP] C1 完整性: " + (mismatched < 0
                ? "没有清单（构建时未生成 cp-manifest.txt）"
                : mismatched == 0 ? "全部一致 ✓" : (mismatched + " 个类被改过 ✗")));
        System.out.println("[CP] C3 生效: " + (missing == 0 ? "全部注入在位 ✓" : (missing + " 处注入缺失 ✗")));
        System.out.println("[CP] ================================");
    }

    // ---------------------------------------------------------------- C1

    /**
     * 比对运行时哈希与构建清单。
     *
     * @return 不一致的类数量；返回 -1 表示清单不存在（没做 C1）
     */
    private static int verifyIntegrity() {
        Map<String, String> expected = loadManifest();
        if (expected.isEmpty()) {
            return -1;
        }

        // 跨 classloader：agent 侧写成文件，这里读文件 ✓
        Map<String, String> actual = loadRuntimeFile();
        if (actual.isEmpty()) {
            actual = CpHash.snapshot();
        }
        if (actual.isEmpty()) {
            // agent 没记录（可能 agent 没挂上）→ 不能判定"没被改"
            System.out.println("[CP] C1: agent 没有记录到运行时字节（agent 未生效？）");
            return expected.size();
        }

        int bad = 0;
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String got = actual.get(e.getKey());
            if (got == null) {
                continue;   // 还没加载，不算不一致
            }
            if (!got.equalsIgnoreCase(e.getValue())) {
                ++bad;
                System.out.println("[CP] C1 不一致: " + e.getKey()
                        + " 期望=" + e.getValue() + " 实际=" + got);
            }
        }
        return bad;
    }

    /** 读 agent 落下的运行时哈希（跨 classloader 桥接） */
    private static Map<String, String> loadRuntimeFile() {
        Map<String, String> map = new LinkedHashMap<>();
        java.io.File f = new java.io.File("config/gildedmod/cp-runtime.txt");
        if (!f.isFile()) {
            return map;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (Throwable ignored) {
        }
        return map;
    }

    /** 读 jar 里的 com/gildedmod/cp-manifest.txt（构建期由 Gradle 生成） */
    private static Map<String, String> loadManifest() {
        Map<String, String> map = new LinkedHashMap<>();
        try (InputStream in = CP.class.getResourceAsStream("/com/gildedmod/cp-manifest.txt")) {
            if (in == null) {
                return map;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (Throwable ignored) {
            // 没有清单就跳过 C1
        }
        return map;
    }

    // ---------------------------------------------------------------- C3

    /**
     * 检查我们的 Mixin 注入方法是否真的存在。
     *
     * <p>Mixin 的 {@code @Inject} 私有方法会作为<b>真实方法</b>长在目标类上，
     * 名字保持我们写的（因为这些名字本来就是唯一的）。
     * 所以反射找得到 = 注入成功 ✓ 找不到 = 注入失败（或被别人取消了）✗
     *
     * @return 缺失的处数
     */
    private static int verifyInjections() {
        int missing = 0;
        missing += check(net.minecraft.world.entity.Entity.class, "liuAllowRemoval", "LiuArmorRemoveMixin→Entity");
        missing += check(net.minecraft.world.entity.LivingEntity.class, "liuArmorSetHealth", "LiuArmorMixin→LivingEntity");
        missing += check(net.minecraft.world.entity.LivingEntity.class, "liuArmorDie", "LiuArmorMixin→LivingEntity(die)");
        missing += check(net.minecraft.world.entity.player.Player.class, "liuAttack", "BlackGoldSwordMixin→Player");
        missing += check(net.minecraft.server.level.ServerLevel.class, "liuBlockForeignEntity", "Pig2BlockMixin→ServerLevel");
        return missing;
    }

    private static int check(Class<?> owner, String methodName, String what) {
        try {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Method m : owner.getDeclaredMethods()) {
                String n = m.getName();
                names.add(n);
                // Mixin 对 @Inject 的私有方法可能改名（加前缀），所以做包含匹配
                if (n.equals(methodName) || n.endsWith(methodName) || n.contains(methodName)) {
                    return 0;
                }
            }
            System.out.println("[CP] C3 缺失: " + what + " （找 " + methodName + "）");
            StringBuilder cand = new StringBuilder();
            int shown = 0;
            for (String n : names) {
                if (n.contains("liu") || n.contains("gildedmod") || n.contains("Liu")) {
                    if (shown++ >= 8) break;
                    cand.append(n).append(' ');
                }
            }
            System.out.println("[CP] C3   候选方法: " + (shown == 0 ? "（没有任何 liu/gildedmod 方法 → 注入真的没生效）" : cand.toString()));
            return 1;
        } catch (Throwable t) {
            System.out.println("[CP] C3 检查异常: " + what + " → " + t);
            return 1;
        }
    }
}
