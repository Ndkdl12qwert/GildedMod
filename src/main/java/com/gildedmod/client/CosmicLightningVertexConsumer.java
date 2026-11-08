package com.gildedmod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.LightningBolt;

/**
 * 把闪电的几何喂给「鎏金闪电」管线。
 *
 * <p>原版 {@code LightningBoltRenderer} 写出的是 {@code POSITION_COLOR}（没有 UV、没有光照），
 * 而宇宙着色器要的是 {@code POSITION_COLOR_TEX_LIGHTMAP}，所以这里补两样东西：
 * <ul>
 *     <li><b>UV</b>：由顶点相对闪电原点的偏移现算 —— 着色器把 {@code texCoord0} 当作
 *         "天空窗口坐标"，喂进有变化的 UV，整条闪电才会铺满星空，而不是整根一个颜色；</li>
 *     <li><b>光照</b>：直接写满亮（{@code 0xF000F0}），闪电不该受环境光影响。</li>
 * </ul>
 *
 * <p>颜色被强行改成白色：着色器里 {@code shade = vertexColor*0.25 + 0.75}，
 * 原版那身 0.45 的青白会把鎏金效果压暗，所以不让它参与。
 *
 * <p>alpha 无所谓 —— 星空层的最终透明度由遮罩决定（这里绑的是纯白遮罩 → 整条都出星空）。
 */
public class CosmicLightningVertexConsumer implements VertexConsumer {

    /**
     * 竖直方向每格世界坐标对应多少 UV。
     * <p><b>这个值必须小</b> —— 着色器里有一道"银河带"闸门（cosmic.fsh:160）：
     * <pre>(1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5)))</pre>
     * 其中 {@code rawv = 0.5 + asin(ray.y)/π}，而 {@code ray.y} 由
     * {@code win.y = (0.5 - texCoord0.y) * 0.62} 决定 ——
     * 也就是<b>只有当采样方向在水平面 ±27° 以内时星星才可见</b>，超出直接变 0。
     * <p>0.08 时竖直方向 {@code |ray.y| ≈ 0.29}（6 格高的闪电顶端）→ 闸门仍为 1 ✓；
     * 一旦提到 0.30 → 闸门 = 0 → <b>整条闪电全黑</b> ✗（踩过这个坑）。
     */
    private static final float UV_PER_BLOCK_V = 0.08F;

    /**
     * 横向每格 UV —— <b>必须取竖直方向的两倍</b>。
     * <p>原因在着色器的映射（cosmic.fsh:106-107）：
     * <pre>rawu = 0.5 + atan(ray.z, ray.x) / (2π)    // 经度周期 2π
     * rawv = 0.5 + asin(ray.y) / π              // 纬度周期 π</pre>
     * 这是等距柱状投影，天然是 <b>2:1</b>。若横竖用同一个比例，两个方向的角度变化速率会差一倍，
     * 星星就会被沿一个方向拉长 ✗。横向乘 2 之后，闪电表面映射到星空是各向同性的 ✓。
     */
    private static final float UV_PER_BLOCK_U = UV_PER_BLOCK_V * 2.0F;
    /** 满亮光照值（见 LightTexture，两个 0xF 打包）。 */
    private static final int FULL_BRIGHT = 0xF000F0;

    private final VertexConsumer delegate;
    private final double originX;
    private final double originY;
    private final double originZ;
    /** 目标顶点格式里有没有 lightmap（UV2）元素：星空那套有 ✓，金液那套没有 ✗。 */
    private final boolean emitLightmap;

    private float lastU;
    private float lastV;

    public CosmicLightningVertexConsumer(VertexConsumer delegate, LightningBolt bolt) {
        this(delegate, bolt, true);
    }

    public CosmicLightningVertexConsumer(VertexConsumer delegate, LightningBolt bolt, boolean emitLightmap) {
        this.delegate = delegate;
        this.originX = bolt.getX();
        this.originY = bolt.getY();
        this.originZ = bolt.getZ();
        this.emitLightmap = emitLightmap;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        // 用相对闪电原点的偏移当 UV：横向取 x/z 中变化更大的那个，纵向取 y。
        // 横向用 2 倍比例（见 UV_PER_BLOCK_U 的说明），否则等距柱状投影会把星星拉长。
        double dx = x - originX;
        double dz = z - originZ;
        double span = Math.abs(dx) >= Math.abs(dz) ? dx : dz;
        this.lastU = (float) (span * UV_PER_BLOCK_U) + 0.5F;
        this.lastV = (float) ((y - originY) * UV_PER_BLOCK_V) + 0.5F;
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        // 忽略原版颜色，交给着色器出鎏金星空
        this.delegate.color(255, 255, 255, 255);
        return this;
    }

    @Override
    public VertexConsumer color(float red, float green, float blue, float alpha) {
        this.delegate.color(1.0F, 1.0F, 1.0F, 1.0F);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        // 原版闪电不提供 UV，这里用现算的那份
        this.delegate.uv(this.lastU, this.lastV);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        if (this.emitLightmap) {
            this.delegate.uv2(u, v);
        }
        return this;
    }

    @Override
    public void endVertex() {
        // 原版不会调 uv()/uv2()，所以在收尾前把缺的两项补齐
        this.delegate.uv(this.lastU, this.lastV);
        if (this.emitLightmap) {
            this.delegate.uv2(FULL_BRIGHT);   // 金液那套格式里没有 UV2，写了会出错
        }
        this.delegate.endVertex();
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        this.delegate.defaultColor(255, 255, 255, 255);
    }

    @Override
    public void unsetDefaultColor() {
        this.delegate.unsetDefaultColor();
    }
}
