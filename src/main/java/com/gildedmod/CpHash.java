package com.gildedmod;

import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CP-Class / C1 的数据源：记录 agent 在 {@code ClassFileLoadHook} 里
 * <b>实际看到</b>的类字节哈希。
 *
 * <h3>为什么必须由 agent 记</h3>
 * {@code Class.getResourceAsStream} 读到的是 <b>jar 里</b>的字节 ✗，
 * 而别人对我们的篡改发生在<b>加载时</b>（transform）✗ —— jar 里那份是干净的 ✓，
 * 所以只有 transformer 看到的字节才是"运行时真身" ✓
 *
 * <p>本类会被 agent 的 classloader 加载，所以不能依赖任何 Forge / MC 的类。
 */
public final class CpHash {

    /** 类名（斜杠形式，如 com/gildedmod/LiuCore）→ SHA-256 十六进制 */
    private static final Map<String, String> RUNTIME = new ConcurrentHashMap<>();

    private CpHash() {
    }

    /** agent 的 transformer 每见到一个我方类就调一次 */
    public static void record(String className, byte[] bytes) {
        if (className == null || bytes == null) {
            return;
        }
        if (!className.startsWith("com/gildedmod/")) {
            return;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            RUNTIME.put(className, sb.toString());
            writeRuntimeLine(className, sb.toString());
        } catch (Throwable ignored) {
            // 哈希失败绝不影响类加载
        }
    }

    public static int size() {
        return RUNTIME.size();
    }

    public static Map<String, String> snapshot() {
        return RUNTIME;
    }

    // ---------------------------------------------------------------- 跨 classloader 桥接
    //
    // agent 的类由 agent classloader 加载，mod 的类由 Forge 的 mod classloader 加载 ——
    // 两边同名类【不是同一个类】，RUNTIME 静态表互相看不见 ✗（本项目踩过同一个坑）。
    // 所以额外落一个文件，mod 侧读文件即可 ✓
    private static java.io.Writer gWriter;
    private static final Object G_LOCK = new Object();

    private static void writeRuntimeLine(String className, String hex) {
        synchronized (G_LOCK) {
            try {
                if (gWriter == null) {
                    java.io.File f = new java.io.File("config/gildedmod/cp-runtime.txt");
                    java.io.File dir = f.getParentFile();
                    if (dir != null) {
                        dir.mkdirs();
                    }
                    gWriter = new java.io.BufferedWriter(new java.io.FileWriter(f, false));
                }
                gWriter.write(className + "=" + hex + "\n");
                gWriter.flush();
            } catch (Throwable ignored) {
                // 写不了就算了，绝不影响类加载
            }
        }
    }
}
