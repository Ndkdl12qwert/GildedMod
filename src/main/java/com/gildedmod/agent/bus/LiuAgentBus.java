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
package com.gildedmod.agent.bus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiuAgentBus {

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
    public static boolean isJvmtiAvailable() {
        return jvmtiLoaded;
    }
    // ============ DLL 加载 ============
    private static boolean jvmtiLoaded = false;

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
            jvmtiLoaded = true;
            System.out.println("[LIU-BUS] JVMTI DLL already loaded (via -agentpath)");
        } catch (Throwable t) {
            System.err.println("[LIU-BUS] JVMTI load failed: " + t);
        }
    }
}