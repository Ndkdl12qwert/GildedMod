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
package com.example.examplemod.agent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LiuArmorHook {

    private static final String ARMOR_CLASS = "com.example.examplemod.BlackGoldArmor";

    private static boolean isBypass() {
        return "true".equals(System.getProperty("liu.bypass"));
    }

    public static boolean hasFullArmor(Object entity) {
        if (isBypass()) return false;
        if (!(entity instanceof Player player)) return false;
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

    public static boolean shouldBlockSetHealth(Object entity, float health) {
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