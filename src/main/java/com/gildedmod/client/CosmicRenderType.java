package com.gildedmod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * 星空叠层用的 {@link RenderType}。
 *
 * 要点：
 * <ul>
 *     <li>顶点格式必须和 {@code shaders/core/cosmic.json} 里的 attributes 完全一致
 *         （Position / Color / UV0 / UV2），否则打包出来的顶点数据会被着色器读错；</li>
 *     <li>{@code COLOR_WRITE}：只写颜色不写深度，星空层不会挡住后面的东西；</li>
 *     <li>{@code LEQUAL_DEPTH_TEST}：星空层的顶点直接复用物品模型自己的顶点，
 *         深度值和物品表面完全相等，LEQUAL 才能贴着物品面画上去；</li>
 *     <li>{@code NO_CULL}：正反面都要贴（掉落物/展示框会看到背面）；</li>
 *     <li>不设置 textureState —— 两张贴图由 {@code ItemRendererMixin} 手动
 *         {@code setSampler} 绑到 Sampler0 / Sampler1，加了会被图集覆盖。</li>
 * </ul>
 */
public class CosmicRenderType extends RenderType {

    public static final RenderType COSMIC = RenderType.create(
            "gildedmod_cosmic",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> GildedModClient.cosmicShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
    );

    /**
     * 「鎏金闪电」用的变体：着色器、顶点格式、深度设置都和 {@link #COSMIC} 一样，
     * 唯独把混合换成 {@code LIGHTNING_TRANSPARENCY}（{@code SRC_ALPHA, ONE} 加色混合）。
     * <p>加色是关键：星空层的底色是暗金、星点是亮金，加色叠到世界上就成了一道
     * <b>发光</b>的金色星带，而不是一块贴在画面上的暗色贴片。
     */
    public static final RenderType COSMIC_LIGHTNING = RenderType.create(
            "gildedmod_cosmic_lightning",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> GildedModClient.cosmicShader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
    );

    /**
     * 「鎏金闪电」用的金液变体：改用工具提示面板那套 {@code gildedmod:tooltip} 着色器
     * （FBM 噪声 + 域扭曲的黑金流动）。顶点格式随之变成 {@code POSITION_COLOR_TEX}
     * —— 该着色器是纯程序化的，<b>不需要任何贴图</b>，所以不用绑 sampler、也不需要 lightmap。
     */
    public static final RenderType COSMIC_LIQUID = RenderType.create(
            "gildedmod_cosmic_liquid",
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

    private CosmicRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                             int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                             Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
