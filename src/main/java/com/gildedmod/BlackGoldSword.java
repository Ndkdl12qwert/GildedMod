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

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BlackGoldSword extends SwordItem {

    public BlackGoldSword() {
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

    @Override
    public void setDamage(ItemStack stack, int damage) {}

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            String entityName = target.getName().getString();
            attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0F, 0.5F);

            if (attacker instanceof Player player) {
                player.sendSystemMessage(Component.literal(
                        "§c☠ §6GODSWORD §c☠ §8» §7" + entityName + " §8已被§c§k fjafjOFJoiewjf§r"
                ));
            }
        }
        return true;
    }

    private static boolean isHasAnyBlackGoldWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return (mainHand.getItem() instanceof BlackGoldSword) ||
                (offHand.getItem() instanceof BlackGoldSword) ||
                (mainHand.getItem() instanceof BlackGoldPickaxe) ||
                (offHand.getItem() instanceof BlackGoldPickaxe) ||
                (mainHand.getItem() instanceof BlackGoldAxe) ||
                (offHand.getItem() instanceof BlackGoldAxe);
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miner) {
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        castLightning(world, player);
        return InteractionResultHolder.success(stack);
    }

    private void castLightning(Level world, Player player) {
        if (!world.isClientSide) {
            AABB box = new AABB(
                    player.getX() - 128, player.getY() - 64, player.getZ() - 128,
                    player.getX() + 128, player.getY() + 64, player.getZ() + 128
            );

            List<Entity> entities = world.getEntities(player, box,
                    entity -> entity != player && entity.isAlive());
            int count = 0;
            for (Entity entity : entities) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.moveTo(entity.position());
                    world.addFreshEntity(lightning);
                }
                // 用 setHealth(0) + die 代替 remove(KILLED)，走死亡流程
                if (entity instanceof LivingEntity living) {
                    living.setHealth(0);
                    living.die(world.damageSources().magic());
                } else {
                    entity.remove(Entity.RemovalReason.KILLED);
                }
                count++;
            }

            for (Player p : world.players()) {
                p.sendSystemMessage(Component.literal(
                        "§e⚡ §6GODSWORD §e⚡ §8» §7" + player.getName().getString() +
                                " §8发动了 §e神权·天罚 §8» §7抹除了 §c" + count + " §7个存在"
                ));
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0F, 1.0F);

        } else {
            for (int i = 0; i < 300; i++) {
                world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        player.getX() + (world.random.nextDouble() - 0.5) * 100,
                        player.getY() + world.random.nextDouble() * 20,
                        player.getZ() + (world.random.nextDouble() - 0.5) * 100,
                        0, 0, 0);
            }
            player.displayClientMessage(Component.literal("§e⚡ §l神权·天罚 §e⚡"), true);
            player.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0F, 1.0F);
        }
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClientSide) {
            stack.getOrCreateTag().putLong("AnimTick", System.currentTimeMillis());
        }
        if (world.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        if (selected) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                if (!isHasAnyBlackGoldWeapon(player) && player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        if (selected && player.tickCount % 40 == 0) {
            player.displayClientMessage(
                    Component.literal("§6§lGODSWORD §8| §e§l天罚 §8| §7右键释放"),
                    true
            );
        }
    }

    private static Component rainbowSmooth(String text, long offsetMs, float hueStart, float hueSpan) {
        MutableComponent root = Component.empty();
        int len = text.length();
        if (len == 0) return root;

        float timeShift = (offsetMs / 1000.0f) % 1.0f;

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                root.append(Component.literal("\n"));
                continue;
            }

            float t = len == 1 ? 0 : (float) i / (len - 1);
            float hue = (hueStart + t * hueSpan + timeShift) % 1.0f;
            if (hue < 0) hue += 1.0f;

            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;

            MutableComponent ch = Component.literal(String.valueOf(c))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
            root.append(ch);
        }
        return root;
    }


    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.clear();

        long t = System.currentTimeMillis();

        // 顶部装饰线（暗金渐变）
        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));

        // 标题：GildedEX 彩虹 + 副标题
        tooltip.add(Component.empty()
                .append(Component.literal("  "))
                .append(rainbowSmooth("GildedEX", t, 1.0f, 1.0f))
                .append(Component.literal("  §7·  §7§l「§e神 陨§7§l」")));

        // 底部装饰线
        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));
        tooltip.add(Component.literal(""));

        // 属性列表
        tooltip.add(Component.literal("  §e✦ §6神权·永恒    §7»  §a无限耐久"));
        tooltip.add(Component.literal("  §e✦ §6神权·不灭    §7»  §a免疫一切伤害"));
        tooltip.add(Component.literal("  §e✦ §6神权·翱翔    §7»  §a手持自动飞行"));
        tooltip.add(Component.literal("  §e✦ §6神权·抹除    §7»  §a左键直接删除"));
        tooltip.add(Component.literal(""));

        // 当前神权（彩虹文字）
        tooltip.add(Component.empty()
                .append(Component.literal("  §e✦ §6当前神权    §7»  "))
                .append(rainbowSmooth("天罚 · 雷霆万钧", t, 1.0f, 1.0f)));

        tooltip.add(Component.literal("  §e✦ §6释放方式    §7»  §7右键"));
        tooltip.add(Component.literal(""));

        tooltip.add(Component.literal("  §7§o「吾持手中之太阿，至以无穷，鎏！」"));
        tooltip.add(Component.literal(""));

        tooltip.add(rainbowSmooth("                                        ", t, 0.8f, 0.6f));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}