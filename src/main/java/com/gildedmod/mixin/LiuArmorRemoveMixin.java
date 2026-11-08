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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鎏金甲玩家的"移除"守卫。
 *
 * <p>旧版有两个问题，这里都修了：
 * <ol>
 *     <li>只拦了 {@code remove}，<b>没拦 {@code setRemoved}</b> ✗ ——
 *         而 {@code remove(reason)} 内部就是调 {@code setRemoved(reason)}，
 *         任何 mod 直接调 {@code setRemoved} 就能把穿满甲的玩家抹掉。现在两个入口都拦。</li>
 *     <li>无脑全拦 ✗ —— 换维度 / 登出 / 区块卸载同样会走 {@code remove}，
 *         全拦会导致玩家自己卡在原地离不开世界。现在按<b>移除原因</b>放行
 *         （见 {@link LiuArmorCore#nativeGuardRemoveReason(String)}）。</li>
 * </ol>
 */
@Mixin(Entity.class)
public class LiuArmorRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void liuArmorRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (liuAllowRemoval(reason)) return;
        ci.cancel();
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void liuArmorSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (liuAllowRemoval(reason)) return;
        ci.cancel();
    }

    /** true = 放行这次移除 */
    @Unique
    private boolean liuAllowRemoval(Entity.RemovalReason reason) {
        if (LiuBypass.isBypass()) return true;
        if ("true".equals(System.getProperty("liu.bypass"))) return true;
        if (!((Object) this instanceof Player player)) return true;
        if (player.getInventory() == null) return true;
        if (!GildedMod.isProtected(player)) return true;

        // ★ 已经在死亡流程里的，必须放行。
        //
        //   为什么：守卫的本意是"活着的时候不能被抹掉"。
        //   可一旦玩家真的死了（例如被无视护甲的伤害打死），
        //   再拦 KILLED 就会让服务端**永远删不掉这个玩家实体** ✗ ——
        //   表现就是死亡界面卡住、重生打架、tick 被拖到 2~3 秒（实测过
        //   "Can't keep up! Running 3453ms"）。
        //
        //   判定用 isDeadOrDying()：dead 字段为 true 或血量 <= 0 都算。
        if (player.isDeadOrDying()) {
            return true;
        }

        String name = (reason == null) ? "" : reason.name();
        try {
            return LiuArmorCore.nativeGuardRemoveReason(name);
        } catch (Throwable t) {
            // native 不可用时宁可拦下（玩家还在，比被抹掉好）
            return false;
        }
    }
}
