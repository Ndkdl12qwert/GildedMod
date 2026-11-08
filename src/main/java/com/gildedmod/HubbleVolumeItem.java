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

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class HubbleVolumeItem extends Item {

    public HubbleVolumeItem() {
        super(new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world,
                                List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("§5§l§kJWJ§r可观测宇宙§kJWJ§r"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7§o「930亿光年的距离，凝于一掌」"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7万物之源 §8| §5存在之究极"));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}