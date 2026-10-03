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
package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import java.util.List;

public class BlackGoldAxe extends AxeItem {

    public BlackGoldAxe() {
        super(
                Tiers.NETHERITE,
                0,
                -2.0F,
                new Item.Properties()
                        .durability(1)
                        .rarity(Rarity.EPIC)
                        .fireResistant()
        );
    }

    // ===== ✨ 永不磨损三重保障 =====
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 什么都不做！永远不掉耐久！
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;  // 永远显示满耐久！
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;  // 无法修复
    }

    // ===== ✨ 挖方块不掉耐久 =====
    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        return true;  // 挖掘成功，不掉耐久！
    }

    // ===== ⚔️ 斧头攻击生物 =====
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            // 高额伤害（20点 = 10颗心）
            target.hurt(player.damageSources().playerAttack(player), 2147483646.9F);

            // 破盾
            if (target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
                targetPlayer.disableShield(true);
            }

            // 击退
            target.knockback(1.5F,
                    attacker.getX() - target.getX(),
                    attacker.getZ() - target.getZ());
        }
        return true;  // 不掉耐久
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 99999.0F;  // 秒天秒地秒空气！
    }

    // ===== 🌪️ 右键技能：潜行+右键 = 3x3挖掘，普通右键 = 连锁砍树 =====
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                // ✨ 潜行+右键：3x3范围挖掘
                BlockPos center = player.blockPosition();
                int count = 0;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos target = center.offset(x, y, z);
                            BlockState state = world.getBlockState(target);

                            if (!state.isAir() && state.getDestroySpeed(world, target) >= 0) {
                                world.destroyBlock(target, true);
                                count++;
                            }
                        }
                    }
                }

                player.sendSystemMessage(Component.literal(
                        "§2🪓 §6GODAXE §2🪓 §8» §7潜行·劈地 §8» §7挖掉了 §a" + count + " §7个方块"
                ));

            } else {
                // ✨ 普通右键：连锁砍树
                BlockPos target = player.blockPosition();
                cutTree(world, target, player);
            }

        } else {
            // 客户端特效
            if (player.isShiftKeyDown()) {
                player.displayClientMessage(Component.literal("§2🪓 潜行·劈地"), true);
                player.playSound(SoundEvents.STONE_BREAK, 1.0F, 1.0F);
            } else {
                player.displayClientMessage(Component.literal("§6🌳 连锁砍树"), true);
                player.playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
            }

            for (int i = 0; i < 30; i++) {
                world.addParticle(ParticleTypes.CLOUD,
                        player.getX() + (world.random.nextDouble() - 0.5) * 5,
                        player.getY() + world.random.nextDouble() * 2,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 5,
                        0, 0.1, 0);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    // ===== 🌳 连锁砍树方法 =====
    private void cutTree(Level world, BlockPos start, Player player) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        int maxBlocks = 100;
        int count = 0;

        while (!queue.isEmpty() && count < maxBlocks) {
            BlockPos current = queue.poll();
            BlockState state = world.getBlockState(current);

            if (state.is(BlockTags.LOGS)) {
                world.destroyBlock(current, true);
                count++;

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        if (world.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal(
                "§6🪓 连锁砍树 §8» §7砍掉了 §a" + count + " §7个木头"
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.clear();

        tooltip.add(Component.literal("§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("              §2§lG O D A X E"));
        tooltip.add(Component.literal("             §7§l「神 权 · 劈 地」"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §2✦ §l神权·永恒    §7»  §k fkgajfakgdsjlkg§r"));
        tooltip.add(Component.literal("  §2✦ §l神权·开山    §7»  §b右键3x3范围挖掘"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §7右键: §b3x3范围挖掘"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §7§o「一斧劈万物 · 一斧定乾坤」"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k"));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true;
    }
}