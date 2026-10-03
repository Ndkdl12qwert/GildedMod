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
package com.example.examplemod.agent;

import com.sun.tools.attach.VirtualMachine;

public class LiuAgentLoader {

    private static boolean attached = false;

    public static void attach() {
        if (attached) return;
        attached = true;

        try {
            String pid = String.valueOf(ProcessHandle.current().pid());

            // ✅ 优先读系统属性
            String agentPath = System.getProperty("liu.agent.path");
            java.io.File jarFile;
            if (agentPath != null && !agentPath.isBlank()) {
                jarFile = new java.io.File(agentPath);
            } else {
                jarFile = new java.io.File("D:/IDEA/MyMode/forge-1.20.1-47.4.13-mdk/build/libs/blackgoldmod-1.0-agent.jar");
            }

            System.out.println("[LIU-AGENT] attaching to pid " + pid);
            System.out.println("[LIU-AGENT] agent path: " + jarFile.getAbsolutePath());
            System.out.println("[LIU-AGENT] agent exists: " + jarFile.isFile());

            if (!jarFile.isFile()) {
                System.out.println("[LIU-AGENT] agent jar not found, skipping");
                return;
            }

            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(jarFile.getAbsolutePath(), "");
            vm.detach();
            System.out.println("[LIU-AGENT] attached successfully");
        } catch (Throwable t) {
            System.err.println("[LIU-AGENT] attach failed");
            t.printStackTrace();
        }
    }
}