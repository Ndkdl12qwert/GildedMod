package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 把指定类里某些 {@code boolean} 方法体<b>整体替换成 {@code return false}</b>。
 *
 * <h3>用途</h3>
 * 对抗型 mod 拒绝"别的 mod 调它"的判定集中在极少数方法里：
 * <pre>
 *   MyLib2.isCalledFromOtherModWithin1()   // 取栈帧第 3 层判断
 *   MyLib2.isCalledFromOtherModWithin5()   // 取第 3~7 层
 *   MyLib2.isCalledFromOtherModWithin10()
 * </pre>
 * 全 jar 有 <b>101 处</b>调用它们（setRemoved / remove / hurt / startJisatsu /
 * permitItemSwing / preventForgeEventAttack / onEntityJoinWorld /
 * replaceRendererOfOtherMOD …）。
 *
 * <p>把这几个方法改成恒 {@code false}，那 101 处守卫<b>同时失效</b> ——
 * 相当于把它的"防别的 mod"开关整个关掉，而不是逐个去绕。
 *
 * <h3>为什么在加载期改，而不是 retransform</h3>
 * retransform 会从原始字节重建、把 Mixin 注入一起剥掉（我们崩过三次）；
 * 而在 {@code ClassFileLoadHook} 里改，是"加载时决定它的样子"，不会二次重定义 ✗
 */
public class LiuStubFalseVisitor extends ClassVisitor {

    private final String prefix;

    public LiuStubFalseVisitor(ClassVisitor cv, String methodPrefix) {
        super(Opcodes.ASM9, cv);
        this.prefix = methodPrefix;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        // 只处理返回 boolean 的、且名字符合前缀的方法
        if (name.startsWith(prefix) && descriptor.endsWith(")Z")) {
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean emitted = false;

                @Override
                public void visitCode() {
                    if (emitted) {
                        return;
                    }
                    emitted = true;
                    original.visitCode();
                    original.visitInsn(Opcodes.ICONST_0);   // false
                    original.visitInsn(Opcodes.IRETURN);
                    original.visitMaxs(1, Math.max(8, 1));
                    original.visitEnd();
                }

                // 其余指令全部丢弃（方法体已被替换）
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
