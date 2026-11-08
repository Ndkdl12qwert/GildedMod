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

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class BlackGoldArmor extends ArmorItem {

    // ===== 神权盔甲材质 =====
    public static final ArmorMaterial BLACKGOLD_MATERIAL = new ArmorMaterial() {
        @Override
        public @NotNull String getName() {
            return "gildedmod:blackgold";
        }

        @Override
        public int getDurabilityForType(Type type) {
            return 9999;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET -> 5;
                case CHESTPLATE -> 8;
                case LEGGINGS -> 6;
                case BOOTS -> 5;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 30;
        }

        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public float getToughness() {
            return 5.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 1.0F;
        }
    };

    public BlackGoldArmor(Type type) {
        super(BLACKGOLD_MATERIAL, type, new Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    // ===== 永不磨损 =====
    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 什么都不做！永远不掉耐久！
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }

    // ===== 全套效果：每 tick 触发 =====
    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        long t0 = GildedDebug.begin();

        int armorPieces = countArmorPieces(player);

        // ===== 全套鎏金 → 开启飞行 =====
        if (armorPieces >= 4) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            // 不是全套 → 关掉飞行（创造/旁观除外）
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        // ✗✗ 2026-11-07 删除：吸引仇恨（每件盔甲 × 每秒 × 32 格立方体扫描）
        //   实测在怪多的整合包里这是主要卡顿源 —— 生存模式待机都卡 ✓
        //   需要"怪来打你"用别的低开销方式，不做全范围扫描 ✗

        // ✗✗ 2026-11-07 删除：维持 9 个增益效果（MOVEMENT_SPEED / ABSORPTION /
        //   DAMAGE_BOOST / RESISTANCE / REGENERATION / NIGHT_VISION / WATER_BREATHING /
        //   FIRE_RESISTANCE）。每次 addEffect 都会触发属性重算 + 同步包，
        //   即使做了 10 tick 门限，生存模式待机仍然明显掉帧 ✓
        //   → 改为【纯代码级伤害拦截】：见 LiuArmorMixin（hurt / actuallyHurt 直接 cancel）✓

        GildedDebug.end("armorTick", t0);
    }


    // ===== 全屏神谕 =====
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.clear();

        String color = switch (this.getType()) {
            case HELMET -> "§b";
            case CHESTPLATE -> "§c";
            case LEGGINGS -> "§a";
            case BOOTS -> "§e";
        };

        tooltip.add(Component.literal(color + "§m§k" + color + "§m§k" + color + "§m§k"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("       " + color + "§lG I L E D E A R M O R"));
        tooltip.add(Component.literal(" 鎏 金 · " + getPieceName() + "汇集全寰宇之力"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  " + color + " "));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  §7套装效果:"));
        tooltip.add(Component.literal("    §7• §b2件: 速度提升"));
        tooltip.add(Component.literal("    §7• §d3件: 伤害吸收"));
        tooltip.add(Component.literal("    全套，你就是神明"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("  上帝的盔甲-GildedArmor"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(color + "§m§k" + color + "§m§k" + color + "§m§k"));
    }

    // ===== 辅助方法 =====
    private String getPieceName() {
        return switch (this.getType()) {
            case HELMET -> "神 识";
            case CHESTPLATE -> "神 躯";
            case LEGGINGS -> "神 行";
            case BOOTS -> "神 足";
        };
    }

    private int countArmorPieces(Player player) {
        int count = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof BlackGoldArmor) {
                count++;
            }
        }
        return count;
    }
}