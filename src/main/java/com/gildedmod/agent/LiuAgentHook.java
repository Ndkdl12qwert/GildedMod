package com.gildedmod.agent;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.LiuDeathHelper;
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
        try {
            if (!(self instanceof net.minecraft.server.network.ServerGamePacketListenerImpl impl)) return;
            if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundInteractPacket p)) return;

            net.minecraft.server.level.ServerPlayer player = impl.player;
            if (player == null) return;
            if (!(player.getMainHandItem().getItem() instanceof BlackGoldSword)) return;

            net.minecraft.world.entity.Entity target = p.getTarget(player.serverLevel());
            if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) return;

            LiuDeathHelper.liuDie(living, player.damageSources().playerAttack(player));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}