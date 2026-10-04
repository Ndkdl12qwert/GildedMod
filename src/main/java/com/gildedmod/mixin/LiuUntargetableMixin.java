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
package com.gildedmod.mixin;

import com.gildedmod.BlackGoldArmor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class LiuUntargetableMixin {

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void liuUntargetable(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!isFullBlackGoldArmor(player)) return;
        cir.setReturnValue(false);
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