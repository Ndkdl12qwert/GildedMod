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
package com.example.examplemod.mixin;

import com.example.examplemod.BlackGoldArmor;
import com.example.examplemod.BlackGoldSword;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class LiuClientAttackMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void liuStartAttack(CallbackInfoReturnable<Boolean> ci) {
        Minecraft mc = (Minecraft)(Object)this;
        System.out.println("[LIU-AGENT] startAttack called, crosshairPickEntity=" + mc.crosshairPickEntity);

        Player player = mc.player;
        if (player == null) return;
        System.out.println("[LIU-AGENT]   player=" + player.getName().getString() + ", mainHand=" + player.getMainHandItem().getItem());

        if (!(player.getMainHandItem().getItem() instanceof BlackGoldSword)) {
            System.out.println("[LIU-AGENT]   not holding BlackGoldSword");
            return;
        }
        if (!(mc.crosshairPickEntity instanceof Player targetPlayer)) {
            System.out.println("[LIU-AGENT]   crosshairPickEntity is not Player");
            return;
        }
        System.out.println("[LIU-AGENT]   targetPlayer=" + targetPlayer.getName().getString() + ", fullArmor=" + isFullBlackGoldArmor(targetPlayer));

        if (!isFullBlackGoldArmor(targetPlayer)) return;
        System.out.println("[LIU-AGENT]   canceling attack (target has full BlackGoldArmor)");
        ci.cancel();
    }

    private boolean isFullBlackGoldArmor(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (!(armor.getItem() instanceof BlackGoldArmor)) {
                return false;
            }
        }
        return true;
    }
}