package com.gildedmod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀注册表：记录"哪些生物类要被秒杀"。
 *
 * <h3>为什么需要注册表</h3>
 * 把所有 {@code LivingEntity} 子类都改写会连玩家一起杀掉 ✗，
 * 而对 {@code net/minecraft/**} 核心类做 retransform 会剥掉 Mixin、直接崩 ✗。
 * 所以必须<b>按类显式注册</b>：
 * <pre>
 *   你用黑金剑砍中某只生物
 *        → 它的类名进表
 *        → 这只当场被 nativeSmite 秒掉
 *        → 之后同类生物【出生即秒杀】（加载时就被改写）
 * </pre>
 */
public final class LiuInstantKill {

    /** 已注册的类（内部名，如 {@code kakiku/pig2mod/entity/Pig2}）。 */
    private static final Set<String> TARGETS = ConcurrentHashMap.newKeySet();

    /** 已被 transformer 确认属于 LivingEntity 后代的类（用于判断"能不能改"）。 */
    private static final Set<String> LIVING = ConcurrentHashMap.newKeySet();

    /** 永久黑名单：MC 核心类，改了必崩。 */
    private static final String[] FORBIDDEN = {
            "net/minecraft/world/entity/LivingEntity",
            "net/minecraft/world/entity/player/Player",
            "net/minecraft/world/entity/Entity",
            "net/minecraft/world/entity/Mob",
            "net/minecraft/world/entity/animal/Animal",
            "net/minecraft/world/entity/monster/Monster",
            "net/minecraft/server/level/ServerLevel",
    };

    static {
        LIVING.add("net/minecraft/world/entity/LivingEntity");
    }

    private LiuInstantKill() {
    }

    public static boolean isForbidden(String internalName) {
        for (String f : FORBIDDEN) {
            if (f.equals(internalName)) {
                return true;
            }
        }
        return internalName.startsWith("net/minecraft/");
    }

    /** transformer 用：这个类的父类是不是 LivingEntity 后代？ */
    public static boolean markIfLiving(String internalName, String superName) {
        if (internalName == null || superName == null) {
            return false;
        }
        if (LIVING.contains(superName)) {
            LIVING.add(internalName);
            return true;
        }
        return false;
    }

    /** 登记一个要秒杀的类（内部名）。 */
    public static boolean register(String internalName) {
        if (internalName == null || isForbidden(internalName)) {
            return false;
        }
        boolean added = TARGETS.add(internalName);
        if (added) {
            System.out.println("[KILL-REG] 已登记秒杀类: " + internalName);
        }
        return true;
    }

    /** transformer 用：这个类要不要改写？ */
    public static boolean shouldRewrite(String internalName) {
        return internalName != null && TARGETS.contains(internalName) && !isForbidden(internalName);
    }

    /** 由实体对象登记（取自它的类名）。 */
    public static boolean registerEntity(Object entity) {
        if (entity == null) {
            return false;
        }
        return register(entity.getClass().getName().replace('.', '/'));
    }

    public static int size() {
        return TARGETS.size();
    }
}
