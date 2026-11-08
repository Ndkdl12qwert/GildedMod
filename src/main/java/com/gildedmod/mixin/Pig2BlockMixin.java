package com.gildedmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦掉指定 mod 的实体进入世界。
 *
 * <h3>为什么需要它</h3>
 * 有些对抗型 mod 的实体在 {@code getHealth()} / {@code isRemoved()} 上撒谎，
 * 于是"杀掉"只是把它变成一具会走路的尸体 —— 它下一 tick 就调用自己的
 * {@code addFreshEntity(自己)} 复活 ✗。
 *
 * <p>实测证据（{@code pig2mod} 的 {@code Pig2}）：
 * <pre>
 *   [ERASE] 抹除后: step=6 isRemoved=1 health=10.0 dead=1
 *   // 6 条路径全跑完了，但下一 tick 它又出现在世界里
 * </pre>
 *
 * <h3>拦在哪一层</h3>
 * {@code ServerLevel.addFreshEntity(Entity)} —— 所有"实体进入世界"的**唯一**收口：
 * 刷怪、刷怪笼、命令、别的 mod 生成、以及它自己复活，最终都走这里 ✓
 *
 * <p>返回 {@code false}（而不是抛异常）：调用方会以为"没加进去"，
 * 于是它自己的复活逻辑正常收尾，不会因为异常而反复重试 ✗。
 *
 * <h3>范围</h3>
 * 只拦包名以 {@code kakiku.pig2mod} 开头的实体，其它一切照常。
 */
@Mixin(ServerLevel.class)
public class Pig2BlockMixin {

    /** 要拦的包前缀（改这里就能拦别的 mod） */
    private static final String LIU_BLOCK_PACKAGE = "kakiku.pig2mod";

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void liuBlockForeignEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) {
            return;
        }
        // 用 getClass().getName() 而不是 instanceof：对方可能有子类/代理类，
        // 包名判断能把整族都覆盖掉 ✓
        if (entity.getClass().getName().startsWith(LIU_BLOCK_PACKAGE)) {
            System.out.println("[LIU-BLOCK] 拦截进入世界: " + entity.getClass().getSimpleName()
                    + " id=" + entity.getId());
            cir.setReturnValue(Boolean.FALSE);
            cir.cancel();
        }
    }
}
