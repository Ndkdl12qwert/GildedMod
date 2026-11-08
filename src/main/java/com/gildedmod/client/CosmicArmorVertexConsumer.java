package com.gildedmod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 盔甲几何用的是 {@code NEW_ENTITY} 顶点格式（比星空层多出 Overlay 和 Normal），
 * 而星空层用的 RenderType 是 {@code POSITION_COLOR_TEX_LIGHTMAP}。
 * <p>这个适配器把盔甲模型写出来的顶点原样转发给星空层的 consumer，
 * 位置 / 颜色 / UV / 光照四项透传，自己用不到的 Overlay 和 Normal 直接丢掉。
 * <p>用法：{@code model.renderToBuffer(poseStack, new CosmicArmorVertexConsumer(cosmicConsumer), ...)}
 */
public class CosmicArmorVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;

    public CosmicArmorVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        // 星空层没有 Overlay 属性，丢掉
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        // 星空层没有 Normal 属性，丢掉
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
