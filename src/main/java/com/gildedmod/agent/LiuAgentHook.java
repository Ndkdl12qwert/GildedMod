package com.gildedmod.agent;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.LiuDeathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LiuAgentHook {

    /**
     * 独立文件日志：写到 {@code logs/gilded-abfla.log}。
     *
     * <p>为什么不用 log4j / System.out：
     * agent 类由 system classloader 加载，拿不到 MC 的 log4j 配置；
     * 而 System.out 在非 launch.bat 启动时会被丢掉（我们已踩过 ✗）。
     * 直接写文件 = 一定看得见 ✓
     */
    private static void AbfLog(String msg) {
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

    private static final Set<UUID> DOOMED = ConcurrentHashMap.newKeySet();

    public static void markDoomed(Object entity) {
        if (entity instanceof Entity e) {
            DOOMED.add(e.getUUID());
        }
    }

    public static boolean isDoomed(Object entity) {
        if (entity instanceof Entity e) {
            return DOOMED.contains(e.getUUID());
        }
        return false;
    }

    public static void clearDoomed(UUID uuid) {
        if (uuid != null) {
            DOOMED.remove(uuid);
        }
    }

    public static void onPlayerAttack(Object self, Object target) {
        if (!(self instanceof Player player)) return;
        if (!(target instanceof LivingEntity living)) return;

        // ★ 攻击生物时调用用户的 C++ 函数（nativeAbsoErase）
        //   位置刻意放在【武器判断之前】：不论拿的是什么，只要攻击生物就触发 ✓
        //   并且只在服务端执行（客户端那份实体数据不是权威的，读出来不准）
        // ★ 空手/别的武器不跑击杀链（否则生存模式空手也秒杀 ✗）
        boolean needsSwordForErase =
                !(player.getMainHandItem().getItem() instanceof BlackGoldSword)
                        && !(player.getOffhandItem().getItem() instanceof BlackGoldSword);

        if (!player.level().isClientSide && !needsSwordForErase) {
            AbfLog("[ABFLA] 进入 onPlayerAttack  target=" + living.getClass().getName()
                    + " id=" + living.getId());
            try {
                float v = com.gildedmod.LiuCore.absoErase(living);
                AbfLog("[ABFLA] 调用成功 → " + v);
                System.out.println("[ABFLA] 攻击 "
                        + living.getClass().getSimpleName()
                        + " id=" + living.getId()
                        + " → " + v);
            } catch (Throwable t) {
                // 不再吞：写着文件里，一定能看到
                AbfLog("[ABFLA] 调用失败 → " + t);
            }
        }

        if (!(player.getMainHandItem().getItem() instanceof BlackGoldSword)) return;

        // ★ 卡顿修复（2026-11-07）：以前【每一刀】都跑完整 6 路径 liuDie
        //   （4~6 次非虚调用 + 十几行日志）→ 生存模式砍怪明显卡 ✓
        //   现在只对"需要特殊处理的目标"跑：
        //     · pig2mod 的实体（它 override 了血量/移除，普通伤害杀不掉）
        //     · 已被标记处决的实体（DOOMED）
        //   普通僵尸/猪：原版伤害就够了，不走我们这套 ✓
        boolean needsSpecialKill = living.getClass().getName().startsWith("kakiku.pig2mod")
                || isDoomed(living);
        if (!needsSpecialKill) {
            return;
        }

        if (player.level().isClientSide) {
            player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        LiuDeathHelper.liuDie(living, player.damageSources().playerAttack(player));
    }

    public static void onHandleInteract(Object self, Object packet) {
        try {
            if (!(self instanceof net.minecraft.server.network.ServerGamePacketListenerImpl impl)) return;
            if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundInteractPacket p)) return;

            net.minecraft.server.level.ServerPlayer player = impl.player;
            if (player == null) return;
            if (!(player.getMainHandItem().getItem() instanceof BlackGoldSword)) return;

            net.minecraft.world.entity.Entity target = p.getTarget(player.serverLevel());
            if (!(target instanceof net.minecraft.world.entity.LivingEntity living)) return;

            LiuDeathHelper.liuDie(living, player.damageSources().playerAttack(player));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}