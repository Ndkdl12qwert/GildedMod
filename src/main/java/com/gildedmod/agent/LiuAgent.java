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
package com.gildedmod.agent;

import java.lang.instrument.Instrumentation;

public class LiuAgent {

    public static void premain(String args, Instrumentation inst) {
        System.out.println("[LIU-AGENT] premain called");
        inst.addTransformer(new LiuTransformer(), true);
    }

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[LIU-AGENT] agentmain called");
        inst.addTransformer(new LiuTransformer(), true);

        try {
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                String name = clazz.getName();
                // ✅ 三件套都 retransform，否则 Player.attack / handleInteract 的 hook 不生效
                if (name.equals("net.minecraft.world.entity.LivingEntity")
                        || name.equals("net.minecraft.world.entity.player.Player")
                        || name.equals("net.minecraft.server.network.ServerGamePacketListenerImpl")) {
                    System.out.println("[LIU-AGENT] retransforming " + name);
                    inst.retransformClasses(clazz);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}