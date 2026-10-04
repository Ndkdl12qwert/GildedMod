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

import com.gildedmod.GildedMod;
import com.gildedmod.LiuArmorCore;
import com.gildedmod.LiuBypass;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = -21780031)
public class LiuArmorMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void liuArmorHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (!((Object)this instanceof Player player)) return;
        if (!GildedMod.hasFullBlackGoldArmor(player)) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void liuArmorSetHealth(float health, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.hasFullBlackGoldArmor(player)) return;

        float currentHealth = player.getHealth();
        if (!LiuArmorCore.nativeGuardSetHealth(currentHealth, health)) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void liuArmorDie(DamageSource source, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.hasFullBlackGoldArmor(player)) return;
        if (!LiuArmorCore.nativeGuardDie()) {
            ci.cancel();
        }
    }

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void liuArmorSound(DamageSource source, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.hasFullBlackGoldArmor(player)) return;
        ci.cancel();
    }
}