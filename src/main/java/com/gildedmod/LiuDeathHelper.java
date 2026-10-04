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

import com.gildedmod.mixin.LivingEntityHealthAccessor;
import com.gildedmod.mixin.LivingEntityInvoker;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class LiuDeathHelper {

    private static final java.util.Map<java.util.UUID, Long> LAST_KILL = new java.util.concurrent.ConcurrentHashMap<>();

    public static void liuDie(LivingEntity living, DamageSource source) {
        long now = System.currentTimeMillis();
        Long last = LAST_KILL.get(living.getUUID());
        if (last != null && now - last < 1000) return;   // 1 秒冷却
        LAST_KILL.put(living.getUUID(), now);

        LiuBypass.setBypass(true);
        try {
            int step = LiuCore.nativeAbsoluteKill(living);
            System.out.println("[LIU-KILL] nativeAbsoluteKill last step: " + step);   // 保留
        } finally {
            LiuBypass.setBypass(false);
        }
    }
}