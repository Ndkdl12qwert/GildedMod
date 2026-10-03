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

import com.example.examplemod.BlackGoldSword;
import com.example.examplemod.LiuCore;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Player.class, priority = -214748302)
public class BlackGoldSwordMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void liuAttack(Entity target, CallbackInfo ci) {
        Player self = (Player)(Object)this;

        if (!(self.getMainHandItem().getItem() instanceof BlackGoldSword)) return;
        if (!(target instanceof LivingEntity living)) return;

        if (self.level().isClientSide) {
            self.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
            return;
        }

        if (LiuCore.shouldStrike(living)) {
            DamageSource source = self.damageSources().playerAttack(self);
            LiuCore.strike(living, source);
            ci.cancel();
        }
    }
}