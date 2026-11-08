package com.gildedmod.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.entity.ItemRenderer;

import java.util.function.Consumer;

public class GildedIngotItem extends Item {

    public GildedIngotItem() {
        super(new Item.Properties());
        System.out.println("[GILDED] GildedIngotItem constructed");
    }


}