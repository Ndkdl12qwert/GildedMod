package com.gildedmod.mixin;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.LiuCore;
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

        int step = LiuCore.nativeAbsoluteKill(living);
        System.out.println("[LIU-MIXIN] nativeAbsoluteKill last step: " + step);
        ci.cancel();
    }
}