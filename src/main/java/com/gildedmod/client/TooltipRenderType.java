package com.gildedmod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * 工具提示面板用的 {@link RenderType}（金液流动背景）。
 *
 * <p>要点：
 * <ul>
 *     <li>顶点格式 {@code POSITION_COLOR_TEX} 必须和 {@code shaders/core/tooltip.json} 里的
 *         attributes（Position / Color / UV0）完全一致；</li>
 *     <li><b>不设 lightmapState</b> —— GUI 里没有光照贴图，设了会采到无效值；</li>
 *     <li>不设 textureState —— 这个效果是纯程序化生成的，不需要任何贴图；</li>
 *     <li>{@code COLOR_WRITE}：只写颜色不写深度，面板不会挡住别的 GUI 元素；</li>
 *     <li>GUI 里通过 {@code GuiGraphics.bufferSource()} 拿缓冲、画一个覆盖面板的 quad 即可。</li>
 * </ul>
 */
public class TooltipRenderType extends RenderType {

    public static final RenderType TOOLTIP = RenderType.create(
            "gildedmod_tooltip",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> GildedModClient.tooltipShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );

    private TooltipRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                              int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                              Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
