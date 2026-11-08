package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 把指定类里的某些方法体替换成"直通父类"。
 *
 * <h3>为什么要这个</h3>
 * pig2mod 的 Pig2 把两个方法改成了谎话：
 * <pre>
 *   getHealth()      → isEnding() ? super.getHealth() : 10.0f   // entityData 是 0 也报 10
 *   setHealth(float) → super.setHealth(10.0f)                   // 参数被丢弃
 * </pre>
 * 结果：我们把它的血量字段清零了，它自己却"不知道" —— {@code LivingEntity.tick}
 * 里的 {@code getHealth() <= 0} 永远为 false → 永远进不了死亡流程 ✗
 *
 * <p>这个 visitor 把这两个方法体换成：
 * <pre>
 *   getHealth()      → return super.getHealth();
 *   setHealth(float) → super.setHealth(arg);      // 参数照传
 * </pre>
 * 改完之后 {@code getHealth()} 就等于 entityData 里的真值（0）✓
 * → {@code isDeadOrDying()} 为 true ✓ → 它自己走死亡流程 ✓
 */
public class LiuPassthroughVisitor extends ClassVisitor {

    private final String[] names;      // 要替换的方法名
    private final String superOwner;   // 父类内部名，如 net/minecraft/world/entity/LivingEntity

    public LiuPassthroughVisitor(ClassVisitor cv, String superOwner, String... names) {
        super(Opcodes.ASM9, cv);
        this.superOwner = superOwner;
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        for (String target : names) {
            if (!name.equals(target)) {
                continue;
            }
            final boolean returnsFloat = descriptor.endsWith(")F");
            final boolean takesFloat = descriptor.startsWith("(F)");
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean emitted = false;

                @Override
                public void visitCode() {
                    if (emitted) {
                        return;
                    }
                    emitted = true;
                    original.visitCode();
                    original.visitVarInsn(Opcodes.ALOAD, 0);          // this
                    if (takesFloat) {
                        original.visitVarInsn(Opcodes.FLOAD, 1);      // 第一个 float 参数
                    }
                    original.visitMethodInsn(Opcodes.INVOKESPECIAL, superOwner, name,
                            descriptor, false);
                    if (returnsFloat) {
                        original.visitInsn(Opcodes.FRETURN);
                        original.visitMaxs(2, 2);
                    } else {
                        original.visitInsn(Opcodes.RETURN);
                        original.visitMaxs(2, 2);
                    }
                    original.visitEnd();
                }

                @Override
                public void visitEnd() {
                    if (!emitted) {
                        original.visitEnd();
                    }
                }
            };
        }
        return original;
    }
}
