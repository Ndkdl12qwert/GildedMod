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

package com.gildedmod.agent;

import com.gildedmod.GildedMod;
import com.gildedmod.LiuArmorCore;
import com.gildedmod.LiuCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 穿满鎏金甲时每 tick 的维护工作。
 *
 * <p><b>性能要点</b>（这套逻辑曾经把服务端卡到 "Can't keep up! 416 ticks behind"）：
 * <ul>
 *     <li>C++ 侧所有 native 调用都做了句柄缓存，每次只做几次 JNI 调用；</li>
 *     <li>这里也做"能省就省"：只在阶段切换时处理一次、没有负面效果就不建 List、
 *         方法级回血兜底从每 tick 降到每 {@value #FALLBACK_HEAL_INTERVAL} tick 一次。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GildedMod.MODID)
public class LiuArmorTicker {

    /** 用 log4j 输出：System.out 在非 launch.bat 启动时会丢，log4j 一定落进 latest.log ✓ */
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger("gildedmod-ticker");


    /** 方法级回血兜底（nativeSetHealthJVMTI）的间隔，单位 tick。每 tick 调没必要。 */
    private static final int FALLBACK_HEAL_INTERVAL = 20;

    /**
     * 周期性自愈间隔（tick）。600 tick = 30 秒。
     *
     * <p>把 com/gildedmod/** 的所有类从 jar 原始字节重建一遍。
     * 别的 mod 在加载期改过我们的类（C1 已记录过这种篡改）→ 每 30 秒打回原形。
     *
     * <p>成本：一次重建 60~150 个类，约几毫秒，30 秒一次可忽略。
     */
    /**
     * 周期性自愈间隔（tick）。<b>默认 0 = 关闭</b>。
     *
     * <p>为什么默认关：一次自愈要 retransform ~150 个类 → 短暂 Stop-The-World →
     * 实测每 30 秒来一次会让游戏明显卡住 ✗
     *
     * <p>需要的话用 -Dgilded.selfheal.interval=6000（5 分钟一次）打开 ✓
     */
    /**
     * 周期性自愈间隔（tick）。来源优先级：
     * <ol>
     *     <li>{@code -Dgilded.selfheal.interval=N}（命令行最高优先）</li>
     *     <li>{@code config/gildedmod/kernel.properties} 里的 {@code selfheal.interval}</li>
     *     <li>默认 {@code 0 = 关闭}</li>
     * </ol>
     *
     * <p><b>⚠ 开启会卡：</b>每次自愈把 ~150 个 com/gildedmod/** 类
     * 从原始字节 retransform 一遍，会短暂 Stop-The-World（实测 0.2~1 秒）。
     * 只在"怀疑被别的 mod 篡改"时临时开，平时保持 0。
     */
    private static final int SELFHEAL_INTERVAL = resolveSelfHealInterval();

    /** 读配置 + 打警告（只在类初始化时跑一次） */
    private static int resolveSelfHealInterval() {
        int value = 0;

        // ① 配置文件
        java.io.File f = new java.io.File("config/gildedmod/kernel.properties");
        if (f.isFile()) {
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(f),
                            java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String k = line.substring(0, eq).trim();
                    String v = line.substring(eq + 1).trim();
                    if (k.equals("selfheal.interval")) {
                        try {
                            value = Integer.parseInt(v);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.warn("[SELFHEAL] 读配置失败：{}", String.valueOf(t));
            }
        }

        // ② 命令行优先
        value = Integer.getInteger("gilded.selfheal.interval", value);

        // ③ 明确标注会不会卡
        if (value > 0) {
            LOG.warn("[SELFHEAL] ⚠ 周期性自愈已开启：间隔 {} tick（{} 秒）",
                    value, value / 20);
            LOG.warn("[SELFHEAL] ⚠ 每次执行会 retransform ~150 个类，游戏会卡 0.2~1 秒");
            LOG.warn("[SELFHEAL] ⚠ 只在怀疑被别的 mod 篡改时使用；平时请把 selfheal.interval 设为 0");
        } else {
            LOG.info("[SELFHEAL] 周期性自愈：关闭（推荐）。需要时在 config/gildedmod/kernel.properties 里设 selfheal.interval");
        }
        return value;
    }

    /** 要清扫的 mod 包前缀（与 Pig2BlockMixin 保持一致） */
    private static final String BLOCK_PACKAGE = "kakiku.pig2mod";

    private static int tickCounter;

    /**
     * 上一 tick 处于"穿满甲"状态的玩家。
     * <p>之前用的是单个 boolean —— 单人世界够用，多人会互相覆盖（A 穿甲 / B 脱甲 → 状态乱跳）。
     * <p>用它做<b>边沿触发</b>：封禁/解封只在穿脱甲的那一次执行，而不是每 tick 重复调用。
     * <p>⚠ 之前的 bug：只调用过 {@code nativeSealPlayer(player, true)}，从来没调用过 false，
     * 于是脱甲之后玩家仍然被 JVMTI 封着 —— {@code /kill} 依旧无效。现在按边沿两个方向都调。
     */
    private static final java.util.Set<java.util.UUID> ARMORED = new java.util.HashSet<>();

    /** 实体 id → 下次允许跑重活（ultkill）的 tick。防止对打不死的目标每 tick 重复抹除。 */
    private static final java.util.Map<Integer, Integer> ULT_COOLDOWN =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 已经报过"血量项被换回原生"的玩家。
     *
     * <p>只用于限流日志：真的被篡改时每 tick 都会命中，不限制的话控制台会被刷爆。
     * 用并发的 Set 是因为 tick 在服务端线程、而查询可能在别的线程。
     */
    private static final java.util.Set<java.util.UUID> TAMPER_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 玩家登出时**必须解封** —— 否则 JVMTI 那边的 {@code NewGlobalRef} 会永久保留这个
     * {@code ServerPlayer} 对象（连带 entityData / inventory / chunkTracker 全都不能回收）。
     *
     * <p>为什么以前会泄漏：封禁/解封只在「穿甲 ↔ 脱甲」状态切换时触发，
     * 而「穿着甲直接退出」这个路径永远走不到解封 —— 每登录-退出一次泄漏一个完整实体。
     *
     * <p>这里还顺手清掉 {@code ARMORED} 缓存，避免 UUID 集合无限增长。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        try {
            LiuCore.nativeSealPlayer(player, false);      // ★ 解封（DeleteGlobalRef）
        } catch (Throwable ignored) {
        }
        // ★ 脱登出时必须解锁血量，否则下次登录如果没穿甲，血量还被冻在最大值
        try {
            com.gildedmod.HealthLock.unlock(player);
        } catch (Throwable ignored) {
        }
        ARMORED.remove(player.getUUID());
    }


    /**
     * 服务端停止时清空装甲登记表。
     *
     * <p>C++ 侧持有的是 {@code NewGlobalRef} 的 {@code ServerPlayer}；
     * 不清的话这些引用会一直吊着玩家对象（连带 entityData / inventory），
     * 在 IDE 里反复重载世界时尤其明显。
     */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        try {
            LiuArmorCore.nativeArmorClear();
        } catch (Throwable ignored) {
        }
        ARMORED.clear();
        TAMPER_LOGGED.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long t0 = com.gildedmod.GildedDebug.begin();

        // 处决名单递减（空名单时零开销，内部第一行就返回）
        LiuCore.nativeDoomTick();

        tickCounter++;

        // ★★ 玩家血量锁（2026-11-07）：把它钉在最大血量。
        //
        //   为什么需要：别的 mod 可以绕过 hurt/actuallyHurt 直接改血量 ✗
        //   （我们实测对战"元氏忒修斯"时被这样打死 —— 守卫拦不住 ✓）
        //   每 tick 把血量顶回最大值 = 不管什么路径造成伤害，下一 tick 就复原 ✓
        //
        //   成本：每个玩家 1 次 getHealth + 必要时 1 次 setHealth ✓ 可忽略
        try {
            for (net.minecraft.server.level.ServerLevel lv : event.getServer().getAllLevels()) {
                for (net.minecraft.world.entity.player.Player p : lv.players()) {
                    if (p.isDeadOrDying()) {
                        continue;
                    }
                    float max = p.getMaxHealth();
                    if (p.getHealth() < max) {
                        p.setHealth(max);
                    }

                    // ★ 原版无敌标志（2026-11-07）：Entity.hurt 内部先查 isInvulnerableTo()
                    //   比我们自己的 Mixin 守卫更底层 -> 走原版伤害路径的 mod 绕不过
                    //   每 tick 重设，防止被别的 mod 清掉
                    if (!p.isInvulnerable()) {
                        p.setInvulnerable(true);
                    }
                    if (p.tickCount % 20 == 0) {
                        com.gildedmod.HealthLock.lock((net.minecraft.world.entity.LivingEntity) p, max);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        boolean doFallbackHeal = (tickCounter % FALLBACK_HEAL_INTERVAL) == 0;

        // 一次性：对 jvm.dll 打最小补丁（拦 JVM_Halt / JVM_GC）
        if (tickCounter == 60 && com.gildedmod.LiuKernelConfig.jvmPatchEnabled()) {
            try {
                int patched = LiuCore.nativeInstallJvmKernelHooks();
                LOG.info("[KERNEL-JVM] JVM 本体补丁生效 {} 个", patched);
            } catch (Throwable t) {
                LOG.warn("[KERNEL-JVM] 补丁失败：{}", String.valueOf(t));
            }
        }

        // 周期性自愈：每 30 秒把我们的类从原始字节重建一遍（防别的 mod 篡改）
        if (SELFHEAL_INTERVAL > 0 && tickCounter % SELFHEAL_INTERVAL == 0) {
            try {
                int healed = LiuCore.nativeSelfHeal();
                LOG.info("[SELFHEAL] 周期性重建完成：{} 个类", healed);
            } catch (Throwable t) {
                LOG.warn("[SELFHEAL] 失败：{}", String.valueOf(t));
            }
        }

        // SWEEP_PROBE：只打一次，证明这个 ticker 真的在跑
        if (tickCounter == 2) {
            LOG.info("[PIG2] 清扫器在线（tick=2），每 20 tick 扫一次 kakiku.pig2mod");
        }

        // CP 启动自检（C1 完整性 / C2 自愈 / C3 生效）—— 只跑一次
        if (tickCounter == 40) {
            try {
                com.gildedmod.CP.startup();
            } catch (Throwable t) {
                System.out.println("[CP] 自检失败: " + t);
            }
        }

        // ★ 追踪服务：每 tick 把所有登记目标的血量字段重新解析并写 0
        //   （不缓存地址 → GC 搬动无影响 ✓）
        // ✗ 2026-11-07 停用：每 tick 写 Float.value 会与 GC/装箱对象竞争
        // if (true) { LiuCore.nativeTrackZeroAll(); }

        // ★ 兜底清扫：每 20 tick 把世界上所有 kakiku.pig2mod 实体重新送进抹除队列。
        //
        //   为什么要这层：Pig2BlockMixin 拦的是 ServerLevel.addFreshEntity，
        //   但实体也可能从**存档加载**进来（重启后世界里已有的那些），
        //   那条路不走 addFreshEntity ✗。所以再补一个周期扫描 ✓。
        //
        //   为什么 20 tick 一次：getAllEntities() 是遍历整张实体表，
        //   每 tick 做没必要；1 秒一次足够把复活压住，也不影响 tick 预算。
        if ((tickCounter % 20) == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                try {
                    for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
                        if (e == null || e.isRemoved()) {
                            continue;
                        }
                        if (!e.getClass().getName().startsWith(BLOCK_PACKAGE)) {
                            continue;
                        }
                        // 注意：这里的 e 是 Entity，没有 getHealth()（那是 LivingEntity 的），
                        //       所以日志里只打类名和 id，血量等 native 侧再读。
                        LOG.info("[PIG2] 清扫命中: " + e.getClass().getSimpleName()
                                + " id=" + e.getId());
                        // ★ 冷却：同一个实体 100 tick 内只跑一次【重活】
                        //
                        //   之前每 20 tick 对每只 Pig2 跑完整 ultkill → 3 分钟 1000+ 次
                        //   → Can't keep up 3 次（tick 被拖 2~3 秒）。
                        //   现在：重活 5 秒一次；轻活（木桩化）每次都做，几乎没有成本。
                        // ★ 清扫只做【轻活】：压血 + 木桩化。
                        //
                        //   为什么把 ultkill 从清扫里拿掉：
                        //     实测 3 分钟 1445 条 [ULT]（≈180 次完整抹除）→
                        //     每次都是 6 条路径 + 非虚调用 + 十几行日志 →
                        //     Can't keep up 3~4 次（2~4.5 秒一次）✗
                        //   而它本来也杀不死（levelCallback 被换掉）✗
                        //   → 重活只在【用剑砍】时触发（每次挥剑一次，天然有间隔）✓
                        int eid = e.getId();
                        int next = ULT_COOLDOWN.getOrDefault(eid, 0);
                        if (tickCounter >= next) {
                            ULT_COOLDOWN.put(eid, tickCounter + 100);
                            // ✗ 2026-11-07 停用：doom 名单每 tick 打印 → 实测 1224 条刷屏
                            // LiuCore.nativeDoomMark(e, 200);
                        }

                        // ★ 关键：把它变成【木桩】
                        //
                        //   为什么光抹除不够：Pig2 在构造时把 Entity 的 levelCallback
                        //   换成了自己的实现（mLevelCallbackBackup），于是
                        //   Entity.remove() 里的 levelCallback.onRemove() 调的是它的版本
                        //   → 世界层收不到移除通知 → 实体还在区块里、还在 tick、还在打人。
                        //
                        //   所以除了抹除，还要从行为层面掐死它：
                        //     noAi        → 不思考、不寻路、不攻击
                        //     noGravity   → 不掉下去也不飘
                        //     速度归零     → 不会被击退推动
                        //     invulnerable→ 顺便让它不再触发伤害计算
                        // ★ 只读它的血量字段（写丢弃 / 读恒为 0）
                        //   用的是 HealthLock：把 entityData 里血量那个 DataItem
                        //   换成 FrozenItem。不碰它的私有字段、不碰引用、不碰 GC ✓
                        // ★ 纯 JNI：直接写它血量字段那 4 个字节（每轮重写）
                        //   Java 版 HealthLock 会被它看见/换回来 ✗
                        //   这个只改 Float.value，对象不变 → 看不见 ✓
                        // ✗ 2026-11-01 09:48 崩了（hs_err_pid12880）：纯 JNI 写 Float.value
                        //   会崩 —— 与之前三次同类（写 JVM 堆里的对象字段）。
                        //   结论：堆内写入这条路不可用，退回只读锁由 HealthLock 负责。
                        //   这里只做压血 + 木桩化（都不碰堆内存）。

                        // ★ 你自己的函数（C++ 侧 nativeAbsoErase）：读血量并归一化
                        try {
                            float h = LiuCore.absoErase(e);
                            LOG.info("[ABFLA] Java 收到 {}", h);
                        } catch (Throwable ignored) {
                        }

                        // ★ 登记进追踪服务（GC 安全：只存 GlobalRef，每次重解析）
                        try {
                            LiuCore.nativeTrackAdd(e);
                        } catch (Throwable ignored) {
                        }

                        // ★ B 路：从世界实体表强制摘除（不经过它的 levelCallback）
                        // ✗ 2026-11-07 停用：改世界实体表会破坏实体状态
                        //   实测崩溃：Projectile.m_6686_(setPos) reading 0x36b6b7b20
                        // if (true) { LiuCore.nativeForceEvict(e); }

                        if (e instanceof net.minecraft.world.entity.Mob mob) {
                            mob.setNoAi(true);
                        }
                        e.setNoGravity(true);
                        e.setDeltaMovement(0.0, 0.0, 0.0);
                        e.setInvulnerable(true);
                        e.setSilent(true);
                    }
                } catch (Throwable ignored) {
                    // 清扫失败绝不能把服务端 tick 打断
                }
            }
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Player player : level.players()) {
                boolean isArmor = isFullGildedArmor(player);
                java.util.UUID id = player.getUUID();
                boolean wasArmor = ARMORED.contains(id);

                // ★ 只在【状态切换】时做封禁/解封（穿甲封、脱甲解）。
                //   之前这里只调过 true，从没调过 false → 脱甲后玩家仍被封着，/kill 无效 ✗
                if (isArmor != wasArmor) {
                    if (isArmor) {
                        ARMORED.add(id);
                    } else {
                        ARMORED.remove(id);
                    }
                    try {
                        LiuCore.nativeSealPlayer(player, isArmor);
                    } catch (Throwable ignored) {
                    }
                    // ★ 登记等级：穿满甲 = 3（全护），脱下 = 0（注销）
                    //   这一步替代了旧的 nativeSetArmorPlayer(boolean)：
                    //   C++ 侧要 level 才能区分"只护血"和"全护"，boolean 表达不了。
                    try {
                        LiuArmorCore.nativeSetArmorLevel(player, player.getId(), isArmor ? 3 : 0);
                    } catch (Throwable ignored) {
                    }
                    // ★ 脱甲时把血量字段还原，否则"锁定的最大值"会一直留着
                    if (!isArmor) {
                        try {
                            com.gildedmod.HealthLock.unlock(player);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                if (!isArmor) {
                    continue;
                }

                // 2. 锁死满血（C++ 侧句柄已缓存，正常路径只有 3 次 JNI 调用）
                try {
                    LiuCore.nativePinHealth(player, player.getMaxHealth());
                } catch (Throwable ignored) {
                }

                // 2.5 ★★ 把血量【字段本身】换成只读（SynchedEntityData.DataItem 子类）
                //
                //   为什么这一层比钩 setHealth 更硬：
                //     · 它在数据里，所有 Java 方法之下
                //     · setHealth / hurt / 非虚调用 / 别的 mod 的 override —— 全都要走 entityData
                //     · getValue() 恒定返回锁定值 → 连"读出真血量"都做不到
                //   代价：每 tick 一次 map 查找（无分配、无反射、无 JNI）
                //
                //   verify() 用来发现"有人把 DataItem 换回原生的"（直写绕过）。
                try {
                    com.gildedmod.HealthLock.lock(player, player.getMaxHealth());
                    if (!com.gildedmod.HealthLock.verify(player)
                            && TAMPER_LOGGED.add(id)) {
                        System.out.println("[HPLOCK] ⚠ 检测到篡改：id=" + player.getId()
                                + " 的血量项被换回原生 DataItem，已重新装上只读项");
                    }
                } catch (Throwable ignored) {
                }

                // 3. 方法级回血兜底，间隔执行
                if (doFallbackHeal) {
                    try {
                        LiuCore.nativeSetHealthJVMTI(player, player.getMaxHealth());
                    } catch (Throwable ignored) {
                    }
                }

                // 4. 清状态（纯字段写入，很便宜）
                if (player.isOnFire()) {
                    player.clearFire();
                }
                player.setRemainingFireTicks(-100);
                player.setAirSupply(300);
                player.fallDistance = 0.0f;
                player.hurtTime = 0;
                player.hurtDuration = 0;
                player.invulnerableTime = 20;

                // 5. 只清"有害"效果；没有有害效果时连 List 都不建
                clearHarmfulEffects(player);
            }
        }

        com.gildedmod.GildedDebug.end("ticker", t0);
        com.gildedmod.GildedDebug.tickDone();
    }

    /** 只移除 {@code !isBeneficial()} 的效果 —— 绝不能用 removeAllEffects（会连增益一起删）。 */
    private static void clearHarmfulEffects(Player player) {
        List<MobEffectInstance> harmful = null;
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (!instance.getEffect().isBeneficial()) {
                if (harmful == null) {
                    harmful = new ArrayList<>(4);
                }
                harmful.add(instance);
            }
        }
        if (harmful == null) {
            return;
        }
        for (MobEffectInstance instance : harmful) {
            player.removeEffect(instance.getEffect());
        }
    }

    /**
     * 逐槽判断，避免 {@code getArmorSlots()}（它每次都会 new 一个 ArrayList）。
     * <p>这个方法每 tick 每个玩家跑一次，所以选择了零分配的写法。
     */
    private static boolean isFullGildedArmor(Player player) {
        return isGilded(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD))
                && isGilded(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST))
                && isGilded(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS))
                && isGilded(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
    }

    private static boolean isGilded(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.gildedmod.BlackGoldArmor;
    }
}
