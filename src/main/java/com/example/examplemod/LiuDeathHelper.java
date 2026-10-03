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

import com.example.examplemod.mixin.LivingEntityHealthAccessor;
import com.example.examplemod.mixin.LivingEntityInvoker;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class LiuDeathHelper {

    public static void liuDie(LivingEntity living, DamageSource source) {
        LiuBypass.setBypass(true);
        try {
            // ============ 1. 主击杀：JVMTI 直写 health ============
            boolean jvmtiOK = false;
            try {
                jvmtiOK = LiuCore.nativeSetHealthJVMTI(living, 0.0f);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // ============ 2. Java 兜底 ============
            if (!jvmtiOK) {
                System.out.println("[LIU-KILL] JVMTI failed, fallback");
                try {
                    living.getEntityData().set(
                            com.example.examplemod.mixin.LivingEntityHealthAccessor.getDataHealthId(),
                            0.0f);
                } catch (Throwable ignored) {}
                try {
                    ((LivingEntityInvoker) living).invokeSetHealthRaw(0.0f);
                } catch (Throwable ignored) {}
            }

            // ============ 3. 走原版死亡流程 ============
            if (living instanceof ServerPlayer sp) {
                try {
                    sp.connection.send(new ClientboundPlayerCombatKillPacket(
                            sp.getId(), source.getLocalizedDeathMessage(sp)));
                } catch (Throwable ignored) {}
                try { sp.die(source); } catch (Throwable ignored) {}
            } else {
                living.level().broadcastEntityEvent(living, (byte) 3);
            }

            // ============ 4. 掉落 ============
            try {
                ((LivingEntityInvoker) living).invokeDropAllDeathLoot(source);
            } catch (Throwable ignored) {}

            // ============ 5. JVMTI 标记 removed ============
            try {
                LiuCore.nativeMarkRemovedJVMTI(living);
            } catch (Throwable ignored) {}

            // ============ 6. Java 层兜底 remove ============
            if (living.level() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    LiuBypass.setBypass(true);
                    try { living.remove(Entity.RemovalReason.KILLED); }
                    finally { LiuBypass.setBypass(false); }
                });
            }

            // ============ 7. 统计 ============
            if (source.getEntity() instanceof ServerPlayer killer) {
                try {
                    killer.awardStat(net.minecraft.stats.Stats.ENTITY_KILLED.get(living.getType()));
                } catch (Throwable ignored) {}
            }
        } finally {
            LiuBypass.setBypass(false);
        }
    }
}