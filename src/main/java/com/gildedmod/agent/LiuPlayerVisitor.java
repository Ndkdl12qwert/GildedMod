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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LiuPlayerVisitor extends ClassVisitor {

    public LiuPlayerVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // 拦截 Player.attack(Entity)V
        if ((name.equals("attack") || name.equals("m_5706_")) && descriptor.equals("(Lnet/minecraft/world/entity/Entity;)V")) {
            System.out.println("[LIU-AGENT] hooking Player.attack");
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitCode() {
                    // 方法开头：LiuAgentHook.onPlayerAttack(this, target)
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            "com/gildedmod/agent/LiuAgentHook",
                            "onPlayerAttack",
                            "(Ljava/lang/Object;Ljava/lang/Object;)V",
                            false);
                    super.visitCode();
                }
            };
        }

        return mv;
    }
}