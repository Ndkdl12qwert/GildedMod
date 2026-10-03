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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class LiuLivingEntityVisitor extends ClassVisitor {

    public LiuLivingEntityVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // ===== getHealth()F =====
        if ((name.equals("getHealth") || name.equals("m_21223_"))
                && descriptor.equals("()F")) {
            System.out.println("[LIU-AGENT] hooking getHealth (name=" + name + ")");
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC,
                            "com/example/examplemod/agent/LiuAgentHook",
                            "isDoomed",
                            "(Ljava/lang/Object;)Z",
                            false);
                    Label normal = new Label();
                    mv.visitJumpInsn(IFEQ, normal);
                    mv.visitInsn(FCONST_0);
                    mv.visitInsn(FRETURN);
                    mv.visitLabel(normal);
                }
            };
        }

        // ===== hurt(DamageSource;F)Z =====
        if ((name.equals("hurt") || name.equals("m_6469_"))
                && descriptor.equals("(Lnet/minecraft/world/damagesource/DamageSource;F)Z")) {
            System.out.println("[LIU-AGENT] hooking LivingEntity.hurt (name=" + name + ")");
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    // if (LiuArmorHook.shouldBlockHurt(this)) return false;
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC,
                            "com/example/examplemod/agent/LiuArmorHook",
                            "shouldBlockHurt",
                            "(Ljava/lang/Object;)Z",
                            false);
                    Label normal = new Label();
                    mv.visitJumpInsn(IFEQ, normal);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(IRETURN);
                    mv.visitLabel(normal);
                }
            };
        }

        // ===== setHealth(F)V =====
        if ((name.equals("setHealth") || name.equals("m_21153_"))
                && descriptor.equals("(F)V")) {
            System.out.println("[LIU-AGENT] hooking LivingEntity.setHealth (name=" + name + ")");
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    // if (LiuArmorHook.shouldBlockSetHealth(this, health)) return;
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(FLOAD, 1);
                    mv.visitMethodInsn(INVOKESTATIC,
                            "com/example/examplemod/agent/LiuArmorHook",
                            "shouldBlockSetHealth",
                            "(Ljava/lang/Object;F)Z",
                            false);
                    Label normal = new Label();
                    mv.visitJumpInsn(IFEQ, normal);
                    mv.visitInsn(RETURN);
                    mv.visitLabel(normal);
                }
            };
        }

        // ===== die(DamageSource)V =====
        if ((name.equals("die") || name.equals("m_6667_"))
                && descriptor.equals("(Lnet/minecraft/world/damagesource/DamageSource;)V")) {
            System.out.println("[LIU-AGENT] hooking LivingEntity.die (name=" + name + ")");
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC,
                            "com/example/examplemod/agent/LiuArmorHook",
                            "shouldBlockDie",
                            "(Ljava/lang/Object;)Z",
                            false);
                    Label normal = new Label();
                    mv.visitJumpInsn(IFEQ, normal);
                    mv.visitInsn(RETURN);
                    mv.visitLabel(normal);
                }
            };
        }

        // ===== remove(RemovalReason)V =====
        if ((name.equals("remove") || name.equals("m_142687_"))
                && descriptor.equals("(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")) {
            System.out.println("[LIU-AGENT] hooking Entity.remove (name=" + name + ")");
            return new AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                @Override
                protected void onMethodEnter() {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKESTATIC,
                            "com/example/examplemod/agent/LiuArmorHook",
                            "shouldBlockRemove",
                            "(Ljava/lang/Object;)Z",
                            false);
                    Label normal = new Label();
                    mv.visitJumpInsn(IFEQ, normal);
                    mv.visitInsn(RETURN);
                    mv.visitLabel(normal);
                }
            };
        }

        return mv;
    }
}