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

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackGoldMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在客户端且是玩家 tick 结束时处理
        if (event.phase != TickEvent.Phase.END) return;
        if (!event.player.level().isClientSide) return;

        Player player = event.player;
        if (BlackGoldMod.hasGodItem(player) || BlackGoldMod.hasFullBlackGoldArmor(player)) {
            // 立即清除受击动画和音效状态
            player.hurtTime = 0;
            player.hurtDuration = 0;
            // 如果还想让玩家完全不抖动，还可以重置 invulnerableTime（但 setInvulnerable 已处理）
        }
    }
}