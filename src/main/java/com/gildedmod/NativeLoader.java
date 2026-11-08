package com.gildedmod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 原生库加载器：从 <b>jar 内部</b> 取 dll，落盘后再加载。
 *
 * <h3>为什么不能直接从 jar 里加载</h3>
 * {@code System.load} / {@code LoadLibrary} 只接受 <b>真实文件路径</b>，
 * 不能读 zip 里的条目。所以必须先把 dll 解压到磁盘，再按路径加载。
 *
 * <h3>两个 dll 的处境不同</h3>
 * <ul>
 *     <li><b>{@code GildedArmor.dll}</b> —— 由 {@link System#load} 在运行时加载，
 *         所以「从 jar 解压 → 加载」当场生效 ✓</li>
 *     <li><b>{@code GildedNativeKernel.dll}</b> —— 它是 JVMTI agent，由 JVM 用
 *         {@code -agentpath:} 在 <b>JVM 启动之前</b> 读取。
 *         等到 mod 代码跑起来时 JVM 早就启动完了，<b>无法在当次启动生效</b> ✗<br>
 *         所以这里只做「补齐」：（旧做法，已删除），
 *         <b>下一次启动</b> 才由 {@code -agentpath} 用上 ✓
 *         （首次安装时日志会明确提示「下次启动生效」）</li>
 * </ul>
 *
 * <h3>落盘位置</h3>
 * {@code <游戏目录>/config/gildedmod/native/} —— 不用临时目录，原因：
 * 临时目录会被清理、部分安全软件禁止在 %TEMP% 里加载 dll、
 * 且 dll 加载后文件被锁住、{@code deleteOnExit} 根本删不掉（会残留一堆垃圾）。
 */
public final class NativeLoader {

    /** jar 里 dll 的目录 */
    private static final String RES_PREFIX = "/natives/windows/";

    private static final boolean DEBUG = Boolean.getBoolean("gildedmod.native.debug");

    private NativeLoader() {
    }

    // ---------------------------------------------------------------- 路径

    /** 落盘目录：<游戏目录>/config/gildedmod/native */
    public static File nativeDir() {
        File dir = new File("config/gildedmod/native");
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            // 极端情况（只读目录）退回临时目录
            dir = new File(System.getProperty("java.io.tmpdir"), "gildedmod/native");
            dir.mkdirs();
        }
        return dir;
    }

    /** mods 目录（JVMTI 的 dll 必须放这里，-agentpath 才能找到） */
    public static File modsDir() {
        return new File("mods");
    }

    // ---------------------------------------------------------------- 解压

    /**
     * 把 jar 里的 dll 解压到 {@link #nativeDir()}。
     *
     * <p>已经存在且大小一致就跳过（省 IO）；大小不同说明是新版本 → 覆盖。
     * 注意：如果这个 dll <b>正在被当前进程使用</b>，Windows 会拒绝覆盖 ✗ ——
     * 这时我们写到一个带后缀的新文件名里，保证不失败。
     *
     * @param name 例如 {@code GildedArmor.dll}
     * @return 落盘后的文件；jar 里没有这个资源时返回 null
     */
    public static File extract(String name) {
        InputStream in = NativeLoader.class.getResourceAsStream(RES_PREFIX + name);
        if (in == null) {
            if (DEBUG) {
                System.out.println("[NATIVE] jar 里没有 " + RES_PREFIX + name);
            }
            return null;
        }

        File target = new File(nativeDir(), name);
        try (InputStream stream = in) {
            long resourceSize = stream.available();

            if (target.isFile() && target.length() == resourceSize) {
                return target;
            }

            File tmp = new File(target.getParentFile(), name + ".tmp");
            Files.copy(stream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException locked) {
                // 目标被占用（旧 dll 已被加载）→ 换个名字，保证能加载到新版本
                target = new File(target.getParentFile(), name + "." + System.nanoTime() / 1000L + ".dll");
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[NATIVE] 旧文件被占用，改用新副本: " + target.getName());
            }

            if (DEBUG) {
                System.out.println("[NATIVE] 已从 jar 解压 " + name + " → " + target.getAbsolutePath()
                        + " (" + target.length() + " 字节)");
            }
            return target;
        } catch (Throwable t) {
            System.out.println("[NATIVE] 解压失败 " + name + ": " + t);
            return null;
        }
    }

    // ---------------------------------------------------------------- 加载

    /**
     * 加载 dll：优先用 {@code mods/} 里的（兼容老部署），
     * 没有就从 jar 解压一份再加载。
     *
     * @return true = 加载成功
     */
    public static boolean load(String name) {
        // ① 先看 mods/（老部署，或用户手动放的调试版本）
        File inMods = new File(modsDir(), name);
        if (inMods.isFile()) {
            try {
                System.load(inMods.getAbsolutePath());
                System.out.println("[NATIVE] " + name + " ← mods/ 加载成功");
                return true;
            } catch (Throwable t) {
                System.out.println("[NATIVE] mods/" + name + " 加载失败，改从 jar 解压: " + t);
            }
        }

        // ② 从 jar 解压后加载
        File extracted = extract(name);
        if (extracted == null) {
            System.out.println("[NATIVE] 找不到 " + name + "（mods/ 和 jar 里都没有）");
            return false;
        }
        try {
            System.load(extracted.getAbsolutePath());
            System.out.println("[NATIVE] " + name + " ← jar 加载成功（" + extracted.getAbsolutePath() + "）");
            return true;
        } catch (Throwable t) {
            System.out.println("[NATIVE] " + name + " 加载失败: " + t);
            t.printStackTrace();
            return false;
        }
    }

}
