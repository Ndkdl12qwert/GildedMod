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

import com.example.examplemod.BlackGoldSword;
import com.example.examplemod.LiuDeathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LiuAgentHook {

    private static final Set<UUID> DOOMED = ConcurrentHashMap.newKeySet();

    public static void markDoomed(Object entity) {
        if (entity instanceof Entity e) {
            DOOMED.add(e.getUUID());
        }
    }

    public static boolean isDoomed(Object entity) {
        if (entity instanceof Entity e) {
            return DOOMED.contains(e.getUUID());
        }
        return false;
    }

    public static void clearDoomed(UUID uuid) {
        if (uuid != null) {
            DOOMED.remove(uuid);
        }
    }

    public static void onPlayerAttack(Object self, Object target) {
        System.out.println("[LIU-AGENT] onPlayerAttack called: self=" + self + ", target=" + target);

        if (!(self instanceof Player player)) return;
        if (!(target instanceof LivingEntity living)) return;
        if (!(player.getMainHandItem().getItem() instanceof BlackGoldSword)) return;

        if (player.level().isClientSide) {
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        LiuDeathHelper.liuDie(living, player.damageSources().playerAttack(player));
    }

    public static void onHandleInteract(Object self, Object packet) {
        System.out.println("[LIU-AGENT] onHandleInteract called: self=" + self + ", packet=" + packet);

        try {
            if (!(self instanceof net.minecraft.server.network.ServerGamePacketListenerImpl impl)) return;
            if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundInteractPacket p)) return;

            net.minecraft.server.level.ServerPlayer player = impl.player;
            if (player == null) return;
            if (!(player.getMainHandItem().getItem() instanceof com.example.examplemod.BlackGoldSword)) return;

            net.minecraft.world.entity.Entity target = p.getTarget(player.serverLevel());
            if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) return;

            com.example.examplemod.LiuDeathHelper.liuDie(
                    living,
                    player.damageSources().playerAttack(player)
            );
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}