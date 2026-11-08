package com.gildedmod.net;

import com.gildedmod.GildedMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端每 tick 上报一次 Alt 状态（状态不变时不发包）。
 * <p>用 {@code Dist.CLIENT} + {@code Bus.FORGE} 注册，专用服务器上不会加载。
 */
@Mod.EventBusSubscriber(modid = GildedMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltClientTicker {

    private AltClientTicker() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        AltClient.tick();
    }
}
