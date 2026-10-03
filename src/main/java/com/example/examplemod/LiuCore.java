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

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class LiuCore {

    public static boolean shouldStrike(LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        return true;
    }

    public static void strike(LivingEntity target, DamageSource source) {
        LiuDeathHelper.liuDie(target, source);
    }
    public static native void nativeInitJVMTI();
    public static native boolean nativeSetHealthJVMTI(Object living, float health);
    public static native boolean nativeMarkRemovedJVMTI(Object entity);
}