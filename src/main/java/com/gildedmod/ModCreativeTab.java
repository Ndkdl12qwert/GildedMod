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

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GildedMod.MODID);

    public static final RegistryObject<CreativeModeTab> BLACKGOLD_TAB = CREATIVE_MODE_TABS.register(
            "blackgold_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(GildedMod.BLACKGOLD_SWORD.get()))
                    .title(Component.translatable("creativetab.gildedmod"))
                    .displayItems((parameters, output) -> {
                        // 武器
                        output.accept(GildedMod.BLACKGOLD_SWORD.get());
                        output.accept(GildedMod.BLACKGOLD_PICKAXE.get());
                        output.accept(GildedMod.BLACKGOLD_AXE.get());
                        // 盔甲
                        output.accept(GildedMod.BLACKGOLD_HELMET.get());
                        output.accept(GildedMod.BLACKGOLD_CHESTPLATE.get());
                        output.accept(GildedMod.BLACKGOLD_LEGGINGS.get());
                        output.accept(GildedMod.BLACKGOLD_BOOTS.get());
                        // 材料
                        output.accept(GildedMod.BLACKGOLD_NUGGET.get());
                        output.accept(GildedMod.BLACKGOLD_INGOT.get());
                        output.accept(GildedMod.GOD_BLACKGOLD_UPGRADE_TEMPLATE.get());
                        // 压缩下界合金块
                        output.accept(GildedMod.COMPRESSED_NETHERITE_1_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_2_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_3_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_4_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_5_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_6_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_7_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_8_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_9_ITEM.get());
                        output.accept(GildedMod.COMPRESSED_NETHERITE_FINAL_ITEM.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}