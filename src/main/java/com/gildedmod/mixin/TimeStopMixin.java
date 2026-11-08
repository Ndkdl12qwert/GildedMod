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

import com.gildedmod.TimeStop;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时停：冻结所有非玩家的生物。
 *
 * <h3>为什么钩 {@code LivingEntity.tick} 而不是 {@code ServerLevel.tickNonPassenger}</h3>
 * 我一开始选的是 {@code ServerLevel.tickNonPassenger}（更彻底：所有实体都停），
 * 但它在映射表里<b>没有 SRG 名</b> ✗ —— 编译直接失败：
 * <pre>
 *   Unable to locate obfuscation mapping for @Inject target m_8647_
 *   Cannot find target method "m_8647_(...Entity;)V" in ServerLevel
 * </pre>
 * 而 {@code LivingEntity.tick} 在 tsrg 里明确映射为 {@code m_8119_} ✓，
 * 写官方名 {@code tick} 就能被注解处理器解析 ✓。
 *
 * <h3>这一刀能停住什么</h3>
 * <ul>
 *     <li>血量恢复 / 每秒回血 ✓（实测 Pig2 的自愈就在 tick 里）</li>
 *     <li>AI 移动、寻路、攻击 ✓（{@code aiStep} 由 {@code tick} 调用，一起被跳过）</li>
 *     <li>药水效果计时、着火、摔落 ✓</li>
 *     <li>生物 tick 全部跳过 → <b>服务端 tick 时间大幅下降</b> ✓</li>
 * </ul>
 *
 * <h3>停不住什么</h3>
 * <ul>
 *     <li>玩家 ✓（这是有意的 —— 否则开了时停自己也动不了）</li>
 *     <li>{@code ItemEntity} / {@code TNT} / 经验球 ✗（不是 LivingEntity，走别的 tick 路径）</li>
 *     <li>方块实体（熔炉/漏斗）✗（走 {@code Level.tickBlockEntities}）</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class TimeStopMixin {

    /** 诊断日志计数器（前 5 次才打印） */
    private static int liuTimeLogCount = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void liuTimeStopTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // ★★★ 无条件诊断（前 8 次）：用来区分两种完全不同的失败
        //   有这行 → 注入成功，问题在 shouldFreeze 返回值
        //   没有这行 → 注入根本没触发（挂载点/优先级问题）
        if (liuTimeLogCount < 8) {
            liuTimeLogCount++;
            boolean on;
            String err = "";
            try {
                on = com.gildedmod.LiuCore.nativeTimeFreezeGet();
            } catch (Throwable t) {
                on = false;
                err = " err=" + t.getClass().getSimpleName();
            }
            System.out.println("[TIME] tick 注入命中: class="
                    + self.getClass().getSimpleName()
                    + " id=" + self.getId()
                    + " isPlayer=" + (self instanceof net.minecraft.world.entity.player.Player)
                    + " nativeFrozen=" + on + err
                    + "（第 " + liuTimeLogCount + " 次）");
        }

        if (TimeStop.shouldFreeze(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void liuTimeStopAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (TimeStop.shouldFreeze(self)) {
            ci.cancel();
        }
    }
}
