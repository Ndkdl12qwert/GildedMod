package com.gildedmod;

public class LiuCore {
    // 初始化
    public static native void nativeInitJVMTI();

    // 秒杀
    public static native boolean nativeKillJVMTI(Object living);
    public static native int nativeAbsoluteKill(Object target);   // ★
    public static native boolean nativeSetHealthJVMTI(Object living, float health);
    public static native boolean nativeMarkRemovedJVMTI(Object entity);

    // 侦察
    public static native boolean nativeEnableFieldWatch(boolean enable);
    public static native void nativeLogDump(String msg);

    // 反制
    public static native int nativeSuspendThreadsByName(String nameFilter);
    public static native int nativeCountInstances(String className);
}