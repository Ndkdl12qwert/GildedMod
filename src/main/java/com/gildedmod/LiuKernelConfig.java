package com.gildedmod;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 内核配置读取（{@code config/gildedmod/kernel.properties}）+ 攻击归属判定。
 *
 * <h3>"有攻击就废"</h3>
 * {@link #blockAllHurt()} 打开时，{@code LivingEntity.hurt} 里任何来源的伤害都会被取消，
 * <b>除了我们自己发出的攻击</b>（见 {@link #isOurAttack(DamageSource)}）——
 * 这样"任何 mod 的任何攻击"都无效，而我们的剑照常能打。
 */
public final class LiuKernelConfig {

    private static volatile boolean loaded;
    private static volatile boolean blockAllHurt;
    private static volatile boolean blockAttacks = true;
    private static volatile boolean jvmPatch = true;

    private LiuKernelConfig() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;

        java.io.File f = new java.io.File("config/gildedmod/kernel.properties");
        if (!f.isFile()) {
            blockAllHurt = Boolean.parseBoolean(System.getProperty("liu.block.all.hurt", "false"));
            return;
        }
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(f),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if (k.equals("block.all.hurt")) {
                    blockAllHurt = Boolean.parseBoolean(v);
                } else if (k.equals("block.attacks")) {
                    blockAttacks = Boolean.parseBoolean(v);
                } else if (k.equals("kernel.jvm.patch")) {
                    jvmPatch = Boolean.parseBoolean(v);
                }
            }
        } catch (Throwable ignored) {
        }

        // 命令行可覆盖
        blockAllHurt = Boolean.parseBoolean(
                System.getProperty("liu.block.all.hurt", String.valueOf(blockAllHurt)));

        System.out.println("[KERNEL] block.all.hurt=" + blockAllHurt + "  block.attacks=" + blockAttacks);
    }

    /** 是否取消一切非我方伤害（"有攻击就废"） */
    public static boolean blockAllHurt() {
        ensureLoaded();
        return blockAllHurt;
    }

    public static boolean blockAttacks() {
        ensureLoaded();
        return blockAttacks;
    }

    /** 是否对 jvm.dll 打补丁（JVM_Halt / JVM_GC 立即返回） */
    public static boolean jvmPatchEnabled() {
        ensureLoaded();
        return jvmPatch;
    }

    /**
     * 这次伤害是不是"我们自己发出的"。
     *
     * <p>判定：直接攻击者是玩家，且手里拿着鎏金剑/神剑 → 算我们的 ✓
     * 其它（包括投射物、环境伤害、其它 mod 的实体）→ 一律不算 ✓
     */
    public static boolean isOurAttack(DamageSource source) {
        if (source == null) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return false;
        }
        return GildedMod.holdsBlackGoldSword(player) || GildedMod.hasGodItem(player);
    }
}
