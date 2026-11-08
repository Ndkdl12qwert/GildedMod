package com.gildedmod.mixin;

import com.gildedmod.GildedMod;
import com.gildedmod.client.CosmicRenderType;
import com.gildedmod.client.CosmicTextures;
import com.gildedmod.client.GildedModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 给「鎏金之锭」贴一层星空。
 *
 * 做法：在 {@code ItemRenderer.render} 画完物品本体之后，用同一个姿势矩阵、
 * 同一批顶点位置再画一遍，只把 UV 归一化成 0..1 —— 于是：
 * <ul>
 *     <li>位置和物品表面<b>逐位相等</b>，LEQUAL + 不写深度就能严丝合缝贴在物品上，
 *         不用去猜 item/generated 模型到底落在 z 的哪里，GUI / 手持 / 掉落物全都对得上；</li>
 *     <li>UV 是 0..1，着色器里 {@code texCoord0} 既当遮罩 UV、又当球面方向参数，正好合用。</li>
 * </ul>
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Unique
    private static final Logger GILDEDMOD_LOGGER = LogUtils.getLogger();

    @Unique
    private static boolean gildedmod$warnedNoQuads;

    @Unique
    private static boolean gildedmod$loggedFirstDraw;

    @Unique
    private static boolean gildedmod$loggedFog;

    /** 去重用：上一次画星空层的物品与当时那个 pose 对象。 */
    @Unique
    private static ItemStack gildedmod$lastStack;

    @Unique
    private static PoseStack.Pose gildedmod$lastPose;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            shift = At.Shift.AFTER))
    private void gildedmod$cosmicOverlay(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                         PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                         int packedOverlay, BakedModel model, CallbackInfo ci) {

        // 诊断：第一次画任何物品时报一次当前雾参数（星空层用的是同一个着色器，雾会直接毁掉它）
        if (!gildedmod$loggedFog) {
            gildedmod$loggedFog = true;
            GILDEDMOD_LOGGER.info("[GildedMod] 首次物品渲染的雾参数: start={} end={} 上下文={}",
                    RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(), context);
        }

        // 带星空的物品统一在 CosmicTextures.maskLocation 里登记（加物品只改那一处），
        // 这里拿不到遮罩就说明该物品不带星空层，直接跳过。
        AbstractTexture mask = CosmicTextures.maskFor(stack);
        if (mask == null) return;

        // 诊断开关：/gildeddebug render item 关掉后不再画物品星空层
        if (!com.gildedmod.GildedDebug.isRenderItemStars()) return;

        if (!(buffer instanceof MultiBufferSource.BufferSource bufferSource)) return;

        // 去重：同一次渲染里 renderModelLists 可能被调用多次（黑金剑恒带附魔光效 → 本体 + 光效两遍），
        // 不去重会把半透明星空叠两遍、亮度翻倍。
        // 用「同一个 ItemStack 对象 + 同一个 pose 对象」判断；PoseStack.pushPose() 每次都 new 一个 Pose，
        // 所以下一帧的 pose 一定是新对象，不会误判。
        PoseStack.Pose pose = poseStack.last();
        if (gildedmod$lastStack == stack && gildedmod$lastPose == pose) {
            return;
        }
        gildedmod$lastStack = stack;
        gildedmod$lastPose = pose;

        ShaderInstance shader = GildedModClient.cosmicShader;
        AbstractTexture stars = CosmicTextures.stars();
        if (shader == null || stars == null) return;

        // 1) 先把物品本体这一批真正画出去（写入深度缓冲），星空层才能压在它上面。
        //    否则星空层可能先于物品被提交，被物品反盖掉。
        bufferSource.endBatch();

        // 2) 绑定两张贴图 + uniform
        Minecraft minecraft = Minecraft.getInstance();
        float yaw = 0.0F;
        float pitch = 0.0F;
        if (minecraft.player != null) {
            // 着色器里 fwd = (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))，
            // 与 MC 的约定一致：yaw 0=朝 +Z、yaw 90=朝 -X，pitch 取正值表示低头。
            yaw = (float) Math.toRadians(minecraft.player.getYRot());
            pitch = (float) Math.toRadians(minecraft.player.getXRot());
        }

        // 关键：必须先 setSampler 再 apply()。
        // apply() 才是把采样器真正绑到纹理单元的那一步；顺序反了的话，这一帧绑上去的
        // 是上一帧残留在单元上的纹理 —— 实际就是物品本体渲染留下的方块图集，
        // 于是"星星"变成物品贴图碎片、遮罩也随机裁形。
        shader.setSampler("Sampler0", mask);
        shader.setSampler("Sampler1", stars);
        // 双保险：直接按纹理单元绑一次原始纹理 id（setSampler 走的是着色器内部的采样器表）
        RenderSystem.setShaderTexture(0, mask.getId());
        RenderSystem.setShaderTexture(1, stars.getId());
        shader.apply();
        gildedmod$set(shader, "time", (float) (System.currentTimeMillis() % 100000L) / 1000.0F);
        gildedmod$set(shader, "yaw", yaw);
        gildedmod$set(shader, "pitch", pitch);
        gildedmod$set(shader, "externalScale", 1.0F);
        gildedmod$set(shader, "opacity", 1.0F);
        // 物品模式：方向来自面内 UV（"窗户"），不是相机空间视线（那是盔甲用的）
        gildedmod$set(shader, "armorMode", 0.0F);
        // 终极宇宙模板单独一套星空：纯黑底 + 图集元素彩色渐变（见 cosmic.fsh 的 starMode 分支）
        boolean rainbowStars = CosmicTextures.maskLocation(stack) == CosmicTextures.FINAL_UNIVERSE_TEMPLATE_MASK;
        gildedmod$set(shader, "starMode", rainbowStars ? 1.0F : 0.0F);

        // 3) 收集物品模型自己的面
        boolean collectStats = !gildedmod$loggedFirstDraw;
        StringBuilder sides = collectStats ? new StringBuilder() : null;
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction side : Direction.values()) {
            List<BakedQuad> list = gildedmod$quads(model, side);
            quads.addAll(list);
            if (sides != null) {
                sides.append(side.getSerializedName()).append(':').append(list.size()).append(' ');
            }
        }
        List<BakedQuad> unculled = gildedmod$quads(model, null);
        quads.addAll(unculled);
        if (sides != null) {
            sides.append("null:").append(unculled.size());
        }

        // 4) 按“顶点位置”去重后再画。
        //    位置完全相同的面只画一次：它们在屏幕上完全重合，画多次等于把半透明星空叠加多次（星星会过亮、糊住物品本体）。
        VertexConsumer consumer = bufferSource.getBuffer(CosmicRenderType.COSMIC);
        List<int[]> seenPositions = new ArrayList<>();
        Set<Integer> distinctZ = collectStats ? new HashSet<>() : null;
        Set<Integer> distinctY = collectStats ? new HashSet<>() : null;
        int unique = 0;
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            if (stride < 6) continue;
            int[] key = new int[12];
            for (int i = 0; i < 4; i++) {
                key[i * 3] = vertices[i * stride];
                key[i * 3 + 1] = vertices[i * stride + 1];
                key[i * 3 + 2] = vertices[i * stride + 2];
            }
            if (collectStats) {
                distinctZ.add(vertices[2]);
                distinctY.add(vertices[1]);
            }
            boolean duplicate = false;
            for (int[] other : seenPositions) {
                if (Arrays.equals(other, key)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;
            seenPositions.add(key);
            if (gildedmod$emitQuad(quad, poseStack, consumer, packedLight)) {
                unique++;
            }
        }

        // 5) 立刻提交星空层
        bufferSource.endBatch();

        if (unique == 0) {
            if (!gildedmod$warnedNoQuads) {
                gildedmod$warnedNoQuads = true;
                GILDEDMOD_LOGGER.warn("[GildedMod] 鎏金之锭的模型没有可用面，星空层画不出来");
            }
            return;
        }

        // 只在第一次成功绘制时报一行，方便确认贴图来源和模型的真实几何
        if (collectStats) {
            gildedmod$loggedFirstDraw = true;
            GILDEDMOD_LOGGER.info("[GildedMod] 星空层已挂载 ({}): mask={} stars={} 原始面={} 位置去重后={} 不同z={} 不同y={} [{}]",
                    stack.getHoverName().getString(),
                    mask.getClass().getSimpleName(), stars.getClass().getSimpleName(),
                    quads.size(), unique, distinctZ.size(), distinctY.size(), sides);
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
    private static List<BakedQuad> gildedmod$quads(BakedModel model, Direction side) {
        try {
            List<BakedQuad> quads = model.getQuads(null, side, RandomSource.create());
            return quads == null ? List.of() : quads;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * 把物品模型的一个面改写成星空层：位置原样复制，UV 归一化到 0..1。
     *
     * <b>关键：要按“这张贴图自己的矩形”({@code getU0/V0/U1/V1}) 来归一化，不能按这个面自己的
     * min/max。</b> 原版 {@code item/generated} 会把贴图按行切成一条条横带，每条横带只占贴图的
     * 一小段 v；按面自己归一化的话，每条横带里的 v 都会被拉成 0..1，
     * 遮罩和星空就被压扁成十几条横纹了。
     */
    @Unique
    private static boolean gildedmod$emitQuad(BakedQuad quad, PoseStack poseStack,
                                              VertexConsumer consumer, int packedLight) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        if (stride < 6) return false;

        TextureAtlasSprite sprite = quad.getSprite();
        if (sprite == null) return false;
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();
        float du = u1 - u0;
        float dv = v1 - v0;
        if (du <= 1.0E-6F || dv <= 1.0E-6F) return false;

        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < 4; i++) {
            int base = i * stride;
            float x = Float.intBitsToFloat(vertices[base]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);
            float u = (Float.intBitsToFloat(vertices[base + 4]) - u0) / du;
            float v = (Float.intBitsToFloat(vertices[base + 5]) - v0) / dv;
            consumer.vertex(matrix, x, y, z)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(u, v)
                    .uv2(packedLight)
                    .endVertex();
        }
        return true;
    }
}
