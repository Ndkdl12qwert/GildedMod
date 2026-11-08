package com.gildedmod.net;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端侧的 Alt 检测与状态上报（只在客户端加载）。
 * <p>左右 Alt 都算；每 tick 检查一次，只有状态变化时才发包，所以没有持续开销。
 */
public final class AltClient {

    private static boolean lastState;

    private AltClient() {
    }

    public static boolean isDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    /** 每客户端 tick 调用（见 {@link AltClientTicker}）。 */
    public static void tick() {
        boolean now = isDown();
        if (now != lastState) {
            lastState = now;
            AltSync.CHANNEL.sendToServer(new AltSync.AltPacket(now));
        }
    }
}
