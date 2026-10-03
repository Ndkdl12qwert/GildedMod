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
package com.example.examplemod;

public class LiuArmorCore {

    static {
        try {
            String libName = "liu_armor_core.dll";
            System.out.println("========== LiuArmorCore 静态块开始 ==========");
            java.io.InputStream in = LiuArmorCore.class.getResourceAsStream("/natives/windows/" + libName);
            if (in == null) throw new RuntimeException("找不到 " + libName);
            java.io.File temp = java.io.File.createTempFile("liu_armor_core", ".dll");
            temp.deleteOnExit();
            java.nio.file.Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.getAbsolutePath());
            System.out.println("========== LiuArmorCore: DLL 加载成功 ==========");
        } catch (Throwable e) {
            System.err.println("========== LiuArmorCore: DLL 加载失败 ==========");
            e.printStackTrace();
        }
    }

    // 只传数值，不传 player
    public static native boolean nativeGuardSetHealth(float currentHealth, float newHealth);
    public static native boolean nativeGuardRemove();
    public static native boolean nativeGuardDie();

    // 清 debuff 直接用 Java，不走 native
    public static void clearDebuffs(net.minecraft.world.entity.player.Player player) {
        player.clearFire();
        player.removeAllEffects();
    }
}