package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 把指定名字的 {@code void} 方法体<b>清空</b>（直接 {@code return}）。
 *
 * <h3>用途</h3>
 * 有些"复活 / 自毁"方法不能改成直通父类（父类里根本没有这个方法），
 * 但可以让它<b>什么都不做</b>：
 * <pre>
 *   Pig2.myRevive()        // 每 tick: setHealth(10) + revive() + deathTime = 0
 *   Pig2.revive()          // 真复活
 *   Pig2.startJisatsu()    // 自毁 / 踢人链
 *   MyHelper.reviveInChunk(Pig2)   // 另一个复活入口
 * </pre>
 *
 * <p>清空之后，`LivingEntity.tick` 里的死亡推进（{@code deathTime++} →
 * {@code remove(KILLED)}）才有可能走完 ✓
 */
public class LiuEmptyVoidVisitor extends ClassVisitor {

    private final String[] names;

    public LiuEmptyVoidVisitor(ClassVisitor cv, String... names) {
        super(Opcodes.ASM9, cv);
        this.names = names;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        boolean match = descriptor.endsWith(")V");
        if (match) {
            match = false;
            for (String t : names) {
                if (name.equals(t)) {
                    match = true;
                    break;
                }
            }
        }
        if (!match) {
            return original;
        }

        return new MethodVisitor(Opcodes.ASM9) {
            private boolean emitted = false;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                original.visitCode();
                original.visitInsn(Opcodes.RETURN);
                original.visitMaxs(0, 8);
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
}
