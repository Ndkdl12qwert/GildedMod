package com.gildedmod.mixin;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.LiuCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = -214748302)
public class BlackGoldSwordMixin {

    /** 写 logs/gilded-abfla.log（和 agent 侧同一个文件，方便对照） */
    private static void abfLog(String msg) {
        try {
            java.io.File dir = new java.io.File("logs");
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
            try (java.io.FileWriter fw = new java.io.FileWriter("logs/gilded-abfla.log", true)) {
                fw.write(new java.util.Date().toString() + "  " + msg + System.lineSeparator());
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void liuAttack(Entity target, CallbackInfo ci) {
        Player self = (Player)(Object)this;

        if (!(self.getMainHandItem().getItem() instanceof BlackGoldSword)) return;
        if (!(target instanceof LivingEntity living)) return;

        // ★ 只有【拿着黑金/神剑】攻击时才跑完整击杀链（写0 → dead → setRemoved → NBT 粉碎）
        //   ⚠ 空手/别的武器一律不触发 —— 否则生存模式空手打一下就秒杀 ✗
        if (!self.level().isClientSide) {
            try {
                float v = LiuCore.absoErase(living);
                // ★ 保证型击杀：四条手段全跑
                int ann = LiuCore.nativeAnnihilate(living);
                abfLog("[ABFLA] annihilate steps=" + ann);
                abfLog("[ABFLA] 剑击 " + living.getClass().getName()
                        + " id=" + living.getId() + " → " + v);
            } catch (Throwable t) {
                abfLog("[ABFLA] 调用失败 → " + t);
            }
        }

        if (self.level().isClientSide) {
            self.swing(InteractionHand.MAIN_HAND);
            ci.cancel();
            return;
        }

        // 命中特效：在目标身上落一道鎏金闪电（每次挥击一道，成本很低；
        // 生成在击杀之前，这样闪电的位置就是目标当时所在的位置）
        BlackGoldSword.spawnGildedLightning(self.level(), self, living);

        int step = LiuCore.nativeAbsoluteKill(living);
        System.out.println("[LIU-MIXIN] nativeAbsoluteKill last step: " + step);

        // ★ 贴标签：接下来 40 tick（2 秒）内拒绝一切回血。
        //   必须放在 nativeAbsoluteKill 之后 —— die() 内部要写 setHealth(0)，
        //   那时候标记还没生效，不会被自己的钩子拦掉。
        LiuCore.nativeDoomMark(living, 40);

        // ★★ 打不死就自动补刀（这才是"ultkill 长在剑上"）
        //
        //   为什么不在这一行直接跑 ultimateKill：
        //     这里还在 Player.attack 的栈里，在里面调 die()/remove() 会破坏实体列表
        //     （本项目为此崩过：EXCEPTION_ACCESS_VIOLATION at jvm.dll+0x2d2268）。
        //   所以：这里只【报名】，真正的抹除由 ServerTickEvent.END 里的
        //         nativeDoomTick() 在攻击栈之外执行。
        //
        //   触发条件：常规 6 路径打完之后目标还活着（isRemoved() == false）。
        //   对普通怪这一步几乎不会命中（第一条路径就死了）；
        //   只有"override 型免疫"的目标（例如 pig2mod 的 Pig2）才会走到这里。
        // ★ 不再用 isRemoved() 判断 —— 那正是它会撒谎的地方：
        //     Pig2.isRemoved() { if (mIsAddingNewPig2) return false; return removalReason != null; }
        //   我们的 erase 写过 removalReason → 它返回 true → 老的 if 永远不成立 ✗
        //   现在：打中任何非玩家生物都排队，队列自己去重、自己限制上限。
        if (!(living instanceof Player)) {
            // ★ 天罚：JNI 层的致命一击（无实体 / 无事件 / 全非虚）
            //   —— 专门对付"拦别的 mod 攻击"的实现
            int smiteSteps = LiuCore.nativeSmite(living);
            System.out.println("[LIU-MIXIN] nativeSmite steps: " + smiteSteps);
            LiuCore.nativeRequestUltimate(living);
        }

        // ---- 玩家兜底：native 里 die() 被别的 mod 拦掉时，强制走重生流程 ----
        //
        //  为什么需要这层：
        //    native 对玩家只调 die()（不碰 removalReason/setRemoved，否则重生后双方互不可见）。
        //    但"其他神器模组"的盔甲往往在 hurt / die 上做拦截，die() 就失效了。
        //
        //  为什么用 PlayerList.respawn：
        //    它内部用 Entity.RemovalReason.CHANGED_DIMENSION 移除旧实体 ——
        //    这个原因在 LiuArmorCore 的白名单里，不会被我们自己的守卫拦，
        //    而且重生流程（新建 ServerPlayer + 重发 PlayerInfo）是完整的。
        //
        //  代价（必须知道）：
        //    respawn 不会掉物品、不会弹死亡界面 —— 行为上更像"强制回城"而不是"被杀"。
        //    这是 MC 架构的硬边界：死亡界面只能由 ServerPlayer.die() 发出。
        if (living instanceof ServerPlayer victim && !victim.isDeadOrDying()) {
            if (victim.getHealth() > 0.0F) {
                victim.setHealth(0.0F);
            }
            try {
                victim.server.getPlayerList().respawn(victim, false);
                System.out.println("[LIU-MIXIN] 玩家 die() 被拦 -> 已强制重生兜底");
            } catch (Throwable t) {
                System.out.println("[LIU-MIXIN] 强制重生兜底失败: " + t);
            }
        }

        ci.cancel();
    }

    /**
     * 防缴械 / 防丢失：任何"把剑丢掉"的路径最终都会走 {@link Player#drop}。
     * <p><b>踩过的坑</b>：只 {@code setReturnValue(null)} 会让剑<b>凭空消失</b> ✗ ——
     * 因为 Q 键的流程是"先从背包移除，再调用 drop 生成掉落物"，
     * 只拦 drop 等于"移除成功、掉落物没生成"。
     * <p>现在的做法：<b>把剑收回背包</b>，再拦掉这次掉落 →
     * 剑不会掉出去，也不会消失 ✓；只有背包真的满了才放行走原版（至少它还在世界里 ✓）。
     */
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void liuNoDisarm(ItemStack stack, boolean dropAround, boolean traceItem,
                             CallbackInfoReturnable<ItemEntity> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlackGoldSword)) return;

        Player self = (Player)(Object)this;

        // ★★ 玩家正在死亡时绝不能拦 ★★
        //   die() → dropAll() 会【遍历背包】逐个调用 drop()；
        //   下面的 getInventory().add(...) 会把物品【加回】正在被遍历的背包，
        //   导致"边遍历边添加" → 迭代失控 → 堆/栈爆掉 → jvm.dll 里 0xC0000005 崩溃。
        //   （2026-10-24 实测：强力击杀打到玩家时必崩，就是这个原因）
        //
        //   而且死亡掉落本该掉光，死了还保剑本身就是设计错误。
        if (self.isDeadOrDying()) {
            return;   // 放行走原版 → 正常掉落
        }

        if (self.level().isClientSide) {
            // 客户端不要预测出掉落物
            cir.setReturnValue(null);
            return;
        }

        // 服务端：先收进背包（这样才不会有"消失"的问题）
        if (self.getInventory().add(stack.copy())) {
            cir.setReturnValue(null);
        }
        // 背包满了 → 放行走原版掉落，剑至少还在世界上，而不是消失
    }
}
