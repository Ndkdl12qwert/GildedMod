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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = -214748300)
public class LiuArmorAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void liuArmorAttack(Entity target, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        self.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

        if (self.level().isClientSide) return;
        if (!(target instanceof Player targetPlayer)) return;
        if (!myMode$isFullBlackGoldArmor(targetPlayer)) return;

        if (self.getMainHandItem().getItem() instanceof BlackGoldSword) return;

        ci.cancel();
    }

    @Unique
    private boolean myMode$isFullBlackGoldArmor(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (!(armor.getItem() instanceof BlackGoldArmor)) {
                return false;
            }
        }
        return true;
    }
}