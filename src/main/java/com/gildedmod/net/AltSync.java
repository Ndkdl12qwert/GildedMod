package com.gildedmod.net;

import com.gildedmod.GildedMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 把"客户端是否按住 Alt"同步给服务端。
 *
 * <p>为什么要这个包：<b>Alt 是纯客户端按键</b>，原版只同步潜行等动作状态，
 * 而背包的拿取/丢出判定全部在<b>服务端</b>执行 —— 不把 Alt 状态发过去，
 * 服务端就没法区分"按住 Alt 允许操作"和"没按 → 只许复制"。
 *
 * <p>协议：客户端每 tick 检测一次，<b>状态变化时</b>才发包（几乎零开销）。
 * 本类不能引用任何客户端类（专用服务器也要能加载），客户端部分在 {@link AltClient}。
 */
public final class AltSync {

    private static final String PROTOCOL = "1";

    /** 服务端记录：当前按住 Alt 的玩家。 */
    private static final Set<UUID> SERVER_ALT = ConcurrentHashMap.newKeySet();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(GildedMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private AltSync() {
    }

    /** 在 mod 构造里调用一次（双端都要注册）。 */
    public static void register() {
        CHANNEL.registerMessage(0, AltPacket.class, AltPacket::encode, AltPacket::decode, AltPacket::handle);
    }

    /**
     * 现在是否允许"绕过保护"（真正拿走/丢出黑金剑）。
     * <p>客户端读实时按键；服务端读同步来的集合。
     * <p>注意 {@code AltClient} 只在这个 {@code isClientSide} 分支里被引用 ——
     * JVM 的类解析是惰性的，所以专用服务器上不会去加载客户端类。
     */
    public static boolean bypass(Player player) {
        if (player.level().isClientSide) {
            return AltClient.isDown();
        }
        return SERVER_ALT.contains(player.getUUID());
    }

    static void setServerState(ServerPlayer player, boolean down) {
        if (down) {
            SERVER_ALT.add(player.getUUID());
        } else {
            SERVER_ALT.remove(player.getUUID());
        }
    }

    /** 客户端 → 服务端：Alt 的按下状态。 */
    public record AltPacket(boolean down) {

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(down);
        }

        public static AltPacket decode(FriendlyByteBuf buf) {
            return new AltPacket(buf.readBoolean());
        }

        public static void handle(AltPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    AltSync.setServerState(sender, msg.down());
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
