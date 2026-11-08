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
        // ★ 全局伤害否决（"有攻击就废"）：任何来源的 hurt 一律无效，
        //   除了"我们自己发出的"（玩家手持鎏金剑/神剑 或 攻击者是我们的实体）。
        //   开关：config/gildedmod/kernel.properties -> block.all.hurt
        // ★ 全局否决：只在【受保护的目标（穿甲/持剑的玩家）】上生效
        //
        //   之前写错了 ✗：判断的是"攻击是不是我们发出的"，
        //   结果空手打僵尸也被当成"不是我们的"→ 伤害被取消 ✗
        //
        //   正确语义：伤害的**接收者**是我方受保护玩家 → 取消；
        //             其余（僵尸挨打、怪打怪）→ 正常 ✓
        if (com.gildedmod.LiuKernelConfig.blockAllHurt()
                && ((Object) this) instanceof Player self && GildedMod.isProtected(self)) {
            cir.setReturnValue(false);
            return;
        }

        if (LiuBypass.isBypass()) return;
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.isProtected(player)) return;
        cir.setReturnValue(false);
    }

    /**
     * 第二道伤害守卫：{@code actuallyHurt}。
     * <p>{@code hurt} 只是"入口"，很多 mod 会直接调 {@code actuallyHurt} 绕过它 ——
     * 旧版没拦这里，是盔甲"漏伤害"的主要来源之一。
     */
    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void liuArmorActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        if (LiuBypass.isBypass()) return;
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.isProtected(player)) return;
        ci.cancel();
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void liuArmorSetHealth(float health, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.isProtected(player)) return;

        try {
            if (!LiuArmorCore.nativeGuardSetHealthEx(player.getHealth(), health, player.getMaxHealth())) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // native 库没加载（或导出缺失）时不能让 Error 冒到服务端 tick → 崩端
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void liuArmorDie(DamageSource source, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.isProtected(player)) return;
        try {
            if (!LiuArmorCore.nativeGuardDie()) {
                ci.cancel();
            }
        } catch (Throwable t) {
            ci.cancel();
        }
    }

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    private void liuArmorSound(DamageSource source, CallbackInfo ci) {
        if ("true".equals(System.getProperty("liu.bypass"))) return;
        if (LiuBypass.isBypass()) return;
        if (!((Object)this instanceof Player player)) return;
        if (player.getInventory() == null) return;
        if (!GildedMod.isProtected(player)) return;
        ci.cancel();
    }
}