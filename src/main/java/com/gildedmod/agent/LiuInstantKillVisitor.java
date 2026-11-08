package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 通用「秒杀改写」：把任意生物类的 <b>getHealth 改成恒 0</b>、
 * <b>tick 改成直通父类</b>。
 *
 * <h3>效果</h3>
 * <pre>
 *   getHealth() → 0.0f          → isDeadOrDying() 永远 true
 *   tick()      → super.tick()  → 原版死亡推进 deathTime++ → remove(KILLED)
 * </pre>
 * → 该类生物<b>出生后 1 秒内死亡</b> ✓
 *
 * <h3>为什么不是所有类都能这么改</h3>
 * <ul>
 *     <li>{@code net/minecraft/**} 核心类（LivingEntity / Player / Entity）：
 *         retransform 会剥掉 Mixin 注入 → 直接崩 ✗ <b>永久黑名单</b></li>
 *     <li>玩家自己也是 LivingEntity 子类 → 若被改写，你也会瞬间死亡 ✗
 *         → 所以只对「你亲手标记过的生物类」生效</li>
 * </ul>
 */
public class LiuInstantKillVisitor extends ClassVisitor {

    private static final String SUPER_OWNER = "net/minecraft/world/entity/LivingEntity";

    public LiuInstantKillVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        // getHealth()F → 0.0f
        if (name.equals("m_21223_") && descriptor.equals("()F")) {
            return replace(original, new int[]{ Opcodes.FCONST_0, Opcodes.FRETURN }, 1);
        }

        // tick()V → super.tick()
        if (name.equals("m_8119_") && descriptor.equals("()V")) {
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean emitted = false;

                @Override
                public void visitCode() {
                    if (emitted) return;
                    emitted = true;
                    original.visitCode();
                    original.visitVarInsn(Opcodes.ALOAD, 0);
                    original.visitMethodInsn(Opcodes.INVOKESPECIAL, SUPER_OWNER,
                            "m_8119_", "()V", false);
                    original.visitInsn(Opcodes.RETURN);
                    original.visitMaxs(1, 1);
                    original.visitEnd();
                }

                @Override
                public void visitEnd() {
                    if (!emitted) original.visitEnd();
                }
            };
        }

        return original;
    }

    private static MethodVisitor replace(MethodVisitor original, int[] opcodes, int maxs) {
        return new MethodVisitor(Opcodes.ASM9) {
            private boolean emitted = false;

            @Override
            public void visitCode() {
                if (emitted) return;
                emitted = true;
                original.visitCode();
                for (int op : opcodes) {
                    original.visitInsn(op);
                }
                original.visitMaxs(maxs, 1);
                original.visitEnd();
            }

            @Override
            public void visitEnd() {
                if (!emitted) original.visitEnd();
            }
        };
    }
}
