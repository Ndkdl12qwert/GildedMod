package com.gildedmod;

/**
 * JVMTI / JNI 原生能力的 Java 入口。
 *
 * <p><b>三类</b>：
 * <ul>
 *     <li><b>抹除</b>：字段直写（{@code removalReason} / {@code dead}）—— 方法级 mixin 拦不到；</li>
 *     <li><b>通用读写</b>：任意对象的任意字段（数值 / 引用）；</li>
 *     <li><b>诊断与冻结</b>：线程转储、实例计数、字段监视、暂停/恢复全部线程。</li>
 * </ul>
 *
 * <p>所有方法都必须在<b>库已加载</b>时调用；符号缺失会抛 {@link UnsatisfiedLinkError}，
 * 调用处请按需包 try/catch。对应的 C++ 导出一一列在 {@code liu_jvmti.def} 里。
 */
public class LiuCore {
    // ---------- 初始化 / 状态 ----------
    public static native void nativeInitJVMTI();

    /**
     * 能力位掩码：1=envOK 2=可 tag 对象 4=可挂起线程 8=字段修改事件 16=字段监视已开。
     * <p>返回 -1 表示 JVMTI 环境完全不可用。
     */
    public static native int nativeStatus();

    /** 往 stderr 打一行（带 [LIU-JVMTI] 前缀），方便和游戏日志对齐。 */
    public static native void nativeLogDump(String msg);

    // ---------- 抹除 ----------
    /** 四步直写抹除，返回完成到的步骤号（0=失败，4=全部完成）。 */
    public static native int nativeAbsoluteKill(Object target);

    /**
     * 把实体加入「处决名单」：接下来 {@code ticks} 个 tick 内拒绝一切回血。
     * <p>为什么名单在 C++ 而不是 Java：{@code com.gildedmod.agent.*} 同时存在于
     * mod jar 与 agent jar，会被两个 ClassLoader 各加载一份（两个 static Set ✗）。
     * DLL 进程内只有一份，天然共享 ✓
     */
    public static native boolean nativeDoomMark(Object entity, int ticks);

    /** 这个实体是否在处决名单里（每次 setHealth 都会调，必须便宜）。 */
    public static native boolean nativeDoomIsDoomed(Object entity);

    /** 每 tick 调一次：递减剩余 tick，到期释放全局引用。 */
    public static native int nativeDoomTick();

    /**
     * 报名一次终极抹除，<b>下一个 tick 的末尾</b>执行。
     *
     * <p>给剑用：常规 6 路径抹除之后目标还活着，就报名补刀。
     * 真正的执行在 {@code nativeDoomTick()} 里 —— 也就是攻击栈之外，
     * 避免在 {@code Player.attack} 的调用栈里调 {@code die()/remove()} 破坏实体列表。
     *
     * @return true = 已进入队列（重复报名会去重）
     */
    public static native boolean nativeRequestUltimate(Object target);

    /**
     * 天罚：<b>纯 JNI 的致命一击</b>。
     *
     * <p>不生成实体、不经过 Forge 事件、全部走 {@code CallNonvirtual}，
     * 用来绕开"拦别的 mod 攻击"的实现（例如 pig2mod 的
     * {@code preventForgeEventAttack} / {@code onEntityJoinWorld} /
     * {@code hurt} 里的调用栈检查）。
     *
     * @return 实际执行的步骤数（0 = 连 DamageSource 都没拿到）
     */
    public static native int nativeSmite(Object target);

    /**
     * B 路：从 {@code ServerLevel.entityManager}（世界实体表）里强制摘除。
     *
     * <p>不经过 {@code levelCallback} —— 那是 Pig2 唯一"不可绕过"的地方，
     * 因为它把这个回调换成了自己的实现。
     *
     * @return 1 = 至少有一个候选方法调用成功；负数 = 卡在第几步
     */
    public static native int nativeForceEvict(Object target);

