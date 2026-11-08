package com.gildedmod;

/**
 * 「拦截其它 mod 攻击方法」的判定与开关。
 *
 * <p>由 {@code LiuTransformer} 在类加载时调用；返回 true 才交给
 * {@code LiuAttackStubVisitor} 去替换方法体。
 *
 * <h3>安全白名单（这些包一律不碰）</h3>
 * <ul>
 *     <li>{@code net/minecraft/**} —— 原版，被多个 Mixin 抢改，动它必崩 ✗</li>
 *     <li>{@code com/gildedmod/**} —— 我们自己的（要能打人 ✓）</li>
 *     <li>{@code java/ jdk/ sun/ com/sun/} —— JDK</li>
 *     <li>{@code org/ cpw/ net/minecraftforge/ net/minecraftforge/} —— 框架 / 库</li>
 *     <li>{@code io/netty/ com/mojang/} —— 网络 / 基础库</li>
 * </ul>
 * 其余（各 mod 自己的类）才可能被拦截 ✓
 */
public final class LiuAttackGuard {

    /** 总开关，默认开；想关掉：-Dliu.block.attacks=false 或到这里改成 false */
    private static final boolean ENABLED =
            // ★ 默认关闭（2026-11-08）：规则②（"参数含 Entity 且返回 boolean"）命中了
            //   渲染可见性判定 → 所有实体都不渲染 ✗ 眼睛看不见自己/生物 ✗
            //   要开：-Dliu.block.attacks=true 或 config 里 block.attacks=true
            !"false".equalsIgnoreCase(System.getProperty("liu.block.attacks", "true"));

    /**
     * 渲染相关的类名关键词：命中就【一律不碰】。
     *
     * <p>为什么需要：2026-11-08 实测，把"参数含 Entity 且返回 boolean"的方法全替换成
     * return false 之后，渲染可见性判定（shouldRender / isVisible / Predicate&lt;Entity&gt;）
     * 全部返回 false → 玩家和所有生物都不渲染 ✗ 眼睛看不见自己 ✗
     * 所以对渲染相关的类整体跳过 ✓
     */
    private static final String[] RENDER_HINTS = {
            "render", "Render", "model", "Model", "visibility", "Visibility",
            "frustum", "Frustum", "cull", "Cull", "layer", "Layer",
            "gui", "Gui", "screen", "Screen", "client", "Client",
            "shader", "Shader", "texture", "Texture",
    };

    /** 一律不碰的前缀 */
    private static final String[] SAFE = {
            "net/minecraft/",
            "com/gildedmod/",
            "java/", "javax/", "jdk/", "sun/", "com/sun/",
            "org/", "cpw/", "net/minecraftforge/", "net/minecraftforge/",
            "io/netty/", "com/mojang/", "it/unimi/", "com/google/",
    };

    private LiuAttackGuard() {
    }

    public static boolean shouldBlock(String internalName) {
        if (!ENABLED || internalName == null || internalName.isEmpty()) {
            return false;
        }
        for (String prefix : SAFE) {
            if (internalName.startsWith(prefix)) {
                return false;
            }
        }
        // 渲染相关的类一律跳过（否则看不见实体 ✗）
        for (String hint : RENDER_HINTS) {
            if (internalName.contains(hint)) {
                return false;
            }
        }

        // 只处理 mod 的类：必须有包名（没有包名的多半是特殊生成类，跳过更安全）
        return internalName.indexOf('/') > 0;
    }
}
