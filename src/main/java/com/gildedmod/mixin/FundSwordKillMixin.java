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

import com.gildedmod.FundSword;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 基金之剑（{@link FundSword}）的击杀 —— <b>故意用最弱的一层</b>。
 *
 * <h3>为什么是"最弱"</h3>
 * <ul>
 *     <li>只做 {@code setHealth(0)} + {@code die(...)} —— 走的是<b>普通死亡流程</b>；</li>
 *     <li>任何在 {@code setHealth}/{@code die}/{@code hurt} 上做拦截的 mod 都能挡下它
 *         （这正是"纯数值剑"该有的地位）；</li>
 *     <li>对比：{@code BlackGoldSword} 走 {@code nativeAbsoluteKill}（6 条路径 + 挂起线程），
 *         那个才是"绝对"。</li>
 * </ul>
 *
 * <h3>为什么放在 Player.attack 的 HEAD</h3>
 * <ul>
 *     <li>在伤害结算<b>之前</b>就把目标做掉 → 不会因为目标已死而漏掉击杀统计/掉落；</li>
 *     <li>{@code FundSword} 的攻击力是 {@code Double.MAX_VALUE}，但**伤害管线会被别的
 *         mod 的 {@code hurt} 守卫砍掉** → 所以这里单独补一次"确定死亡"；</li>
 *     <li>不 cancel 原方法：让正常攻击流程继续走（击退、耐久、音效都还能生效）。</li>
 * </ul>
 *
 * <p>注意：主手必须是 {@link FundSword}；副手黑金剑、其它武器都不受影响。
 */
@Mixin(Player.class)
public class FundSwordKillMixin {

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void gildedmod$fundSwordKill(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;

        // 只在服务端做（客户端只负责动画）
        if (self.level().isClientSide()) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        // 主手必须是基金之剑
        if (!(self.getMainHandItem().getItem() instanceof FundSword)) {
            return;
        }
        // 不打自己（防止某些反射/自伤路径）
        if (living == self) {
            return;
        }

        try {
            // 最弱的一层：普通击杀。
            // setHealth(0) 让他进 isDeadOrDying 分支，die() 完成死亡流程（掉落/统计/音效）。
            living.setHealth(0.0F);
            living.die(self.damageSources().playerAttack(self));
        } catch (Throwable ignored) {
            // 纯数值剑不该因为某个实体的特殊实现而把服务端 tick 搞崩
        }
    }
}
