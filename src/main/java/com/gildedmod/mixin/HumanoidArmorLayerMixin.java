package com.gildedmod.mixin;

import com.gildedmod.BlackGoldArmor;
import com.gildedmod.client.CosmicArmorVertexConsumer;
import com.gildedmod.client.CosmicRenderType;
import com.gildedmod.client.CosmicTextures;
import com.gildedmod.client.GildedModClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让<b>穿在身上</b>的黑金盔甲也带星空层。
 *
 * <p>为什么物品那套办法不能照搬：
 * <ul>
 *     <li>盔甲不走 {@code ItemRenderer}，走 {@code HumanoidArmorLayer}，几何是
 *         {@code ModelPart}/{@code Polygon} 而不是 {@code BakedQuad}；</li>
 *     <li>它的顶点格式是 {@code NEW_ENTITY}（多 Overlay / Normal），和星空层的
 *         {@code POSITION_COLOR_TEX_LIGHTMAP} 不同 → 用 {@link CosmicArmorVertexConsumer} 桥接；</li>
 *     <li>它的 {@code texCoord0} 是 64x32 图上的 <b>UV 岛</b>，拿它当"窗户坐标"会在每个部件上
 *         各开一个小窗、转视角也不连贯 → 方向改用相机空间的 {@code fPos}
 *         （着色器里 {@code armorMode = 1} 走这一支）。</li>
 * </ul>
 *
 * <p>做法反而比物品简单：不用抄顶点，直接把<b>同一个 Model</b> 用星空层的 RenderType 再画一遍。
 *
 * <h3>注入点是怎么定下来的（踩了两个坑，记下来免得以后再踩）</h3>
 * <ol>
 *     <li>这个类里有<b>两个</b> {@code renderModel} 重载。带 {@code ResourceLocation} 的那个才是
 *         {@code renderArmorPiece} 实际调用的（反编译确认：偏移 172/200/230 三处都调它），
 *         带 {@code String} 的那个<b>根本没人调用</b>。</li>
 *     <li>但带 {@code ResourceLocation} 的那个在官方映射表里<b>没有 SRG 名</b>
 *         （查 srg_to_official 确认，只有 {@code m_289609_} = 带 String 的那个），
 *         拿它当 {@code @Inject} 目标时注解处理器直接报
 *         {@code Unable to locate obfuscation mapping}。→ 先瞄 String 版试过，编译过但运行期
 *         一次都没触发（日志里没有任何探针行），这才回头查字节码发现瞄错了。</li>
 *     <li>最终改成瞄 {@code renderArmorPiece}（<b>被映射</b>：{@code m_117118_}），
 *         它的 RETURN 正好是"这件盔甲已经画完"的时刻，而且
 *         {@code renderModel} 里<b>没有任何 pushPose/translate/scale</b>（字节码确认只有
 *         getBuffer + renderToBuffer 两步），所以这里的姿势栈和刚才画盔甲时完全一致，
 *         直接复用即可，不用抄变换。</li>
 * </ol>
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Unique
    private static final Logger GILDEDMOD_LOGGER = LogUtils.getLogger();

    @Unique
    private static boolean gildedmod$loggedArmor;

    /** 探针用：分别只记一次，用来区分"没被调用"和"被哪个条件挡掉了"。 */
    @Unique
    private static boolean gildedmod$loggedProbe;

    @Unique
    private static boolean gildedmod$loggedBuffer;

    @Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("RETURN"))
    private void gildedmod$cosmicArmor(PoseStack poseStack, MultiBufferSource buffer, LivingEntity entity,
                                       EquipmentSlot slot, int packedLight, HumanoidModel<?> model,
                                       CallbackInfo ci) {

        ItemStack stack = entity.getItemBySlot(slot);

        // 探针（放在所有 return 之前）：只要这行出现，就说明注入成功、盔甲确实在渲染
        if (!gildedmod$loggedProbe) {
            gildedmod$loggedProbe = true;
            GILDEDMOD_LOGGER.info("[GildedMod] 盔甲层探针: 实体={} 槽位={} 物品={} 物品类={} 模型={} buffer={}",
                    entity.getType().toShortString(), slot, stack, stack.getItem().getClass().getSimpleName(),
                    model.getClass().getSimpleName(), buffer.getClass().getSimpleName());
        }

        if (!(stack.getItem() instanceof BlackGoldArmor armorItem)) return;

        // 诊断开关：/gildeddebug render off 之后整层不画（用来判断卡顿是否来自渲染）
        if (!com.gildedmod.GildedDebug.isRenderArmorStars()) return;

        if (!(buffer instanceof MultiBufferSource.BufferSource)) {
            if (!gildedmod$loggedBuffer) {
                gildedmod$loggedBuffer = true;
                GILDEDMOD_LOGGER.info("[GildedMod] 盔甲层被跳过: buffer 不是 BufferSource，而是 {}",
                        buffer.getClass().getName());
            }
            return;
        }
        MultiBufferSource.BufferSource bufferSource = (MultiBufferSource.BufferSource) buffer;

        // 腿部件用 layer_2，其余用 layer_1（和原版 usesInnerModel 一致）
        boolean legs = slot == EquipmentSlot.LEGS;
        ResourceLocation layerTexture = gildedmod$layerTexture(armorItem, legs);
        if (layerTexture == null) return;

        ShaderInstance shader = GildedModClient.cosmicShader;
        AbstractTexture stars = CosmicTextures.stars();
        // 盔甲遮罩：拿 layer 图自己的 alpha 现合成（黑金盔甲暗色像素 R 也低，不能用 R 通道）
        AbstractTexture mask = CosmicTextures.armorMask(layerTexture);
        if (shader == null || stars == null || mask == null) return;

        // 1) 先把"这一件盔甲"这一批画出去（写深度），星空层才能压在它上面。
        //    ⚠ 必须用 endLastBatch()：无参 endBatch() 会把整帧所有 RenderType 全部刷出去 ✗，
        //    而盔甲层是在实体渲染中途被调用的 —— 整帧顺序被打乱后，
        //    半透明的星空层（COSMIC 是 TRANSLUCENT、按类型排序）就会一会儿在前一会儿在后，
        //    表现出来就是"紧身全套时效果一闪一闪" ✗。
        bufferSource.endLastBatch();

        // 2) 绑贴图 + uniform。顺序同物品：必须先 setSampler 再 apply()，
        //    否则这一帧绑上去的是上一帧残留在纹理单元上的贴图。
        shader.setSampler("Sampler0", mask);
        shader.setSampler("Sampler1", stars);
        RenderSystem.setShaderTexture(0, mask.getId());
        RenderSystem.setShaderTexture(1, stars.getId());
        shader.apply();
        gildedmod$set(shader, "time", (float) (System.currentTimeMillis() % 100000L) / 1000.0F);
        gildedmod$set(shader, "armorMode", 1.0F);
        // 盔甲永远用默认的金色星空（starMode 是"按物品"的一个状态，
        // 不显式清掉的话会残留上一个物品的彩虹模式 ✗）
        gildedmod$set(shader, "starMode", 0.0F);
        gildedmod$set(shader, "externalScale", 1.0F);
        gildedmod$set(shader, "opacity", 1.0F);

        // 3) 用星空层的 RenderType 把同一个 Model 再画一遍（顶点经适配器丢掉 Overlay/Normal）。
        //    姿势栈和刚才原版画盔甲时完全一致（renderModel 里没有额外变换），直接复用。
        model.renderToBuffer(poseStack,
                new CosmicArmorVertexConsumer(bufferSource.getBuffer(CosmicRenderType.COSMIC)),
                packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        // 同样只刷"星空层"这一批（现在是 last），不要牵连整帧
        bufferSource.endLastBatch();

        if (!gildedmod$loggedArmor) {
            gildedmod$loggedArmor = true;
            GILDEDMOD_LOGGER.info("[GildedMod] 黑金盔甲星空层已挂载: 槽位={} layer={} 遮罩={}",
                    slot, layerTexture, mask);
        }
    }

    /**
     * 按原版 {@code HumanoidArmorLayer.getArmorLocation} 的命名公式推出 layer 图位置：
     * {@code <命名空间>:textures/models/armor/<材质名>_layer_<1|2>.png}（腿部件用 layer_2）。
     */
    @Unique
    private static ResourceLocation gildedmod$layerTexture(ArmorItem armorItem, boolean legs) {
        ResourceLocation material = ResourceLocation.tryParse(armorItem.getMaterial().getName());
        if (material == null) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(material.getNamespace(),
                "textures/models/armor/" + material.getPath() + "_layer_" + (legs ? 2 : 1) + ".png");
    }

    @Unique
    private static void gildedmod$set(ShaderInstance shader, String name, float value) {
        var uniform = shader.safeGetUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
