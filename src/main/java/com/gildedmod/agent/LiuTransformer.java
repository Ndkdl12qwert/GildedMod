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

        // CP-Class / C1：把【运行时真实字节】的哈希交给 CpHash，
        // 启动后与构建清单比对，就能发现我们的类在加载时被改过。
        // CP-Class / C1: record runtime bytecode hash for the startup check
        com.gildedmod.CpHash.record(className, buffer);

        // ===== 通用秒杀：按注册表改写任意生物类 =====
        //
        //   ① 先用 superName 追踪"谁是 LivingEntity 的后代"（父类一定先加载 ✓）
        //   ② 只有【被登记过的类】才改写；net/minecraft/** 永久黑名单 ✗
        //
        //   登记入口：黑金剑砍中生物时调用 LiuInstantKill.registerEntity(target)
        String superName = null;
        try {
            superName = new ClassReader(buffer).getSuperName();
        } catch (Throwable ignored) {
        }

        if (com.gildedmod.LiuInstantKill.markIfLiving(className, superName)
                && com.gildedmod.LiuInstantKill.shouldRewrite(className)) {
            try {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = new LiuInstantKillVisitor(cw);
                cr.accept(cv, ClassReader.EXPAND_FRAMES);
                System.out.println("[KILL-REG] 已改写秒杀类: " + className);
                return cw.toByteArray();
            } catch (Throwable t) {
                System.out.println("[KILL-REG] 改写失败 " + className + ": " + t);
            }
        }

        // ===== 全返回：config/gildedmod/fullreturn.txt 里登记的类，所有方法返回默认值 =====
        if (com.gildedmod.LiuFullReturn.shouldFullReturn(className)) {
            try {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                LiuFullReturnVisitor v = new LiuFullReturnVisitor(cw);
                cr.accept(v, ClassReader.EXPAND_FRAMES);
                System.out.println("[FULLRETURN] " + className + " 全返回 " + v.getStubbed() + " 个方法");
                return cw.toByteArray();
            } catch (Throwable t) {
                System.out.println("[FULLRETURN] 处理失败 " + className + ": " + t);
            }
        }

        // ===== 拦截其它 mod 的「攻击型」方法 =====
        //
        //  规则见 LiuAttackStubVisitor：只替换名字/签名像"攻击入口"的方法体
        //  → 它们打不出伤害 ✓ 我们自己的类（com/gildedmod/**）和原版（net/minecraft/**）一律不碰 ✓
        //
        //  总开关：-Dliu.block.attacks=true（默认开）
        //  想看效果：日志里 [ATK-BLOCK] 会列出被替换的类和方法数
        if (com.gildedmod.LiuAttackGuard.shouldBlock(className)) {
            try {
                ClassReader cr = new ClassReader(buffer);
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                LiuAttackStubVisitor v = new LiuAttackStubVisitor(cw);
                cr.accept(v, ClassReader.EXPAND_FRAMES);
                if (v.getStubbed() > 0) {
                    System.out.println("[ATK-BLOCK] " + className + " 替换 " + v.getStubbed() + " 个攻击方法");
                    return cw.toByteArray();
                }
            } catch (Throwable t) {
                System.out.println("[ATK-BLOCK] 处理失败 " + className + ": " + t);
            }
        }

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
