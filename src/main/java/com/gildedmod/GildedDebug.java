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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 性能诊断开关 —— 用来定位"穿上盔甲后服务端 tick 变慢"的具体来源。
 *
 * <p>为什么需要它：盔甲脱掉时 tick 是 {@code 4 ms}，穿上后是 {@code 90 ms}（22 倍），
 * 但把每个子系统单独看一遍都不像能吃掉 86 ms 的样子 —— 这时候只能靠实测，
 * 不能靠读代码猜。
 *
 * <p>用法（游戏内）：
 * <pre>
 * /gildeddebug on      开始统计（每 100 tick 往日志打一行汇总）
 * /gildeddebug off     停止统计
 * /gildeddebug reset   清零
 * </pre>
 *
 * <p>开销：关闭时只有一次 {@code boolean} 判断；开启时每个子系统多两次
 * {@code System.nanoTime()}（约 20 纳秒），可以忽略。
 */
@Mod.EventBusSubscriber(modid = GildedMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GildedDebug {

    /** 是否在统计。默认关闭。 */
    private static volatile boolean enabled = false;

    /**
     * 渲染开关：关掉之后，物品/盔甲上的星空层完全不绘制。
     * <p>用来定位"穿上盔甲后 fps 掉到 0、但服务端代码只占 1ms"这类问题 ——
     * 关掉立刻恢复，就说明是渲染层（着色器 / 纹理 / 批次）的问题。
     */
    public static volatile boolean renderArmorStars = true;
    /** 同上，针对物品栏/手持物品的星空层。 */
    public static volatile boolean renderItemStars = true;

    public static boolean isRenderArmorStars() {
        return renderArmorStars;
    }

    public static boolean isRenderItemStars() {
        return renderItemStars;
    }

    /** 各子系统累计耗时（纳秒）。 */
    private static final Map<String, long[]> TOTALS = new LinkedHashMap<>();
    /** 统计窗口内经过的 tick 数。 */
    private static long ticks;

    /** 统计窗口长度（tick）。 */
    private static final int WINDOW = 100;

    /** 整个服务端 tick 的起点（TickEvent START 时记）。 */
    private static long tickStartNanos;
    /** 上一次窗口时的 GC 累计次数 / 耗时（毫秒），用来算增量。 */
    private static long lastGcCount = -1L;
    private static long lastGcTimeMs = -1L;

    private GildedDebug() {
    }

    /**
     * 记录整个服务端 tick 的耗时（从 START 到 END）。
     * <p>这是判定的关键：如果它接近 "Can't keep up" 里报的毫秒数，
     * 说明慢在整个 tick 上（GC / 别的 mod），而不是本 mod 的那几个子系统。
     */
    @SubscribeEvent
    public static void onServerTickTiming(TickEvent.ServerTickEvent event) {
        if (!enabled) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            tickStartNanos = System.nanoTime();
        } else if (tickStartNanos != 0L) {
            end("serverTickTotal", tickStartNanos);
            tickStartNanos = 0L;
        }
    }

    /** 读取 JVM 的 GC 累计次数与耗时（毫秒）——不需要任何 JVM 参数。 */
    private static long[] gcSnapshot() {
        long count = 0L;
        long timeMs = 0L;
        try {
            for (java.lang.management.GarbageCollectorMXBean gc
                    : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
                long c = gc.getCollectionCount();
                long t = gc.getCollectionTime();
                if (c > 0) {
                    count += c;
                }
                if (t > 0) {
                    timeMs += t;
                }
            }
        } catch (Throwable ignored) {
        }
        return new long[]{count, timeMs};
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** 在子系统开头调用，返回 nanoTime。 */
    public static long begin() {
        return enabled ? System.nanoTime() : 0L;
    }

    /** 在子系统结尾调用。关闭时什么都不做。 */
    public static void end(String name, long startNanos) {
        if (!enabled) {
            return;
        }
        long[] acc = TOTALS.computeIfAbsent(name, k -> new long[1]);
        acc[0] += System.nanoTime() - startNanos;
    }

    /** 每个服务端 tick 结束时调用一次。 */
    public static void tickDone() {
        if (!enabled) {
            return;
        }
        ticks++;
        if (ticks % WINDOW == 0) {
            dump();
        }
    }

    private static void dump() {
        long total = 0L;
        StringBuilder sb = new StringBuilder("[GILDED-TIMING] 最近 ");
        sb.append(WINDOW).append(" tick：");
        for (Map.Entry<String, long[]> e : TOTALS.entrySet()) {
            long ns = e.getValue()[0];
            if ("serverTickTotal".equals(e.getKey())) {
                continue;   // 单独打印
            }
            total += ns;
            sb.append(String.format(" %s=%.3fms", e.getKey(), ns / 1_000_000.0 / WINDOW));
        }
        sb.append(String.format(" 合计=%.3fms/tick", total / 1_000_000.0 / WINDOW));

        long[] totalAcc = TOTALS.get("serverTickTotal");
        if (totalAcc != null) {
            sb.append(String.format("  ★整个tick=%.3fms", totalAcc[0] / 1_000_000.0 / WINDOW));
        }

        // GC 增量：这一窗口内 GC 花了多少时间
        long[] gc = gcSnapshot();
        if (lastGcCount >= 0) {
            sb.append(String.format("  GC: 次数+%d 耗时+%dms"
                    , gc[0] - lastGcCount, gc[1] - lastGcTimeMs));
        }
        lastGcCount = gc[0];
        lastGcTimeMs = gc[1];

        System.out.println(sb);

        for (long[] v : TOTALS.values()) {
            v[0] = 0L;
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<net.minecraft.commands.CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("gildeddebug")
                .requires(src -> src.hasPermission(0))
                .then(Commands.literal("on").executes(ctx -> {
                    enabled = true;
                    ticks = 0L;
                    TOTALS.clear();
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§a[GILDED] 性能统计已开启，每 100 tick 输出一行到 logs"), false);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx -> {
                    enabled = false;
                    dump();
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§c[GILDED] 性能统计已关闭"), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ticks = 0L;
                    TOTALS.clear();
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§e[GILDED] 统计已清零"), false);
                    return 1;
                }))
                .then(Commands.literal("render")
                        .then(Commands.literal("on").executes(ctx -> {
                            renderArmorStars = true;
                            renderItemStars = true;
                            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "§a[GILDED] 星空层渲染：开"), false);
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            renderArmorStars = false;
                            renderItemStars = false;
                            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "§c[GILDED] 星空层渲染：关（用来判断卡顿是否来自渲染层）"), false);
                            return 1;
                        }))
                        .then(Commands.literal("armor").executes(ctx -> {
                            renderArmorStars = !renderArmorStars;
                            final boolean v = renderArmorStars;
                            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "§e[GILDED] 盔甲星空层：" + (v ? "开" : "关")), false);
                            return 1;
                        }))
                        .then(Commands.literal("item").executes(ctx -> {
                            renderItemStars = !renderItemStars;
                            final boolean v = renderItemStars;
                            ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "§e[GILDED] 物品星空层：" + (v ? "开" : "关")), false);
                            return 1;
                        })))
                .then(Commands.argument("any", StringArgumentType.greedyString())
                        .executes(ctx -> 1)));
    }
}
