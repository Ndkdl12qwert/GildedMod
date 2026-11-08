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
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 时停命令。
 *
 * <pre>
 *   /gildedtime stop      开启时停
 *   /gildedtime start     关闭时停
 *   /gildedtime status    查询状态
 * </pre>
 */
@Mod.EventBusSubscriber(modid = GildedMod.MODID)
public final class GildedTimeCommand {

    private GildedTimeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("gildedtime")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("stop").executes(ctx -> {
                    TimeStop.set(true);
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("§6[时停] §a已开启 —— 除玩家外所有实体停止 tick"),
                        true);
                    return 1;
                }))
                .then(Commands.literal("start").executes(ctx -> {
                    TimeStop.set(false);
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("§6[时停] §c已关闭"),
                        true);
                    return 1;
                }))
                .then(Commands.literal("armorstats").executes(ctx -> {
                    var src = ctx.getSource();
                    src.sendSuccess(() -> Component.literal("§6[ARMOR] §f" + LiuArmorCore.nativeGuardStats()), false);
                    String log = LiuArmorCore.nativeGuardLog();
                    System.out.println("[ARMOR] stats: " + LiuArmorCore.nativeGuardStats());
                    System.out.println("[ARMOR] recent events:");
                    if (log != null) {
                        for (String line : log.split("\n")) {
                            System.out.println("[ARMOR] " + line);
                        }
                    }
                    return 1;
                }))
                .then(Commands.literal("armorreset").executes(ctx -> {
                    LiuArmorCore.nativeGuardResetStats();
                    ctx.getSource().sendSuccess(() -> Component.literal("§6[ARMOR] §a统计已清零"), false);
                    return 1;
                }))
                .then(Commands.literal("overrides").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var target = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有实体"));
                        return 0;
                    }
                    String[] list = LiuCore.nativeListOverrides(target);
                    String cls = target.getClass().getName();
                    System.out.println("[BYPASS] " + cls + " override 了 " + (list == null ? 0 : list.length) + " 个父类方法");
                    if (list != null) {
                        for (String s : list) {
                            System.out.println("[BYPASS]   " + s);
                        }
                    }
                    src.sendSuccess(() -> Component.literal(
                            "§6[BYPASS] §f" + target.getClass().getSimpleName()
                            + " override 了 §a" + (list == null ? 0 : list.length) + "§f 个父类方法（见控制台）"), false);
                    return 1;
                }))
                .then(Commands.literal("bypassset").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var target = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有实体"));
                        return 0;
                    }
                    int ok = LiuCore.nativeInvokeSuper(
                            target, "setHealth", "(F)V", 2, 0.0);
                    float real = (float) LiuCore.nativeInvokeSuper(
                            target, "getHealth", "()F", 2, 0.0);
                    src.sendSuccess(() -> Component.literal(
                            "§6[BYPASS] §fsuper.setHealth(0)=" + (ok == 1 ? "§a成功" : "§c失败")
                            + " §7| 触发父类 getHealth=" + real), false);
                    return 1;
                }))
                .then(Commands.literal("hpshow").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var target = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有实体"));
                        return 0;
                    }
                    String info = target.getClass().getSimpleName()
                            + " id=" + target.getId()
                            + " hp=" + target.getHealth()
                            + " max=" + target.getMaxHealth()
                            + " dead=" + target.isDeadOrDying()
                            + " removed=" + target.isRemoved()
                            + " locked=" + HealthLock.isLocked(target);
                    System.out.println("[HPSHOW] " + info);
                    src.sendSuccess(() -> Component.literal("§6[HPSHOW] §f" + info), false);
                    return 1;
                }))
                .then(Commands.literal("lockhp").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var target = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player && e.isAlive()
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有活着的实体"));
                        return 0;
                    }
                    boolean ok = HealthLock.lock(target, 0.0F);
                    src.sendSuccess(() -> Component.literal(
                            "§6[HPLOCK] §f" + target.getClass().getSimpleName()
                            + " §7→ " + (ok ? "§a已锁住血量 0" : "§c失败")), true);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("unlockhp").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var target = player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有实体"));
                        return 0;
                    }
                    HealthLock.unlock(target);
                    src.sendSuccess(() -> Component.literal("§6[HPLOCK] §f已解锁"), true);
                    return 1;
                }))
                .then(Commands.literal("ultkill").executes(ctx -> {
                    var src = ctx.getSource();
                    var player = src.getPlayerOrException();
                    var level = player.level();
                    // 取 8 格内最近的 LivingEntity
                    var target = level.getEntitiesOfClass(
                            net.minecraft.world.entity.LivingEntity.class,
                            player.getBoundingBox().inflate(8.0),
                            e -> e != player && e.isAlive()
                    ).stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                    if (target == null) {
                        src.sendFailure(Component.literal("§c8 格内没有活着的实体"));
                        return 0;
                    }
                    String name = target.getClass().getSimpleName();
                    int step = LiuCore.nativeUltimateKill(target);
                    src.sendSuccess(() -> Component.literal(
                            "§6[ULT] §f" + name + " §7→ step=" + step
                            + "  存活=" + target.isAlive()), true);
                    return 1;
                }))
                .then(Commands.literal("strip").executes(ctx -> {
                    int n = LiuCore.nativeRetransformTargets();
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("§6[ULT] §f重载了 §a" + n + "§f/3 个类"), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    boolean frozen = TimeStop.isFrozen();
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("§6[时停] " + (frozen ? "§a运行中" : "§7未开启")),
                        false);
                    return 1;
                }))
        );
    }
}
