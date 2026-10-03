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

import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BlackGoldPickaxe extends PickaxeItem {

    public BlackGoldPickaxe() {
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

    // ===== ✨ 永不磨损：原版 Unbreakable 标签 =====
    @Override
    public void inventoryTick(ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("Unbreakable")) {
            tag.putBoolean("Unbreakable", true);
        }
    }

    // ===== ⛏️ 挖掘速度 =====
    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        return 100000.0F;
    }

    // ===== ⛏️ 右键技能：3x3 范围挖掘 =====
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BlockHitResult hit = (BlockHitResult) player.pick(20.0D, 0.0F, false);
            BlockPos center = hit.getBlockPos();
            int count = 0;

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos target = center.offset(x, y, z);
                        BlockState state = world.getBlockState(target);

                        if (!state.isAir()) {
                            java.util.List<ItemStack> drops = Block.getDrops(state, serverLevel, target, null);
                            world.setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                            if (drops.isEmpty()) {
                                ItemStack self = new ItemStack(state.getBlock().asItem());
                                if (!self.isEmpty()) {
                                    drops = java.util.List.of(self);
                                }
                            }

                            for (ItemStack drop : drops) {
                                if (!drop.isEmpty()) {
                                    world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                                            world,
                                            target.getX() + 0.5,
                                            target.getY() + 0.5,
                                            target.getZ() + 0.5,
                                            drop
                                    ));
                                }
                            }
                            count++;
                        }
                    }
                }
            }

            player.sendSystemMessage(Component.literal(
                    "§2⛏️ §6GODSWORD §2⛏️ §8» §7神权·开山 §8» §7挖掉了 §a" + count + " §7个方块"
            ));
        } else if (world.isClientSide) {
            player.displayClientMessage(Component.literal("§2⛏️ 神权·开山"), true);
            player.playSound(SoundEvents.STONE_BREAK, 1.0F, 1.0F);
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

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        if (player.level().isClientSide) return true;

        Level world = player.level();
        BlockState state = world.getBlockState(pos);

        if (!state.isAir()) {
            java.util.List<ItemStack> drops = Block.getDrops(state,
                    (net.minecraft.server.level.ServerLevel) world, pos, null);
            world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

            if (drops.isEmpty()) {
                ItemStack self = new ItemStack(state.getBlock().asItem());
                if (!self.isEmpty()) {
                    drops = java.util.List.of(self);
                }
            }

            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    world.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            world,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            drop
                    ));
                }
            }
        }

        return true;
    }

    // ===== 📺 全屏神谕介绍框 =====
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.clear();

        tooltip.add(Component.literal("§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("       §2§lG O D P I C K A X E"));
        tooltip.add(Component.literal("         §7§l「神 权 · 开 山」"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §2✦ §l神权·永恒    §7»  §c∞ 永不磨损"));
        tooltip.add(Component.literal("  §2✦ §l神权·开山    §7»  §b右键3x3范围挖掘"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §7右键: §b3x3范围挖掘"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §7§o「一镐开山，万石臣服」"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k§2§m§k"));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}