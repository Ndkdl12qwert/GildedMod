package com.gildedmod.client.cosmic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CosmicBakeModel extends WrappedItemModel {
    private final List<ResourceLocation> maskSprite;

    public CosmicBakeModel(final BakedModel wrapped, final List<ResourceLocation> maskSprite) {
        super(wrapped);
        this.maskSprite = maskSprite;
        this.cosmic = true;
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, PoseStack pStack, MultiBufferSource source,
                           int packedLight, int packedOverlay,
                           ItemModelShaper itemModelShaper, TextureManager textureManager) {
        this.parentState = TransformUtils.DEFAULT_ITEM;

        this.renderWrapped(stack, pStack, source, packedLight, packedOverlay, true);
        if (source instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }

        final Minecraft mc = Minecraft.getInstance();
        float yaw = 0.0f;
        float pitch = 0.0f;
        float scale = 1f;
        if (transformType == ItemDisplayContext.GUI) {
            scale = 100.0F;
        } else {
            yaw = (float) (mc.player.getYRot() * 2.0f * Math.PI / 360.0);
            pitch = -(float) (mc.player.getXRot() * 2.0f * Math.PI / 360.0);
        }

        if (GildedShaders.cosmicTime != null) GildedShaders.cosmicTime.set(mc.level.getGameTime() % Integer.MAX_VALUE);
        if (GildedShaders.cosmicYaw != null) GildedShaders.cosmicYaw.set(yaw);
        if (GildedShaders.cosmicPitch != null) GildedShaders.cosmicPitch.set(pitch);
        if (GildedShaders.cosmicExternalScale != null) GildedShaders.cosmicExternalScale.set(scale);
        if (GildedShaders.cosmicOpacity != null) GildedShaders.cosmicOpacity.set(1.0F);
        if (GildedShaders.cosmicUVs != null) GildedShaders.cosmicUVs.set(GildedShaders.COSMIC_UVS);

        final VertexConsumer cons = source.getBuffer(GildedRenderTypes.COSMIC);
        List<TextureAtlasSprite> atlasSprite = new ArrayList<>();
        for (ResourceLocation res : maskSprite) {
            atlasSprite.add(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(res));
        }
        mc.getItemRenderer().renderQuadList(pStack, cons, bakeItem(atlasSprite), stack, packedLight, packedOverlay);
    }
}