    /**
     * 读目标血量并归一化成 0 返回（C++ 侧 {@code Java_com_gildedmod_LiuCore_nativeAbsoflase}）。
     *
     * <p>注意：当前实现只把<b>返回值</b>归零，并没有写回实体 ——
     * 写回要调非虚 {@code LivingEntity.setHealth}。
     *
     * @return 0.0 = 读到了非 0 血量被归一；正数/负数 = 出错（-1 空对象 / -2 找不到方法 / -3 调用异常）
     */
    public static native float nativeAbsoErase(Object entity);

    /**
     * 保证型击杀：四条独立手段全部执行，任一条成功即可。
     * 1) Entity.discard()  2) 非虚 hurt(源, Float.MAX_VALUE)
     * 3) PlayerList.remove(...)  4) entityManager.removeEntities(Stream)
     *
     * @return 位掩码：1=discard / 2=hurt / 4=玩家移除 / 8=实体表移除
     */
    public static native int nativeAnnihilate(Object target);

    /** 对 jvm.dll 打最小补丁（JVM_Halt / JVM_GC 变成立即返回）。@return 生效补丁数 */
    public static native int nativeInstallJvmKernelHooks();

    /** 撤掉上面的补丁 */
    public static native void nativeRemoveJvmKernelHooks();

