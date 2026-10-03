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
package com.example.examplemod.agent.bus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 双 Agent 通信总线
 *
 * 作用：
 *   1. Java Agent ↔ JVMTI Agent 通过这个类共享状态
 *   2. 提供 JVMTI 相关的 native 方法声明
 *   3. 提供 DLL 加载入口
 *
 * 注意：
 *   - 所有方法都是 static，方便从任意地方调用
 *   - 存储用 ConcurrentHashMap，线程安全
 */
public class LiuAgentBus {

    // ============ 共享状态 ============
    public static native String jvmtiDumpNativeBinds();
    private static final Map<String, Object> STORE = new ConcurrentHashMap<>();

    public static void put(String key, Object value) {
        if (key == null) return;
        if (value == null) {
            STORE.remove(key);
        } else {
            STORE.put(key, value);
        }
    }

    public static Object get(String key) {
        return key == null ? null : STORE.get(key);
    }

    public static String getString(String key, String def) {
        Object v = STORE.get(key);
        return v == null ? def : v.toString();
    }

    public static boolean getBool(String key, boolean def) {
        Object v = STORE.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    public static int getInt(String key, int def) {
        Object v = STORE.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    public static long getLong(String key, long def) {
        Object v = STORE.get(key);
        return v instanceof Number ? ((Number) v).longValue() : def;
    }

    // ============ JVMTI native 方法 ============

    /** 标记"启动阶段结束"——之后 JVMTI 只打印 retransform 的类 */
    public static native void jvmtiEndStartup();

    /** 累计的类加载次数（含重复） */
    public static native long jvmtiTotalCount();

    /** 记录的类名条数 */
    public static native int jvmtiRecordCount();

    /** 导出全部类名（每行一个） */
    public static native String jvmtiDump();

    /** 清空记录 */
    public static native void jvmtiClear();

    // ============ DLL 加载 ============

    private static boolean jvmtiLoaded = false;

    /**
     * 加载 JVMTI DLL
     * - 若已通过 -agentpath 加载过，会抛 UnsatisfiedLinkError，被吞掉
     * - 若路径为空，直接返回
     */
    public static synchronized void loadJvmtiDll(String path) {
        if (jvmtiLoaded) return;
        if (path == null || path.isBlank()) {
            System.out.println("[LIU-BUS] no jvmti dll path, skip");
            return;
        }
        try {
            System.load(path);
            jvmtiLoaded = true;
            System.out.println("[LIU-BUS] JVMTI DLL loaded: " + path);
        } catch (UnsatisfiedLinkError e) {
            // 已被 -agentpath 加载
            jvmtiLoaded = true;
            System.out.println("[LIU-BUS] JVMTI DLL already loaded (via -agentpath)");
        } catch (Throwable t) {
            System.err.println("[LIU-BUS] JVMTI load failed: " + t);
        }
    }

    public static boolean isJvmtiAvailable() {
        try {
            jvmtiTotalCount();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}