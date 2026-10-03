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
package com.example.examplemod;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackGoldMod.MODID)
public class ModEffects {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;

        if (player.tickCount % 20 != 0) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (hasItem(mainHand, offHand, BlackGoldSword.class)) {
            applyGodEffects(player);
        }

        if (hasItem(mainHand, offHand, BlackGoldPickaxe.class)) {
            applyGodEffects(player);
        }

        if (hasItem(mainHand, offHand, BlackGoldAxe.class)) {
            applyGodEffects(player);
        }
    }

    private static boolean hasItem(ItemStack mainHand, ItemStack offHand, Class<? extends Item> itemClass) {
        return itemClass.isInstance(mainHand.getItem()) || itemClass.isInstance(offHand.getItem());
    }

    private static void applyGodEffects(Player player) {
        // 力量 V
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, 999999999, 255, false, true, true
        ));
        // 急迫
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, 999999999, 255, false, true, true
        ));
        // 抗性提升
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, 999999999, 255, false, true, true
        ));
        // 再生
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, 999999999, 255, false, true, true
        ));
        // 防火
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE, 999999999, 255, false, true, true
        ));
        // 水下呼吸
        player.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING, 999999999, 255, false, true, true
        ));
        // 夜视
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, 999999999, 255, false, true, true
        ));
        // 伤害吸收
        player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION, 999999999, 255, false, true, true
        ));
    }
}