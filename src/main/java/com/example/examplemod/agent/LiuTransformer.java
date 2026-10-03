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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class LiuTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] buffer) {
        if (className == null) return null;

        try {
            if (className.equals("net/minecraft/world/entity/LivingEntity")) {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = new LiuLivingEntityVisitor(cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                System.out.println("[LIU-AGENT] transformed LivingEntity");
                return cw.toByteArray();
            }

            if (className.equals("net/minecraft/world/entity/player/Player")) {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = new LiuPlayerVisitor(cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                System.out.println("[LIU-AGENT] transformed Player");
                return cw.toByteArray();
            }

            if (className.equals("net/minecraft/server/network/ServerGamePacketListenerImpl")) {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = new LiuServerVisitor(cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                System.out.println("[LIU-AGENT] transformed ServerGamePacketListenerImpl");
                return cw.toByteArray();
            }

            if (className.equals("net/minecraft/world/entity/LivingEntity")) {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = new LiuLivingEntityVisitor(cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                return cw.toByteArray();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}