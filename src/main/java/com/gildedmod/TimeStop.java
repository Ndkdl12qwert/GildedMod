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

package com.gildedmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时停（Time Stop）—— 让指定的实体这一 tick <b>完全不执行 tick()</b>。
 *
 * <h3>为什么需要它</h3>
 * 实测 {@code kakiku/pig2mod} 的 {@code Pig2}：血量被我们压到 0.0、{@code dead=1}、
 * {@code isRemoved=1} 连续 6 tick，它<b>照样攻击并杀死了玩家</b> ✗ ——
 * 因为它是一只<b>不遵守原版生死逻辑</b>的实体：自愈代码每 tick 都在跑，
 * 攻击代码也完全不检查自己的血量。
 *
 * <p>结论：改它的<b>状态</b>永远赢不了它，必须让它的<b>代码根本不运行</b>。
 *
 * <h3>挂载点为什么是 ServerLevel.tickNonPassenger</h3>
 * <pre>
 *   ServerLevel.tickNonPassenger(Entity)      ← ★ 服务端每个实体从这里进
 *      └─ entity.tick()  (m_8119_)
 *           └─ Pig2.tick() → super.tick() → ...
 * </pre>
 * 只钩 {@code Entity.tick} 是<b>不够</b>的 ✗ —— 子类 override 了它，
 * Mixin 只能取消被注入的那一个方法体，子类的 tick 照样跑。
 * 钩住 {@code tickNonPassenger} 才是"整只实体的 tick 都不发生" ✓。
 *
 * <h3>副作用（都是想要的）</h3>
 * <ul>
 *     <li>生物不移动、不攻击、不刷新 AI ✓</li>
 *     <li>掉落物不落地 ✓、经验球不飞 ✓、TNT 不炸 ✓</li>
 *     <li>生物 tick 全部跳过 → <b>服务端 tick 时间大幅下降</b> ✓（顺便治卡顿）</li>
 * </ul>
 * <b>注意</b>：方块实体（熔炉/漏斗）不受影响 ✗ —— 它们走 {@code Level.tickBlockEntities}，
 * 如果要一并冻结，需要额外钩那个方法。
 */
public final class TimeStop {

    // ★ 开关不在 Java 里！
    //   TimeStop 会被两个类加载器各加载一份（命令侧 / Mixin 侧），
    //   Java 的 static volatile 无法跨加载器共享 —— 实测 /gildedtime stop
    //   打印了"开启"，但 Mixin 里读到的却是 false，一条拦截都没有 ✗。
    //   所以真正的开关放在 DLL 里，这里只是转发。

    /** 免疫名单：这些实体在时停中仍然正常 tick（默认加玩家）。 */
    private static final Set<UUID> EXEMPT = ConcurrentHashMap.newKeySet();

    private TimeStop() {
    }

    public static boolean isFrozen() {
        try {
            return LiuCore.nativeTimeFreezeGet();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 切换时停。返回切换后的状态。 */
    public static boolean toggle() {
        boolean next = !isFrozen();
        set(next);
        return next;
    }

    /** 保存被我们改过的 gamerule 原值，关闭时恢复 */
    private static int savedRandomTick = -1;
    private static Boolean savedDaylight = null;
    private static Boolean savedWeather = null;

    public static void set(boolean value) {
        try {
            LiuCore.nativeTimeFreezeSet(value);
        } catch (Throwable t) {
            System.out.println("[TIME] 设置失败: " + t);
        }
        applyGameRules(value);
    }

    /**
     * 冻结世界规则：随机刻 / 昼夜 / 天气。
     * 这三样在 gamerule 层面，不需要字节码注入，最可靠也最省。
     */
    private static void applyGameRules(boolean frozen) {
        try {
            net.minecraft.server.MinecraftServer server =
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            net.minecraft.world.level.GameRules rules = server.getGameRules();

            if (frozen) {
                // 记原值（只记一次）
                if (savedRandomTick < 0) {
                    savedRandomTick = rules.getInt(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING);
                }
                if (savedDaylight == null) {
                    savedDaylight = rules.getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
                }
                if (savedWeather == null) {
                    savedWeather = rules.getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE);
                }
                rules.getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING).set(0, server);
                rules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
                rules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
                System.out.println("[TIME] gamerule 已冻结: randomTick=0 daylight=off weather=off");
            } else {
                if (savedRandomTick >= 0) {
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING).set(savedRandomTick, server);
                    savedRandomTick = -1;
                }
                if (savedDaylight != null) {
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(savedDaylight, server);
                    savedDaylight = null;
                }
                if (savedWeather != null) {
                    rules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(savedWeather, server);
                    savedWeather = null;
                }
                System.out.println("[TIME] gamerule 已恢复");
            }
        } catch (Throwable t) {
            System.out.println("[TIME] gamerule 设置失败: " + t);
        }
    }

    /** 把这个实体加入免疫名单（时停中它还能动）。 */
    public static void exempt(Entity entity) {
        if (entity != null) {
            EXEMPT.add(entity.getUUID());
        }
    }

    public static void unexempt(Entity entity) {
        if (entity != null) {
            EXEMPT.remove(entity.getUUID());
        }
    }

    public static void clearExempt() {
        EXEMPT.clear();
    }

    /**
     * 这个实体的 tick 是否应该被跳过。
     * <p>热路径（每 tick 每个实体一次）：必须先判 {@code frozen}，未开启时零开销。
     */
    public static boolean shouldFreeze(Entity entity) {
        boolean on;
        try {
            on = LiuCore.nativeTimeFreezeGet();
        } catch (Throwable t) {
            return false;                 // native 不可用 → 不冻结，避免误伤
        }
        if (!on) {
            return false;                 // ★ 快路径：没开时停，什么都不做
        }
        if (entity == null) {
            return false;
        }
        // 玩家永远可以动（否则开了时停自己也动不了，没法玩）
        if (entity instanceof Player) {
            return false;
        }
        if (!EXEMPT.isEmpty() && EXEMPT.contains(entity.getUUID())) {
            return false;
        }
        return true;
    }
}
