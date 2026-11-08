package com.gildedmod.mixin;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.client.CosmicLightningVertexConsumer;
import com.gildedmod.client.CosmicRenderType;
import com.gildedmod.client.CosmicTextures;
import com.gildedmod.client.GildedModClient;
import com.gildedmod.client.RainbowLightningVertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 闪电换皮：默认走「鎏金闪电」—— 直接用我们自己的宇宙着色器画整条闪电
 * （暗金底 + 星空 + 加色混合 = 发光的金色星带）。
 *
 * <p>为什么这么省事：原版 {@code LightningBoltRenderer} 整个渲染过程里只有
 * <b>一次</b> {@code MultiBufferSource.getBuffer(RenderType.lightning())}（反编译确认：偏移 95），
 * 之后所有面片都写进同一个 {@code VertexConsumer} → 只要把这次 getBuffer 换成我们的
 * {@link CosmicRenderType#COSMIC_LIGHTNING}，整条闪电就走我们的管线了。
 *
 * <p>两个坑：
 * <ol>
 *     <li>原版闪电只有 {@code POSITION_COLOR}，缺 UV 和光照 → 由
 *         {@link CosmicLightningVertexConsumer} 补齐；</li>
 *     <li>几何进了批量缓冲后是<b>稍后</b>才真正绘制的，所以采样器/ uniform 必须在
 *         "立刻 flush"的那一刻绑好 → 见下面的 {@code RETURN} 注入。</li>
 * </ol>
 */
@Mixin(LightningBoltRenderer.class)
public class LightningBoltRendererMixin {

    /**
     * true = 金液（工具提示那套 {@code gildedmod:tooltip} 着色器，黑金流动）；
     * false = 星空（{@code gildedmod:cosmic} 那套，需要绑两张贴图）—— 当前用这个。
     */
    @Unique
    private static final boolean GILDEDMOD_LIQUID_LIGHTNING = false;

    @Unique
    private static boolean gildedmod$loggedLightning;

    /**
     * 目标方法（public，官方映射里有 SRG 名，注解处理器不会像盔甲那次报 mapping 错误）：
     * {@code public void render(LightningBolt, float, float, PoseStack, MultiBufferSource, int)}
     */
    @Redirect(method = "render(Lnet/minecraft/world/entity/LightningBolt;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer gildedmod$lightningBuffer(MultiBufferSource buffer, RenderType renderType,
                                                     LightningBolt bolt, float yaw, float partialTicks,
                                                     PoseStack poseStack, MultiBufferSource originalBuffer,
                                                     int packedLight) {
        // 只处理天罚召唤的闪电：用会同步到客户端的自定义名当标记。
        // （踩过的坑：一开始用 bolt.getCause() 判断 ✗ —— 那是纯服务端字段，
        //   客户端永远是 null，结果天罚的闪电被判成"自然闪电"，全都变回原版了。）
        if (!gildedmod$isGilded(bolt)) {
            return buffer.getBuffer(renderType);
        }
        if (GILDEDMOD_LIQUID_LIGHTNING) {
            // 金液着色器不需要光照贴图（顶点格式里也没有 UV2）
            return new CosmicLightningVertexConsumer(
                    buffer.getBuffer(CosmicRenderType.COSMIC_LIQUID), bolt, false);
        }
        return new CosmicLightningVertexConsumer(
                buffer.getBuffer(CosmicRenderType.COSMIC), bolt);
    }

    @Unique
    private static boolean gildedmod$isGilded(LightningBolt bolt) {
        var name = bolt.getCustomName();
        return name != null && BlackGoldSword.GILDED_LIGHTNING_TAG.equals(name.getString());
    }

    /**
     * 几何写完后立刻设好 uniform 并 flush 这一批。
     * 之所以要"立刻"：几何进了批量缓冲是稍后才真正绘制的，uniform 必须在
     * 着色器已成为当前程序、且马上要绘制的那一刻写入 —— 见下面 apply() 的顺序。
     * <p>金液着色器是纯程序化的（无贴图），所以这里<b>不需要绑任何 sampler</b>。
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/LightningBolt;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void gildedmod$flushLightning(LightningBolt bolt, float yaw, float partialTicks,
                                          PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                          CallbackInfo ci) {
        if (!gildedmod$isGilded(bolt)) return;   // 自然闪电：什么都没写进我们的批次，直接跳过
        if (!(buffer instanceof MultiBufferSource.BufferSource bufferSource)) return;

        if (GILDEDMOD_LIQUID_LIGHTNING) {
            ShaderInstance liquid = GildedModClient.tooltipShader;
            if (liquid == null) return;
            liquid.apply();
            gildedmod$set(liquid, "time", (float) (System.currentTimeMillis() % 100000L) / 1000.0F);
            // panelSize 决定金液丝缕的尺度（闪电约 0.5 格宽、6 格高，这里放大到看得清丝缕）
            gildedmod$set2(liquid, "panelSize", 26.0F, 110.0F);
            bufferSource.endBatch(CosmicRenderType.COSMIC_LIQUID);
            gildedmod$logOnce(liquid.getName());
            return;
        }

        ShaderInstance shader = GildedModClient.cosmicShader;
        AbstractTexture stars = CosmicTextures.stars();
        AbstractTexture white = CosmicTextures.whiteMask();
        if (shader == null || stars == null || white == null) return;

        // 先绑采样器再 apply（顺序反了会采到上一帧残留的贴图）
        shader.setSampler("Sampler0", white);
        shader.setSampler("Sampler1", stars);
        RenderSystem.setShaderTexture(0, white.getId());
        RenderSystem.setShaderTexture(1, stars.getId());
        shader.apply();

        // uniform 要在着色器已成为当前程序时写入
        gildedmod$set(shader, "time", (float) (System.currentTimeMillis() % 100000L) / 1000.0F);
        gildedmod$set(shader, "armorMode", 0.0F);   // 0 = 用 texCoord0 当天空窗口
        // 闪电固定用默认的金色星空（starMode 是按物品设置的状态，必须清掉，否则会残留彩虹模式）
        gildedmod$set(shader, "starMode", 0.0F);
        gildedmod$set(shader, "yaw", 0.0F);
        gildedmod$set(shader, "pitch", 0.0F);
        // 星图采样密度：这才是让"星星更明显"的正确旋钮 ——
        // 它只放大星图坐标（u/v 都乘它），不动射线方向，因此不会把闪电推出"银河带"闸门。
        gildedmod$set(shader, "externalScale", 3.5F);
        gildedmod$set(shader, "opacity", 1.0F);
        // ColorModulator 是在着色器 clamp 之后才相乘的 → 用它把星星提亮（1.6 倍）
        var modulator = shader.safeGetUniform("ColorModulator");
        if (modulator != null) {
            modulator.set(1.6F, 1.6F, 1.6F, 1.0F);
        }

        bufferSource.endBatch(CosmicRenderType.COSMIC);
        gildedmod$logOnce(shader.getName());
    }

    @Unique
    private static void gildedmod$logOnce(String shaderName) {
        if (!gildedmod$loggedLightning) {
            gildedmod$loggedLightning = true;
            System.out.println("[GildedMod] 鎏金闪电已启用: shader=" + shaderName);
        }
    }

    @Unique
    private static void gildedmod$set(ShaderInstance shader, String name, float value) {
        var uniform = shader.safeGetUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    @Unique
    private static void gildedmod$set2(ShaderInstance shader, String name, float a, float b) {
        var uniform = shader.safeGetUniform(name);
        if (uniform != null) {
            uniform.set(a, b);
        }
    }
}
