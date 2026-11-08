package com.gildedmod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 把闪电的顶点颜色换成彩虹渐变。
 *
 * <p>原理：原版 {@code LightningBoltRenderer} 画的是<b>程序化几何 + 顶点色</b>
 * （{@code RenderType.lightning()} 用的是 {@code POSITION_COLOR} 格式，没有贴图），
 * 颜色完全由顶点色决定 → 所以只要在顶点写出去之前把 RGB 换掉，闪电就是彩虹的。
 *
 * <p>alpha <b>原样保留</b>：闪电是 {@code LIGHTNING_TRANSPARENCY}（{@code SRC_ALPHA, ONE} 加色混合），
 * 亮度靠 alpha 控制，改掉它闪电就会变成一坨实心色块。
 *
 * <p>色相由顶点高度 y 决定 → 沿闪电形成竖直的彩虹渐变；再叠加该次闪电自己的
 * {@code seed} 让它每次都不一样；{@code time} 项很轻，只做一点点流动感。
 */
public class RainbowLightningVertexConsumer implements VertexConsumer {

    /** 彩虹沿高度重复的密度：越大条纹越密。 */
    private static final float HUE_PER_BLOCK = 0.18F;
    /** 整体滚动速度（闪电只存在 2 tick，这项基本看不出变化，留着方便调）。 */
    private static final float HUE_SPEED = 0.25F;

    private final VertexConsumer delegate;
    private final float seedOffset;

    // 记录最近一次 vertex() 的坐标，等 color() 到来时用它算色相
    private float lastY;

    public RainbowLightningVertexConsumer(VertexConsumer delegate, long seed) {
        this.delegate = delegate;
        // 用 seed 的低位做一个 [0,1) 的偏移，让每道闪电的配色不同
        this.seedOffset = (float) Math.floorMod(seed, 1000L) / 1000.0F;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.lastY = (float) y;
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        float hue = hueFor(lastY);
        int[] rgb = hsv(hue, 1.0F, 1.0F);
        this.delegate.color(rgb[0], rgb[1], rgb[2], alpha);
        return this;
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        float hue = hueFor(lastY);
        int[] rgb = hsv(hue, 1.0F, 1.0F);
        this.delegate.color(rgb[0], rgb[1], rgb[2], Math.round(alpha * 255.0F));
        return this;
    }

    private static float hueFor(float y) {
        float t = (float) (System.currentTimeMillis() % 100000L) / 1000.0F;
        float hue = y * HUE_PER_BLOCK + t * HUE_SPEED * 0.02F;
        hue = hue - (float) Math.floor(hue);
        return hue;
    }

    /** HSV → RGB（0..255），饱和度和明度固定为 1，这样彩虹最干净。 */
    private static int[] hsv(float hue, float sat, float val) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int i = (int) h;
        float f = h - i;
        float p = val * (1.0F - sat);
        float q = val * (1.0F - sat * f);
        float t = val * (1.0F - sat * (1.0F - f));
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = val; g = t; b = p; }
            case 1 -> { r = q; g = val; b = p; }
            case 2 -> { r = p; g = val; b = t; }
            case 3 -> { r = p; g = q; b = val; }
            case 4 -> { r = t; g = p; b = val; }
            default -> { r = val; g = p; b = q; }
        }
        return new int[]{
                Math.round(r * 255.0F),
                Math.round(g * 255.0F),
                Math.round(b * 255.0F)
        };
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        this.delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        this.delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }
}
