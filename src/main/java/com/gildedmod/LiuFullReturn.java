package com.gildedmod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>全返回</b>注册表：这里登记的类，加载时所有方法都会被替换成"直接返回默认值"。
 *
 * <h3>怎么用（不用改代码、不用重编）</h3>
 * 在 {@code config/gildedmod/fullreturn.txt} 里一行写一个类的内部名（斜杠分隔）：
 * <pre>
 *   kakiku/pig2mod/entity/Pig2
 *   arcanevortex/protection/ProtectionHelper
 * </pre>
 * 重启游戏即生效 ✓
 *
 * <h3>安全白名单（这四类永远不处理）</h3>
 * <ul>
 *     <li>{@code net/minecraft/**} —— 原版，被多个 Mixin 抢改，动它必崩 ✗</li>
 *     <li>{@code com/gildedmod/**} —— 我们自己的</li>
 *     <li>{@code java/ jdk/ sun/} —— JDK</li>
 *     <li>{@code org/ cpw/ net/minecraftforge/} —— 框架</li>
 * </ul>
 *
 * <h3>注意</h3>
 * 全返回是<b>很重</b>的手段：目标类的所有方法都失效 ✓
 * 用它做过实验后建议删掉那一行 ✓ 否则那个 mod 会一直"半死" ✓
 */
public final class LiuFullReturn {

    private static final Set<String> TARGETS = ConcurrentHashMap.newKeySet();

    /** ff 文件里写 "*" 就打开自动模式：任何 mod 类只要含攻击方法，整类全返回 */
    private static volatile boolean AUTO_ALL;
    private static volatile boolean AUTO_LOADED;

    /** 自动模式用：这个类里是否有"攻击型"方法（名字/签名命中） */
    public static boolean looksLikeAttacker(String internalName) {
        if (internalName == null || isSafe(internalName)) {
            return false;
        }
        // 名字里带这些词的多半是攻击相关（保守判断，避免误伤数据类）
        String n = internalName.toLowerCase();
        return n.contains("attack") || n.contains("hurt") || n.contains("damage")
                || n.contains("combat") || n.contains("kill") || n.contains("weapon");
    }

    public static boolean isAutoAll() {
        ensureLoaded();
        return AUTO_ALL;
    }

    private static final String[] SAFE = {
            "net/minecraft/", "com/gildedmod/",
            "java/", "javax/", "jdk/", "sun/", "com/sun/",
            "org/", "cpw/", "net/minecraftforge/", "io/netty/", "com/mojang/",
    };

    private static volatile boolean loaded;

    private LiuFullReturn() {
    }

    private static boolean isSafe(String internalName) {
        for (String p : SAFE) {
            if (internalName.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /** 读取 config/gildedmod/fullreturn.txt（只读一次） */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        File f = new File("config/gildedmod/fullreturn.txt");
        if (!f.isFile()) {
            System.out.println("[FULLRETURN] 没有配置文件（" + f.getAbsolutePath() + "），全返回为空");
            return;
        }

        int n = 0;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if ("*".equals(line)) {
                    AUTO_ALL = true;
                    System.out.println("[FULLRETURN] 已开启自动模式 *（任何 mod 类的攻击型类全返回）");
                    continue;
                }
                String internal = line.replace('.', '/');
                if (isSafe(internal)) {
                    System.out.println("[FULLRETURN] 跳过（白名单）：" + internal);
                    continue;
                }
                if (TARGETS.add(internal)) {
                    n++;
                }
            }
        } catch (Throwable t) {
            System.out.println("[FULLRETURN] 读取失败：" + t);
        }
        System.out.println("[FULLRETURN] 已登记 " + n + " 个类：" + TARGETS);
    }

    public static boolean shouldFullReturn(String internalName) {
        if (internalName == null) {
            return false;
        }
        ensureLoaded();
        if (isSafe(internalName)) {
            return false;
        }
        if (TARGETS.contains(internalName)) {
            return true;
        }
        return AUTO_ALL && looksLikeAttacker(internalName);
    }

    public static boolean register(String internalName) {
        ensureLoaded();
        if (internalName == null) {
            return false;
        }
        String internal = internalName.replace('.', '/');
        if (isSafe(internal)) {
            return false;
        }
        return TARGETS.add(internal);
    }
}
