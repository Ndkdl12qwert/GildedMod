package com.gildedmod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 注入/断点回调用的<b>纯 Java</b> 守卫类。
 *
 * <h3>为什么必须是纯 Java</h3>
 * L2 的字节码注入与 L3 的 JVMTI 断点回调都会在 <b>很早</b> 的时机调用这里
 * （{@code LivingEntity} 加载期间 / 每次 {@code hurt}）。
 * 那时 {@code LiuCore} 的 native 方法可能还没绑定 ✗ —— 调 native 会抛
 * {@code UnsatisfiedLinkError} ✗，异常冒出去会崩服务端 ✗。
 * 纯 Java 永远可用 ✓
 *
 * <h3>性能要求</h3>
 * {@link #shouldGuard(Object)} 会在<b>每次受伤判定</b>时被调用 ✓
 * 所以只做一次 {@code instanceof} + 一次背包检查 ✓ 不分配对象 ✓
 */
public final class LiuGuard {

    /** 总开关：false 时所有注入点立刻返回（零副作用） */
    public static volatile boolean enabled = true;

    /** 被 L2 注入调用的次数（验证注入是否生效） */
    public static volatile long setHealthCalls;

    private LiuGuard() {
    }

    /**
     * 被 L2 注入到 {@code LivingEntity.setHealth(F)V} 开头。
     *
     * <p>注入时只传了 {@code this}（单参数）→ 拿不到血量值 ✓
     * 所以这里只做计数，证明注入通路可用 ✓ 不改变行为 ✓
     */
    public static void onSetHealth(LivingEntity entity) {
        if (!enabled) {
            return;
        }
        setHealthCalls++;
    }

    /**
     * 被 L3 的 JVMTI 断点回调调用：这个实体是否该被保护（免伤）。
     *
     * <ul>
     *   <li>玩家 + （4 件鎏金甲 或 手持剑）→ {@code true} → hurt 被强制返回 false ✓</li>
     *   <li>其它 → {@code false} → 正常受伤 ✓（我们的击杀链要靠伤害生效 ✓）</li>
     * </ul>
     */
    public static boolean shouldGuard(Object entity) {
        if (!enabled) {
            return false;
        }
        if (!(entity instanceof Player player)) {
            return false;
        }
        try {
            return GildedMod.isProtected(player);
        } catch (Throwable t) {
            return false;
        }
    }
}
