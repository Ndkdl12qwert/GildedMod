package com.gildedmod;

public class LiuArmorCore {

    static {
        // dll 现在打在 jar 里（natives/windows/），由 NativeLoader 统一处理：
        //   ① mods/ 里有 → 直接用（方便临时替换调试版）
        //   ② 没有 → 从 jar 解压到 config/gildedmod/native/ 再加载
        // 不再手写"先找 mods 再找资源"的两份逻辑。
        System.out.println("========== LiuArmorCore 静态块开始 ==========");
        try {
            if (NativeLoader.load("GildedArmor.dll")) {
                System.out.println("========== LiuArmorCore: 加载成功 ==========");
            } else {
                System.err.println("========== LiuArmorCore: 加载失败（mods/ 与 jar 里都没有）==========");
            }
        } catch (Throwable e) {
            System.err.println("========== LiuArmorCore: DLL 加载失败 ==========");
            e.printStackTrace();
        }
    }







    // ========================================================================
    //  原生导出（共 8 个 + JNI_OnUnload，每个都有真实调用点）
    //
    //  ⚠ 2026-10-31 清理：删掉了 8 个"写了但没人调用"的函数：
    //      nativeSetArmorPlayer / nativeIsArmorPlayer / nativeArmorPlayerCount
    //      nativeArmorLevel / nativeSetGuardEnabled / nativeIsGuardEnabled
    //      nativeGuardSetHealth(旧) / nativeGuardRemove(旧, 恒 false)
    //      nativeGuardDrift —— 血量字段被锁死后 getHealth() 恒为最大值，
    //                          对账永远不会触发，等于死代码
    // ========================================================================

    /**
     * 登记 / 注销一个装甲玩家（带等级）。
     *
     * <p>为什么需要 id：JNI 的 {@code jobject} 是句柄，同一对象在不同调用里
     * 可能拿到不同句柄，所以旧接口只能逐个 {@code IsSameObject} 线性扫。
     * 传入 {@code entity.getId()} 之后可以 O(1) 命中，热路径通常 0 次 JNI。
     *
     * @param id    {@code player.getId()}
     * @param level 0=注销；1=只护血；2=护血+移除；3=全护（含 die）
     */
    public static native void nativeSetArmorLevel(Object player, int id, int level);

    /** 清空登记表（服务端停止时调用，避免 C++ 侧留着 NewGlobalRef）。 */
    public static native void nativeArmorClear();

    /**
     * 血量守卫（加强版）：比旧版多一个 {@code maxHealth} 上限校验。
     *
     * <p>旧版只比较新旧血量，于是 {@code setHealth(999999)} 会被放行 ✗ ——
     * 血量溢出之后下一步常常变成 NaN 或被判成"已经死了"。
     *
     * @return true = 允许这次写入
     */
    public static native boolean nativeGuardSetHealthEx(float current, float next, float maxHealth);

    /**
     * 移除守卫：按 {@code Entity.RemovalReason} 的<b>名字</b>判断该不该拦。
     *
     * <p>只拦死亡与抹除（{@code KILLED} / {@code DISCARDED} 及未知原因），
     * <b>放行</b>换维度、登出、区块卸载 —— 否则玩家会卡在原地无法离开世界。
     *
     * @return true = 允许移除
     */
    public static native boolean nativeGuardRemoveReason(String reasonName);

    /** 死亡守卫：false = 取消 {@code die()}。 */
    public static native boolean nativeGuardDie();

    /** 一行摘要：各分类被拦了多少次（{@code /gildedtime armorstats} 用）。 */
    public static native String nativeGuardStats();

    /** 最近 64 条拦截事件（新的在前），用于复盘。 */
    public static native String nativeGuardLog();

    /** 清空统计与事件（不影响装甲登记）。 */
    public static native void nativeGuardResetStats();

    /**
     * 清掉"负面"状态：着火 + 所有<b>有害</b>药水效果。
     *
     * <p>⚠ 不能用 {@code removeAllEffects()} ✗ —— 它会连<b>增益</b>一起删掉。
     * 而本方法是被 {@code LiuArmorTicker} <b>每 tick</b> 调用的，
     * 于是信标/潮涌核心/其它 mod 每 tick 重新给回来的增益会被反复删掉，
     * 表现就是"增益药水效果一闪一闪" ✗（实际上增益一直没生效）。
     * 这里只挑 {@code !isBeneficial()} 的效果移除，增益原样保留。
     */
    public static void clearDebuffs(net.minecraft.world.entity.player.Player player) {
        player.clearFire();

        // 先收集再移除：直接边遍历边改会抛 ConcurrentModificationException
        java.util.List<net.minecraft.world.effect.MobEffect> bad = new java.util.ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance instance : player.getActiveEffects()) {
            if (!instance.getEffect().isBeneficial()) {
                bad.add(instance.getEffect());
            }
        }
        for (net.minecraft.world.effect.MobEffect effect : bad) {
            player.removeEffect(effect);
        }
    }
}