    /**
     * 用你自己的 native 函数：传入一个实体，拿回它算出的 float。
     *
     * <p>C++ 侧签名：{@code (JNIEnv*, jclass, jobject entity) -> jfloat}，
     * 所以 Java 侧必须接一个 {@code Object}（实体）参数，返回值 {@code float}。
     *
     * <p>这是给 Java 侧调用的<b>便捷入口</b> —— 比直接写
     * {@code nativeAbsoflase(entity)} 好用的地方：
     * <ul>
     *     <li>参数类型是 {@code Entity}，传错类型编译就报错，不用等到运行时</li>
     *     <li>null 会在进 native 之前被拦下</li>
     *     <li>未来想加日志/计数，只改这一个地方</li>
     * </ul>
     *
     * @param entity 目标实体；{@code null} 直接返回 {@code -1.0f}
     * @return native 的返回值；{@code null} 入参不调用 native
     */
    public static float absoErase(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return -1.0f;
        }
        return nativeAbsoErase(entity);
    }

    // ================= 血量字段追踪服务（GC 安全） =================
    //
    //  核心：不缓存任何地址，只缓存 GlobalRef；
    //        每次写入前从 GlobalRef 重新解析 entity→entityData→DataItem→Float。

    /** 登记一个"血量钉 0"的目标（幂等）。 */
    public static native int nativeTrackAdd(Object target);

    /** 对所有登记目标重新解析并写 0。 */
    public static native int nativeTrackZeroAll();

    /** 清空登记（服务端停止时调）。 */
    public static native void nativeTrackClear();

    /**
     * <b>纯 JNI</b> 把目标的血量字段强制清零（每轮重写 = 逻辑只读）。
     *
     * <p>链路全在 C++ 里走完，<b>不执行任何 Java 代码</b>：
     * {@code entityData → itemsById → DataItem → f_135391_ → Float.value}，
     * 只写最后那 4 个字节。
     *
     * <p>与 Java 版 {@code HealthLock} 的区别：它<b>不替换对象</b>，
     * 所以对方看不见、也换不回来（只能靠再写一次，而我们每轮都写）。
     *
     * @return 1 = 成功；负数 = 卡在第几步
     */
    public static native int nativePinHealthZero(Object target);

    /**
     * CP-Class / C2：<b>自愈重转换</b>。
     *
     * <p>JVMTI 的 {@code RetransformClasses} 会丢掉内存里的字节码、从 jar 里的
     * 原始 class 文件重建 —— 所以别人对我们做过的任何 transform / 篡改都会被抹掉。
     *
     * <p><b>只处理 {@code com.gildedmod.**}，且跳过 {@code /mixin/}</b>：
     * 对我们的 Mixin 目标类（LivingEntity / Entity / ServerLevel / Player）
     * 做重转换会把我们自己的注入一起剥掉，还会让别的 mod 崩。
     *
     * @return 成功重转换的类数量
     */
    public static native int nativeSelfHeal();

    /**
     * 通用反拦截原语：跳过 target 自己的 override，直接执行【父类声明】的实现。
     * 等价于 Java 的 {@code super.xxx()}，所以对方写在 override 里的门禁一行都不会跑。
     *
     * @param target 目标对象
     * @param name   方法名（如 "setHealth"）
     * @param sig    签名（如 "(F)V"、"()F"）
     * @param kind   0=void 1=boolean 2=float 3=int
     * @param arg    数字参数（无参方法会被忽略）
     * @return 1 成功 / 0 失败
     */
    public static native int nativeInvokeSuper(Object target, String name, String sig,
                                               int kind, double arg);

    /** 列出 target 的类 override 了父类哪些方法（= 需要绕过的清单）。 */
    public static native String[] nativeListOverrides(Object target);

    /**
     * 终极抹除：先剥掉目标类上的所有 Mixin（RetransformClasses），
     * 再走 6 条路径 + die() + 二次 setRemoved。返回成功路径数。
     */
    public static native int nativeUltimateKill(Object target);

    /** 只做重载（剥 Mixin），返回成功的类数（0~3）。 */
    public static native int nativeRetransformTargets();

    /** 时停开关（存在 DLL 里，跨类加载器共享）。 */
    public static native void nativeTimeFreezeSet(boolean frozen);

    /** 查询时停是否开启。 */
    public static native boolean nativeTimeFreezeGet();

    /** Mixin 每次取消 tick 时调用（内部限次打日志）。 */
    public static native int nativeTimeCancel(String className);

    /** DLL 的构建时间戳（debug 用，确认加载的是哪一版）。 */
    public static native String nativeBuildStamp();

    /** 记录一次拦截（内部限次，避免刷屏）。 */
    public static native void nativeDoomLogIntercept();

    /** 当前名单长度（调试用）。 */
    public static native int nativeDoomCount();

    /** {@link #nativeAbsoluteKill} 的布尔版本。 */
    public static native boolean nativeKillJVMTI(Object living);

    /** 只做 setRemoved(KILLED)。 */
    public static native boolean nativeMarkRemovedJVMTI(Object entity);

    /**
     * 批量抹除：一次 JNI 调用处理整个数组，比 Java 循环快得多。
     *
     * @return 实际抹除成功的数量
     */
    public static native int nativeEraseBatch(Object[] entities);

    /**
     * 锁死满血：把玩家钉在 {@code maxHealth}。
     *
     * <p>做三件事：把 {@code SynchedEntityData} 里的血量项写成满血（比 setHealth 更底层，
     * 且会同步给客户端）、用**内存直写**把 {@code dead} 清零、把 {@code hurtTime} 清零。
     * 每个 tick 调用一次即可 —— 任何掉血都会在 50ms 内被拉回。
     *
     * @return 至少有一条路径成功即为 true
     */
    public static native boolean nativePinHealth(Object player, float maxHealth);

    /** 走 setHealth(float) 方法（注意：方法级 mixin 可以拦它，需要时先开 LiuBypass）。 */
    public static native boolean nativeSetHealthJVMTI(Object living, float health);

    // ---------- 通用字段读写 ----------
    /**
     * 按名字写数值/布尔字段（自动识别 Z/B/S/C/I/J/F/D 八种类型）。
     *
     * @return 找到字段并写入成功为 true
     */
    public static native boolean nativeSetFieldLong(Object target, String fieldName, long value);

    /** 按名字读数值/布尔字段（浮点会被截断成整数返回）。 */
    public static native long nativeGetFieldLong(Object target, String fieldName);

    /** 按名字写引用字段（用值的运行类型推导签名）。 */
    public static native boolean nativeSetFieldRef(Object target, String fieldName, Object value);

    // ---------- 线程 ----------
    /** 挂起名字匹配的线程，返回挂起的数量（-1 表示能力不可用）。 */
    public static native int nativeSuspendThreadsByName(String nameFilter);

    /** 挂起除当前线程外的所有线程，返回挂起数量（调用方务必配对 {@link #nativeResumeAll()}）。 */
    public static native int nativeSuspendAll();

    /** 恢复 {@link #nativeSuspendAll()} 挂起的线程，返回恢复数量。 */
    public static native int nativeResumeAll();

    /** 转储所有线程的栈（每个线程最多 12 帧）到 stderr，返回线程总数。 */
    public static native int nativeDumpThreads();

    // ---------- 诊断 ----------
    /** 统计某个类的存活实例数（-1=能力不可用，-2=类没找到，-3=遍历失败）。 */
    public static native int nativeCountInstances(String className);

    /** 开关字段监视（盯 removalReason 与 dead）。开启后对<b>被封禁</b>的实体会当场写回安全值。 */
    public static native boolean nativeEnableFieldWatch(boolean enable);

    /**
     * 封禁/解封一个实体（一般传玩家）。
     *
     * <p>封禁后由 JVMTI 的 {@code FieldModification} 回调在<b>字段被写入的瞬间</b>把
     * {@code removalReason} 还原成 null、{@code dead} 还原成 false ——
     * 因此任何 Java 代码 / 任何 mod 的 {@code remove}/{@code setRemoved}/{@code die} 全部失效。
     *
     * <p>这一层在 Java 之下，只有裸内存写入才能绕过它。
     * <b>前提：</b>必须先调用 {@code nativeEnableFieldWatch(true)}（否则没有回调）。
     */
    public static native boolean nativeSealPlayer(Object player, boolean seal);

    /** 当前被封禁的实体数量（诊断用，应等于穿满甲的玩家数，不会无限增长）。 */
    public static native int nativeSealedCount();

    // ---------- 原生内存直写（Windows API，最底层）----------
    /**
     * 取某个<b>基本类型字段</b>的绝对地址（0 = 没找到 / 是引用字段）。
     *
     * <p>原理：HotSpot 的 {@code jfieldID} 对实例字段返回的就是字节偏移，
     * 所以字段地址 = 对象引用 + 偏移。拿到地址后可以用
     * {@link #nativeRawWriteLong(long, long, int)} 直接改内存 ——
     * 不经过 JNI、不经过反射、不需要 safepoint，没有任何拦截点。
     *
     * <p>⚠️ 只对基本类型（Z/B/S/C/I/J/F/D）成立：64 位 JVM 默认压缩指针，
     * 引用字段只有 4 字节，直写 8 字节会崩，因此本方法对引用字段返回 0。
     */
    public static native long nativeFieldAddress(Object target, String fieldName);

    /** 按绝对地址读内存（size = 1/2/4/8），失败返回 0。 */
    public static native long nativeRawReadLong(long address, int size);

    /** 按绝对地址写内存（size = 1/2/4/8），返回是否成功。 */
    public static native boolean nativeRawWriteLong(long address, long value, int size);

    /**
     * 一步到位：按字段名直写基本类型字段（自动按类型取宽度）。
     *
     * <p>float/double 要传<b>位模式</b>：
     * {@code Float.floatToRawIntBits(0.0f)} / {@code Double.doubleToRawLongBits(...)}。
     *
     * <p>典型用法（最底层的击杀，连 JNI 都不经过）：
     * <pre>LiuCore.nativeRawWriteField(entity, "dead", 1);</pre>
     *
     * @return false = 字段不存在 / 是引用类型 / 内存写入失败
     */
    public static native boolean nativeRawWriteField(Object target, String fieldName, long value);
}
