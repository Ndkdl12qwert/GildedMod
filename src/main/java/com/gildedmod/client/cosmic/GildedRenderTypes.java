package com.gildedmod.client.cosmic;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * 注意：{@code TRANSLUCENT_TRANSPARENCY} / {@code LIGHTMAP} / {@code EQUAL_DEPTH_TEST}
 * 在 {@code RenderStateShard} 里是 protected，只有 RenderType 的子类才能直接引用，
 * 所以本类必须 extends RenderType（和 {@code CosmicRenderType} 一样）。
 */
public class GildedRenderTypes extends RenderType {

    public static final RenderType COSMIC = RenderType.create(
            "gildedmod_cosmic",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            2097152,
            true,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> GildedShaders.COSMIC_SHADER))
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .createCompositeState(true)
    );

    private GildedRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                              int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                              Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
