package com.gildedmod.client;

import com.gildedmod.GildedMod;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 星空渲染用到的“独立贴图”（都不进方块图集）：
 * <ul>
 *     <li>{@link #MASK} —— 鎏金之锭的轮廓遮罩；</li>
 *     <li>{@link #SWORD_MASK} —— 黑金剑的轮廓遮罩（换物品只要在这里加一张图 + 在
 *         {@code ItemRendererMixin} 里多一个分支）；</li>
 *     <li>{@link #STARS} —— 星空条带（16x160 = 10 帧竖排），着色器按帧号取其中一格。</li>
 * </ul>
 *
 * <b>星空条带的真正出处是 {@code textures/misc/cosmic/cosmic_0..9.png} 这 10 张 16x16 图</b>
 * （着色器里 {@code cosmiccount = 10}、v 表按 10 段切分，正好一一对应）。
 * 所以这里启动时把这 10 张在内存里竖着拼成 16x160 再上传；只有拼不出来时，
 * 才退回直接读 {@code textures/effect/cosmic.png}（那张就是拼好的成品）。
 *
 * 图集里的贴图由 {@code TextureManager} 自动加载，独立贴图不会：直接拿
 * {@code getTexture(loc)} 只会拿到“紫黑丢失贴图”，着色器采到的就是那片丢失图案。
 * 所以这里自己读 PNG、自己上传，key 用 {@code textures/xxx.png} 这种全路径。
 */
public final class CosmicTextures {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 鎏金之锭的遮罩。 */
    public static final ResourceLocation MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/gilded_ingot_mask.png");
    /** 黑金剑的遮罩。 */
    public static final ResourceLocation SWORD_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/blackgold_sword_mask.png");
    /** 黑金盔甲四件套的遮罩（16x16，用在物品栏图标上）。 */
    /** 终极宇宙鎏金升级模板：它自己是"黑底 + 彩色渐变星"的那套星空（见 cosmic.fsh 的 starMode）。 */
    public static final ResourceLocation FINAL_UNIVERSE_TEMPLATE_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod",
                    "textures/mask/final_universe_gilded_template_mask.png");

    public static final ResourceLocation UNIOXING_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/unioxing_mask.png");

    public static final ResourceLocation FUND_SWORD_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/fund_sword_mask.png");

    public static final ResourceLocation HELMET_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/blackgold_helmet_mask.png");
    public static final ResourceLocation CHESTPLATE_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/blackgold_chestplate_mask.png");
    public static final ResourceLocation LEGGINGS_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/blackgold_leggings_mask.png");
    public static final ResourceLocation BOOTS_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/mask/blackgold_boots_mask.png");
    /** 星空条带（由 misc/cosmic 下 10 张图拼成，注册在这个 key 上）。 */
    public static final ResourceLocation STARS =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "textures/effect/cosmic.png");

    /**
     * 纯白遮罩：闪电这种"没有轮廓遮罩"的几何用它，等于"整块都出星空"。
     * 是运行时合成的 16x16 全白图，不是资源文件。
     */
    public static final ResourceLocation WHITE_MASK =
            ResourceLocation.fromNamespaceAndPath("gildedmod", "cosmic_mask/white");

    /** 所有需要预加载的遮罩。加物品时：这里补一张图 + 在 {@link #maskLocation} 补一行。 */
    private static final java.util.List<ResourceLocation> MASKS = java.util.List.of(
            MASK, SWORD_MASK, FUND_SWORD_MASK, UNIOXING_MASK, FINAL_UNIVERSE_TEMPLATE_MASK,
        HELMET_MASK, CHESTPLATE_MASK, LEGGINGS_MASK, BOOTS_MASK);

    /** 10 张星空图的原始目录：textures/misc/cosmic/cosmic_0.png ... cosmic_9.png */
    private static final String TILE_PREFIX = "textures/misc/cosmic/cosmic_";
    private static final int TILE_COUNT = 10;
    private static final int TILE_SIZE = 16;

    /** 惰性加载只尝试一次，避免资源真的缺失时每帧刷屏。 */
    private static final Set<ResourceLocation> TRIED = new HashSet<>();

    /**
     * 已经成功注册过的 key。合成的遮罩（盔甲那几张）用的是虚拟位置，资源包里并不存在，
     * 若走 {@code getTexture} 探测会让 TextureManager 去打一次 "Failed to load texture" 警告，
     * 所以这里先查这张表直接返回。
     */
    private static final Set<ResourceLocation> LOADED = new HashSet<>();

    private CosmicTextures() {
    }

    /** 客户端启动时调用一次。 */
    public static void register() {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation mask : MASKS) {
            reload(textureManager, mask);
        }
        reload(textureManager, STARS);
        registerWhiteMask(textureManager);
    }

    /** 合成并注册那张 16x16 全白遮罩（给闪电用）。 */
    private static void registerWhiteMask(TextureManager textureManager) {
        NativeImage image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setPixelRGBA(x, y, 0xFFFFFFFF);
            }
        }
        DynamicTexture texture = new DynamicTexture(image);
        textureManager.register(WHITE_MASK, texture);
        texture.upload();
        texture.setFilter(false, false);
        LOADED.add(WHITE_MASK);
    }

    /** 纯白遮罩贴图（闪电用）。 */
    public static AbstractTexture whiteMask() {
        return Minecraft.getInstance().getTextureManager().getTexture(WHITE_MASK);
    }

    /**
     * 这个物品该用哪张遮罩；不带星空层的物品返回 {@code null}。
     * <b>以后给新物品加星空，只需这里补一行 + 准备一张对应的 mask 图。</b>
     */
    public static ResourceLocation maskLocation(ItemStack stack) {
        if (stack.is(GildedMod.GILDED_INGOT.get())) return MASK;
        if (stack.is(GildedMod.BLACKGOLD_SWORD.get())) return SWORD_MASK;
        if (stack.is(GildedMod.FUND_SWORD.get())) return FUND_SWORD_MASK;
        if (stack.is(GildedMod.UNIOXING.get())) return UNIOXING_MASK;
        if (stack.is(GildedMod.FINAL_UNIVERSE_GILDED_TEMPLATE.get())) return FINAL_UNIVERSE_TEMPLATE_MASK;
        if (stack.is(GildedMod.BLACKGOLD_HELMET.get())) return HELMET_MASK;
        if (stack.is(GildedMod.BLACKGOLD_CHESTPLATE.get())) return CHESTPLATE_MASK;
        if (stack.is(GildedMod.BLACKGOLD_LEGGINGS.get())) return LEGGINGS_MASK;
        if (stack.is(GildedMod.BLACKGOLD_BOOTS.get())) return BOOTS_MASK;
        return null;
    }

    /** 物品对应的遮罩贴图；没有就返回 {@code null}，调用方直接跳过该物品。 */
    public static AbstractTexture maskFor(ItemStack stack) {
        ResourceLocation location = maskLocation(stack);
        return location == null ? null : lazy(location);
    }

    /** 合成遮罩的虚拟位置 → 它的源图（alpha 遮罩专用）。 */
    private static final Map<ResourceLocation, ResourceLocation> ALPHA_SOURCES = new HashMap<>();
    /** 源图 → 合成遮罩的虚拟位置（同一个 layer 图只合成一次）。 */
    private static final Map<ResourceLocation, ResourceLocation> ARMOR_MASK_KEYS = new HashMap<>();

    /**
     * 穿在身上的盔甲用：拿 layer 图<b>自己的 alpha 通道</b>当遮罩。
     * <p>不能用 R 通道 —— 黑金盔甲的暗色部分 R 也很低（实测 layer_1：R&gt;127 只有 371 个像素，
     * 但 A&gt;127 有 632 个），直接用 R 会把盔甲主体判成"没有遮罩"而漏掉星空。
     * <p>遮罩会按源图 UV 布局原样合成（64x32 对 64x32），所以 {@code texCoord0} 天然对齐。
     */
    public static AbstractTexture armorMask(ResourceLocation layerTexture) {
        ResourceLocation key = ARMOR_MASK_KEYS.computeIfAbsent(layerTexture, loc ->
                ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "cosmic_mask/" + loc.getPath()));
        ALPHA_SOURCES.put(key, layerTexture);

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        // 合成的 key 在资源包里并不存在，不能走 lazy() 的 getTexture 探测 ✗
        // —— 那会让 TextureManager 去打一条 "Failed to load texture: gildedmod:cosmic_mask/..."
        // 的 FileNotFoundException 警告（功能无害，但日志很难看）。这里直接合成上传。
        if (!LOADED.contains(key) && TRIED.add(key)) {
            reload(textureManager, key);   // 成功时 reload 内部会把它从 TRIED 移入 LOADED
        }
        return textureManager.getTexture(key);
    }

    /** 从源图合成 alpha 遮罩：R=G=B=alpha、A=255（着色器只读 R 通道）。 */
    private static NativeImage composeAlphaMask(ResourceLocation source) {
        NativeImage origin = readImage(source);
        if (origin == null) {
            return null;
        }
        try {
            int width = origin.getWidth();
            int height = origin.getHeight();
            NativeImage mask = new NativeImage(width, height, true);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = (origin.getPixelRGBA(x, y) >>> 24) & 0xFF;
                    mask.setPixelRGBA(x, y, alpha | (alpha << 8) | (alpha << 16) | 0xFF000000);
                }
            }
            return mask;
        } catch (RuntimeException e) {
            LOGGER.warn("[GildedMod] 由 {} 合成 alpha 遮罩失败", source, e);
            return null;
        } finally {
            origin.close();
        }
    }

    public static AbstractTexture stars() {
        return lazy(STARS);
    }

    private static AbstractTexture lazy(ResourceLocation location) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        // 已经注册过的（尤其是合成出来的虚拟 key）直接拿，别再让 TextureManager 去资源包里找
        if (LOADED.contains(location)) {
            return textureManager.getTexture(location);
        }
        AbstractTexture texture = textureManager.getTexture(location);
        if (texture instanceof DynamicTexture) {
            return texture;
        }
        if (!TRIED.add(location)) {
            return texture;
        }
        reload(textureManager, location);
        return textureManager.getTexture(location);
    }

    private static void reload(TextureManager textureManager, ResourceLocation location) {
        NativeImage image;
        ResourceLocation alphaSource = ALPHA_SOURCES.get(location);
        if (alphaSource != null) {
            // 盔甲：由 layer 图的 alpha 合成的遮罩
            image = composeAlphaMask(alphaSource);
        } else if (STARS.equals(location)) {
            // 星空条带由 textures/misc/cosmic/cosmic_0..9 在内存里拼成 16x160
            image = composeStars();
            if (image == null) {
                LOGGER.info("[GildedMod] 星空条带回退到 {}", location);
                image = readImage(location);
            } else {
                LOGGER.info("[GildedMod] 星空条带由 textures/misc/cosmic/cosmic_0..{} 拼接而成 ({}x{})",
                        TILE_COUNT - 1, image.getWidth(), image.getHeight());
            }
        } else {
            // 遮罩直接读对应物品的 mask 图（它的白色区域就是星空该出现的范围）
            image = readImage(location);
        }
        if (image == null) {
            return;
        }
        DynamicTexture texture = new DynamicTexture(image);
        textureManager.register(location, texture);
        // DynamicTexture 不是 TickableTexture，TextureManager 不会替它上传，必须自己来
        texture.upload();
        // 都用 NEAREST：遮罩用线性过滤时边界的半值会让星空多出约 1 像素（会“冲出”mask），
        // 星空用线性会被糊掉。NEAREST 下覆盖范围和 mask 的白色区域严格一致。
        texture.setFilter(false, false);
        logImageStats(label(location), image);
        TRIED.remove(location);
        LOADED.add(location);
    }

    private static String label(ResourceLocation location) {
        if (STARS.equals(location)) {
            return "星空条带";
        }
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        return "遮罩(" + (slash >= 0 ? path.substring(slash + 1) : path) + ")";
    }

    /**
     * 只打一次通道统计，用来看遮罩图的“形状”到底画在 RGB 上还是 alpha 上
     * （着色器目前只读 R 通道，所以这里必须确认 R 通道是对的）。
     */
    private static void logImageStats(String what, NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int count = width * height;
        if (count <= 0) {
            return;
        }
        long sumR = 0L;
        long sumG = 0L;
        long sumB = 0L;
        long sumA = 0L;
        int strongR = 0;
        int strongA = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getPixelRGBA(x, y);
                int r = pixel & 0xFF;
                int g = (pixel >>> 8) & 0xFF;
                int b = (pixel >>> 16) & 0xFF;
                int a = (pixel >>> 24) & 0xFF;
                sumR += r;
                sumG += g;
                sumB += b;
                sumA += a;
                if (r > 127) strongR++;
                if (a > 127) strongA++;
            }
        }
        LOGGER.info("[GildedMod] {} {}x{}: 均值 R/G/B/A={}/{}/{}/{}  R>50%的像素={}/{}  A>50%的像素={}/{}",
                what, width, height, sumR / count, sumG / count, sumB / count, sumA / count,
                strongR, count, strongA, count);
    }

    /**
     * 把 {@code textures/misc/cosmic/cosmic_0..9.png} 竖着拼成 16x160：
     * 第 i 张占 v ∈ [i/10, (i+1)/10]，和着色器里那张硬编码的 10 段 v 表完全对齐。
     * 逐像素原样拷贝（{@code getPixelRGBA}/{@code setPixelRGBA} 用的是同一种打包格式），
     * 所以颜色和 alpha 都不会走样。
     */
    private static NativeImage composeStars() {
        NativeImage strip = new NativeImage(TILE_SIZE, TILE_SIZE * TILE_COUNT, true);
        for (int i = 0; i < TILE_COUNT; i++) {
            NativeImage tile = readImage(tileLocation(i));
            if (tile == null) {
                strip.close();
                return null;
            }
            try {
                int tileWidth = tile.getWidth();
                int tileHeight = tile.getHeight();
                for (int y = 0; y < TILE_SIZE; y++) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        int sourceX = tileWidth > 0 ? x * tileWidth / TILE_SIZE : 0;
                        int sourceY = tileHeight > 0 ? y * tileHeight / TILE_SIZE : 0;
                        strip.setPixelRGBA(x, i * TILE_SIZE + y, tile.getPixelRGBA(sourceX, sourceY));
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.warn("[GildedMod] 拼接星空条带失败", e);
                strip.close();
                return null;
            } finally {
                tile.close();
            }
        }
        return strip;
    }

    private static ResourceLocation tileLocation(int index) {
        return ResourceLocation.fromNamespaceAndPath("gildedmod", TILE_PREFIX + index + ".png");
    }

    private static NativeImage readImage(ResourceLocation location) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            LOGGER.warn("[GildedMod] 找不到贴图资源 {}", location);
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            return NativeImage.read(stream);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("[GildedMod] 贴图读取失败 {}", location, e);
            return null;
        }
    }
}
