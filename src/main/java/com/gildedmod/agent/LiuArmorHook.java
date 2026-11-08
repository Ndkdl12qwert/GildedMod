/*
 * GildedMod
 * Copyright (C) 2026 Onicox
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gildedmod.agent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LiuArmorHook {

    /**
     * 盔甲的<b>点分全限定名</b>（下面用 {@code Class.getName()} 比较，所以必须是点分、不是斜杠）。
     * <p>这里刻意用字符串而不是 {@code instanceof BlackGoldArmor}：本类会被打进独立的 agent jar，
     * 与 Minecraft/mod 的类加载器不同，直接引用 mod 类会抛 {@code NoClassDefFoundError}。
     * <p>注意：改包名时这里也要跟着改（历史上这里写的是 MDK 模板包名
     * {@code com.example.examplemod} 导致整套守卫静默失效）。
     */
    private static final String ARMOR_CLASS = "com.gildedmod.BlackGoldArmor";

    /**
     * <b>必须缓存</b>：{@code System.getProperty} 内部是 {@code Hashtable} +
     * {@code synchronized} + SecurityManager 检查，而这个方法会被
     * {@code hurt}/{@code setHealth}/{@code die}/{@code remove}/{@code getHealth} 的钩子
     * 在每 tick 调用上万次 —— 现场读属性会造成跨线程锁竞争，实测是主要开销之一。
     *
     * <p>{@code liu.bypass} 只在启动时由 {@code -D} 设定，运行期不会变，所以读一次即可。
     */
    private static final boolean BYPASS = "true".equals(System.getProperty("liu.bypass"));

    private static boolean isBypass() {
        return BYPASS;
    }

    /**
     * 判断是否穿着全套鎏金甲。
     *
     * <p><b>顺序很重要</b>：先做 {@code instanceof Player}（一次 cheap 判断，99.9% 的调用
     * 到这里就返回了），再走后面的反射字符串比较。之前写成先查 bypass 属性，
     * 等于让每一只怪、每一次伤害计算都去抢一把全局锁。
     */
    public static boolean hasFullArmor(Object entity) {
        if (!(entity instanceof Player player)) return false;
        if (isBypass()) return false;
        try {
            int pieces = 0;
            for (ItemStack armor : player.getArmorSlots()) {
                if (armor == null || armor.isEmpty()) continue;
                String cls = armor.getItem().getClass().getName();
                if (cls.equals(ARMOR_CLASS) || cls.startsWith(ARMOR_CLASS + "$")) {
                    pieces++;
                }
            }
            return pieces >= 4;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean shouldBlockHurt(Object entity) {
        return hasFullArmor(entity);
    }

    /**
     * 时停判定 —— 由 agent 注入到 {@code LivingEntity.tick()} 的 HEAD。
     *
     * <p><b>为什么走 agent 而不是 Mixin</b>：{@code TimeStopMixin} 实测挂不上 ✗
     * （refmap 映射正确 ✓、无任何报错 ✓、但注入的方法从不执行 ✗）。
     * 而 agent 对同一个 {@code LivingEntity} 的 setHealth / hurt / die / remove
     * 注入一直是成功的 ✓ —— 所以走这条已验证的路径。
     *
     * <p><b>为什么直接问 native</b>：{@code TimeStop} 会被两个 ClassLoader 各加载一份，
     * Java 的 static 字段不共享（这个坑已经踩过两次），DLL 只有一份 ✓。
     *
     * <p><b>为什么放行玩家</b>：否则开了时停自己也动不了，没法玩。
     *
     * <p><b>性能</b>：每次 {@code tick()} 都会调用，所以第一件事就是问 native 的
     * atomic bool（几个纳秒），未开启时立刻返回。
     */
    public static boolean shouldFreezeTick(Object entity) {
        try {
            if (entity == null) {
                return false;
            }
            if (!com.gildedmod.LiuCore.nativeTimeFreezeGet()) {
                return false;                       // ★ 快路径：没开时停，什么都不做
            }
            return !(entity instanceof net.minecraft.world.entity.player.Player);
        } catch (Throwable t) {
            // 钩子里绝不能抛异常，否则每 tick 一个异常
            return false;
        }
    }

    public static boolean shouldBlockSetHealth(Object entity, float health) {
        // ★ 处决名单优先（对所有实体生效）
        //   只拦「回血」，放行「掉血」和「归零」：
        //     对方回血：health > 0 且 >= 当前   → 拦下 ✓
        //     受到伤害：health <  当前          → 放行 ✓
        //     die() 内部 setHealth(0)          → 放行 ✓（拦了就打不死了）
        //
        //   名单存在 C++ 里（nativeDoomIsDoomed）：com.gildedmod.agent.* 同时存在于
        //   mod jar 与 agent jar，会被两个 ClassLoader 各加载一份 → 两个 static Set ✗。
        //   DLL 进程内只有一份，天然共享 ✓
        try {
            if (entity instanceof LivingEntity doomed
                    && com.gildedmod.LiuCore.nativeDoomIsDoomed(doomed)) {
                float cur = doomed.getHealth();
                if (health > 0.0F && health >= cur) {
                    com.gildedmod.LiuCore.nativeDoomLogIntercept();
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // 钩子里绝不能抛异常，否则每 tick 一个异常
        }

        if (!hasFullArmor(entity)) return false;
        if (!(entity instanceof LivingEntity le)) return false;
        try {
            return health < le.getHealth();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean shouldBlockDie(Object entity) {
        return hasFullArmor(entity);
    }

    public static boolean shouldBlockRemove(Object entity) {
        return hasFullArmor(entity);
    }
}