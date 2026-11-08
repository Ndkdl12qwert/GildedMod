package com.gildedmod.client;

import com.gildedmod.GildedMod;
import com.gildedmod.client.cosmic.GildedShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.EventPriority;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GildedMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GildedModClient {

    public static ShaderInstance cosmicShader;
    public static ShaderInstance tooltipShader;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(GildedMod.MODID, "cosmic"),
                        DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                ),
                shader -> cosmicShader = shader
        );
        // 工具提示面板的金液流动（Position / Color / UV0，GUI 里不需要 lightmap）
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(GildedMod.MODID, "tooltip"),
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                shader -> tooltipShader = shader
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 星空用的两张贴图不进图集，必须自己读 PNG 上传，否则 sampler 只会绑到“丢失贴图”。
        event.enqueueWork(CosmicTextures::register);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            for (int i = 0; i < GildedShaders.COSMIC_SPRITES.length; i++) {
                GildedShaders.COSMIC_SPRITES[i] = event.getAtlas().getSprite(
                        ResourceLocation.fromNamespaceAndPath("gildedmod", "misc/cosmic/cosmic_" + i));
                GildedShaders.COSMIC_UVS[i * 4] = GildedShaders.COSMIC_SPRITES[i].getU0();
                GildedShaders.COSMIC_UVS[i * 4 + 1] = GildedShaders.COSMIC_SPRITES[i].getV0();
                GildedShaders.COSMIC_UVS[i * 4 + 2] = GildedShaders.COSMIC_SPRITES[i].getU1();
                GildedShaders.COSMIC_UVS[i * 4 + 3] = GildedShaders.COSMIC_SPRITES[i].getV1();
            }
        }
    }
}