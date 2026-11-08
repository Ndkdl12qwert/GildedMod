/*
 * GildedMod
 * Copyright (C) 2026 Onicox
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.gildedmod;

import com.gildedmod.agent.LiuAgentLoader;
import com.gildedmod.agent.bus.LiuAgentBus;
import com.gildedmod.client.GildedIngotItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;

@Mod(GildedMod.MODID)
public class GildedMod {
    public static final String MODID = "gildedmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> BLACKGOLD_SWORD = ITEMS.register(
            "blackgold_sword",
            BlackGoldSword::new
    );

    public static final RegistryObject<Item> FUND_SWORD = ITEMS.register(
            "fund_sword",
            FundSword::new
    );

    public static final RegistryObject<Item> BLACKGOLD_PICKAXE = ITEMS.register(
            "blackgold_pickaxe",
            BlackGoldPickaxe::new
    );

    public static final RegistryObject<Item> BLACKGOLD_AXE = ITEMS.register(
            "blackgold_axe",
            BlackGoldAxe::new
    );

    public static final RegistryObject<Item> BLACKGOLD_HELMET = ITEMS.register(
            "blackgold_helmet",
            () -> new BlackGoldArmor(ArmorItem.Type.HELMET)
    );

    public static final RegistryObject<Item> BLACKGOLD_CHESTPLATE = ITEMS.register(
            "blackgold_chestplate",
            () -> new BlackGoldArmor(ArmorItem.Type.CHESTPLATE)
    );

    public static final RegistryObject<Item> BLACKGOLD_LEGGINGS = ITEMS.register(
            "blackgold_leggings",
            () -> new BlackGoldArmor(ArmorItem.Type.LEGGINGS)
    );

    public static final RegistryObject<Item> BLACKGOLD_BOOTS = ITEMS.register(
            "blackgold_boots",
            () -> new BlackGoldArmor(ArmorItem.Type.BOOTS)
    );

    public static final RegistryObject<Item> BLACKGOLD_NUGGET = ITEMS.register(
            "blackgold_nugget",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> BLACKGOLD_INGOT = ITEMS.register(
            "blackgold_ingot",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> GILDED_INGOT = ITEMS.register(
            "gilded_ingot",
            GildedIngotItem::new
    );

    public static final RegistryObject<Item> GOD_BLACKGOLD_UPGRADE_TEMPLATE = ITEMS.register(
            "god_blackgold_upgrade_template",
            () -> new Item(new Item.Properties())
    );

    public static final RegistryObject<Item> FINAL_UNIVERSE_GILDED_TEMPLATE = ITEMS.register(
            "final_universe_gilded_template",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> UNIOXING = ITEMS.register(
            "unioxing",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.EPIC)
                    .fireResistant())
    );

    public static final RegistryObject<Item> STAR = ITEMS.register(
            "star",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.EPIC)
                    .fireResistant())
    );

    public static final RegistryObject<Item> GALAXY = ITEMS.register(
            "galaxy",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.EPIC)
                    .fireResistant())
    );

    public static final RegistryObject<Item> BLACK_HOLE = ITEMS.register(
            "black_hole",
            () -> new Item(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.EPIC)
                    .fireResistant())
    );

    public static final RegistryObject<Item> HUBBLE_VOLUME = ITEMS.register(
            "hubble_volume",
            HubbleVolumeItem::new
    );

    // ===== 压缩下界合金块 1~9 层 + final =====
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_1 = BLOCKS.register(
            "compressed_netherite_1",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_1_ITEM = ITEMS.register(
            "compressed_netherite_1",
            () -> new BlockItem(COMPRESSED_NETHERITE_1.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_2 = BLOCKS.register(
            "compressed_netherite_2",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(60.0F, 1440.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_2_ITEM = ITEMS.register(
            "compressed_netherite_2",
            () -> new BlockItem(COMPRESSED_NETHERITE_2.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_3 = BLOCKS.register(
            "compressed_netherite_3",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(75.0F, 1800.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_3_ITEM = ITEMS.register(
            "compressed_netherite_3",
            () -> new BlockItem(COMPRESSED_NETHERITE_3.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_4 = BLOCKS.register(
            "compressed_netherite_4",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(100.0F, 2400.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_4_ITEM = ITEMS.register(
            "compressed_netherite_4",
            () -> new BlockItem(COMPRESSED_NETHERITE_4.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_5 = BLOCKS.register(
            "compressed_netherite_5",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(150.0F, 3600.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_5_ITEM = ITEMS.register(
            "compressed_netherite_5",
            () -> new BlockItem(COMPRESSED_NETHERITE_5.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_6 = BLOCKS.register(
            "compressed_netherite_6",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(200.0F, 4800.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_6_ITEM = ITEMS.register(
            "compressed_netherite_6",
            () -> new BlockItem(COMPRESSED_NETHERITE_6.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_7 = BLOCKS.register(
            "compressed_netherite_7",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(300.0F, 7200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_7_ITEM = ITEMS.register(
            "compressed_netherite_7",
            () -> new BlockItem(COMPRESSED_NETHERITE_7.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_8 = BLOCKS.register(
            "compressed_netherite_8",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(500.0F, 12000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_8_ITEM = ITEMS.register(
            "compressed_netherite_8",
            () -> new BlockItem(COMPRESSED_NETHERITE_8.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_9 = BLOCKS.register(
            "compressed_netherite_9",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(800.0F, 19200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_9_ITEM = ITEMS.register(
            "compressed_netherite_9",
            () -> new BlockItem(COMPRESSED_NETHERITE_9.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> COMPRESSED_NETHERITE_FINAL = BLOCKS.register(
            "compressed_netherite_final",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(1500.0F, 36000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK))
    );
    public static final RegistryObject<Item> COMPRESSED_NETHERITE_FINAL_ITEM = ITEMS.register(
            "compressed_netherite_final",
            () -> new BlockItem(COMPRESSED_NETHERITE_FINAL.get(), new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Block> BLACKGOLD_BLOCK = BLOCKS.register(
            "blackgold_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(121500.0F, 2916000.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL))
    );

    public static final RegistryObject<Item> BLACKGOLD_BLOCK_ITEM = ITEMS.register(
            "blackgold_block",
            () -> new BlockItem(BLACKGOLD_BLOCK.get(), new Item.Properties())
    );

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);

    public static final RegistryObject<RecipeType<BlackGoldBlastingRecipe>> BLACKGOLD_BLASTING =
            RECIPE_TYPES.register("blackgold_blasting", () -> new RecipeType<BlackGoldBlastingRecipe>() {});

    // 配方序列化器注册
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<RecipeSerializer<BlackGoldBlastingRecipe>> BLACKGOLD_BLASTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("blackgold_blasting",
                    BlackGoldBlastingRecipeSerializer::new);

    /**
     * 用<b>构造器注入</b>拿事件总线：Forge 47.x 起 {@code FMLJavaModLoadingContext.get()} 已弃用
     * （标记 since 1.21.1 / forRemoval），改成让 Forge 把 context 直接传进构造器即可。
     * <p>注入是 Forge 内部机制（{@code FMLModContainer.constructMod} 会先找
     * {@code 构造器(FMLJavaModLoadingContext)}，找不到才退回无参构造），所以这个签名必须精确写
     * {@code FMLJavaModLoadingContext} 这一个参数。
     */
    public GildedMod(FMLJavaModLoadingContext context) {
        System.out.println("GildedEX-Mod | Author: Onicox | ...");

        // 注意：JVMTI 的 dll 现在【内嵌在 gilded_hook.dll 里】了 ——
        // 由启动器在 JVM 启动前解压到 %LOCALAPPDATA%\GildedEX\native\，
        // 再把 -agentpath 指过去。所以 mods\ 里不需要放这个 dll。
        // （曾短暂地在 Java 侧做过"把 jar 里的 dll 补到 mods\"，已废弃：
        //   那样只能下次启动生效，而且又回到"要往 mods 里塞文件"的老路。）
        //
        // 文件名：GildedNativeKernel.dll（CMake target 同名，2026-10-31 从 liu_jvmti.dll 改名）

        String jvmtiPath = System.getProperty("liu.jvmti.path");
        if (jvmtiPath == null || jvmtiPath.isBlank()) {
            jvmtiPath = "mods/GildedNativeKernel.dll";
        }
        LiuAgentBus.loadJvmtiDll(jvmtiPath);

        try {
            LiuCore.nativeInitJVMTI();
            System.out.println("[LIU-CORE] JVMTI kill ready");
        } catch (Throwable t) {
            System.err.println("[LIU-CORE] nativeInitJVMTI failed: " + t);
        }

        System.out.println("[LIU-CHECK] GildedMod constructed, attaching agent...");
        LiuAgentLoader.attach();

        LOGGER.info("§6§l╔════════════════════════════════╗");
        LOGGER.info("§6§l║     G I L D E D E X         ║");
        LOGGER.info("§6§l║      神 · 权 · 降 · 临       ║");
        LOGGER.info("§6§l╚════════════════════════════════╝");
        LOGGER.info("");

        IEventBus modEventBus = context.getModEventBus();
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        // 网络通道：把客户端的 Alt 按住状态同步给服务端（护剑逻辑要用）
        com.gildedmod.net.AltSync.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static boolean hasFullBlackGoldArmor(Player player) {
        int pieces = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof BlackGoldArmor) {
                pieces++;
            }
        }
        return pieces >= 4;
    }

    /**
     * 主手或副手拿着任意鎏金/神剑。
     *
     * <p>用于「纯代码级伤害拦截」：拿着剑就免伤，不再靠常驻药水效果
     * （那套已经因为卡顿删掉了 —— 每次 addEffect 都要属性重算 + 同步包）。
     */
    public static boolean holdsBlackGoldSword(Player player) {
        if (player == null) {
            return false;
        }
        return player.getMainHandItem().getItem() instanceof BlackGoldSword
                || player.getOffhandItem().getItem() instanceof BlackGoldSword;
    }

    /**
     * 统一的"受保护"判定 —— 全套鎏金盔甲，或手里拿着剑。
     *
     * <p>所有伤害/移除守卫都应该用这个，而不是直接问 {@code hasFullBlackGoldArmor}，
     * 否则会出现"穿甲免伤、拿剑不免伤"的不一致 ✓
     */
    public static boolean isProtected(Player player) {
        return hasFullBlackGoldArmor(player) || holdsBlackGoldSword(player);
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (hasGodItem(player) || hasFullBlackGoldArmor(player)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && hasGodItem(player)) {
            event.setCanceled(true);
            player.fallDistance = 0.0f;
        }
    }


    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().player == null) return;

        Player player = Minecraft.getInstance().player;
        if (hasFullBlackGoldArmor(player)) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.clearFire();
            // 不在这里动效果：效果归属服务端（见 onPlayerTick / LiuArmorTicker）
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        long t0 = GildedDebug.begin();

        if (hasFullBlackGoldArmor(player)) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            // 直接 Java 调用，不走 native
            player.clearFire();
            // 只清有害效果 —— removeAllEffects() 会连增益一起删掉
            LiuArmorCore.clearDebuffs(player);
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        GildedDebug.end("playerTickEvent", t0);
    }

    public static boolean hasGodItem(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() instanceof BlackGoldSword ||
                off.getItem() instanceof BlackGoldSword ||
                main.getItem() instanceof BlackGoldPickaxe ||
                off.getItem() instanceof BlackGoldPickaxe ||
                main.getItem() instanceof BlackGoldAxe ||
                off.getItem() instanceof BlackGoldAxe;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("§6§l╔════════════════════════════════╗");
        LOGGER.info("§6§l║     G O D S W O R D     ║");
        LOGGER.info("§6§l║      神 · 权 · 覆 · 盖     ║");
        LOGGER.info("§6§l╚════════════════════════════════╝");
        LOGGER.info("§6§lGODSWORD §r§7- 手持此剑，即为神明");
    }
}