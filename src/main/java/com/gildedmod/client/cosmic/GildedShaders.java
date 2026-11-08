package com.gildedmod.client.cosmic;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

public class GildedShaders {

    public static final float[] COSMIC_UVS = new float[40];
    public static TextureAtlasSprite[] COSMIC_SPRITES = new TextureAtlasSprite[10];

    public static ShaderInstance COSMIC_SHADER;

    public static Uniform cosmicTime;
    public static Uniform cosmicYaw;
    public static Uniform cosmicPitch;
    public static Uniform cosmicExternalScale;
    public static Uniform cosmicOpacity;
    public static Uniform cosmicUVs;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath("gildedmod", "cosmic"),
                            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                    ),
                    shader -> {
                        COSMIC_SHADER = shader;
                        cosmicTime = COSMIC_SHADER.getUniform("time");
                        cosmicYaw = COSMIC_SHADER.getUniform("yaw");
                        cosmicPitch = COSMIC_SHADER.getUniform("pitch");
                        cosmicExternalScale = COSMIC_SHADER.getUniform("externalScale");
                        cosmicOpacity = COSMIC_SHADER.getUniform("opacity");
                        cosmicUVs = COSMIC_SHADER.getUniform("cosmicuvs");
                        COSMIC_SHADER.apply();
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}