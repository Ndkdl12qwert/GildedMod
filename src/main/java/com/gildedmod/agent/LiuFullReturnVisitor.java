package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * <b>全返回</b>：把目标类里【所有方法】的方法体替换成"直接返回默认值"。
 *
 * <h3>和 {@link LiuAttackStubVisitor} 的区别</h3>
 * <ul>
 *     <li>AttackStub：只拦"攻击型"方法，用在所有 mod 类上 ✓</li>
 *     <li>FullReturn：<b>整个类的每个方法</b>都返回默认值 ✓ 用在指定类上 ✓</li>
 * </ul>
 *
 * <h3>构造器 / 静态初始化保留</h3>
 * {@code <init>} / {@code <clinit>} 一律不动 ✓ 否则 new 不出来 / 静态字段全空 ✗
 *
 * <h3>返回默认值规则</h3>
 * <pre>
 *   void            -> return
 *   boolean/byte/char/short/int -> iconst_0; ireturn
 *   long            -> lconst_0; lreturn
 *   float           -> fconst_0; freturn
 *   double          -> dconst_0; dreturn
 *   对象/数组       -> aconst_null; areturn
 * </pre>
 */
public class LiuFullReturnVisitor extends ClassVisitor {

    /**
     * 全返回时【必须保留】的方法：移除相关。
     *
     * <p>为什么：如果这几个也被替换成空方法，
     * discard() / remove() 内部调 setRemoved 就成了空操作
     * → 实体永远无法从世界里移除 → 变成永不消失的雕像 ✗
     *
     * <p>保留之后：类里其它方法全废（不能 tick / 复活 / 复制 / 攻击），
     * 但"被移除"这条路径畅通 → discard() 真的能摘掉它 ✓
     */
    private static final String[] KEEP_METHODS = {
            "m_142467_", "m_142687_", "m_213877_",   // setRemoved / remove / isRemoved
            "m_146870_",                                // discard（有的类会 override）
            "m_142687_", "m_142045_",
    };

    private int stubbed;

    public LiuFullReturnVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    public int getStubbed() {
        return stubbed;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals("<init>") || name.equals("<clinit>")
                || (access & Opcodes.ACC_ABSTRACT) != 0
                || (access & Opcodes.ACC_NATIVE) != 0) {
            return original;
        }

        // ★ 移除相关方法必须保留（否则实体永远无法被移除 ✗ 见 KEEP_METHODS 注释）
        for (String keep : KEEP_METHODS) {
            if (keep.equals(name)) {
                return original;
            }
        }

        stubbed++;

        final Type ret = Type.getReturnType(descriptor);

        return new MethodVisitor(Opcodes.ASM9) {
            private boolean emitted = false;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                original.visitCode();
                switch (ret.getSort()) {
                    case Type.VOID:
                        original.visitInsn(Opcodes.RETURN);
                        original.visitMaxs(0, 8);
                        break;
                    case Type.LONG:
                        original.visitInsn(Opcodes.LCONST_0);
                        original.visitInsn(Opcodes.LRETURN);
                        original.visitMaxs(2, 8);
                        break;
                    case Type.FLOAT:
                        original.visitInsn(Opcodes.FCONST_0);
                        original.visitInsn(Opcodes.FRETURN);
                        original.visitMaxs(1, 8);
                        break;
                    case Type.DOUBLE:
                        original.visitInsn(Opcodes.DCONST_0);
                        original.visitInsn(Opcodes.DRETURN);
                        original.visitMaxs(2, 8);
                        break;
                    case Type.OBJECT:
                    case Type.ARRAY:
                        original.visitInsn(Opcodes.ACONST_NULL);
                        original.visitInsn(Opcodes.ARETURN);
                        original.visitMaxs(1, 8);
                        break;
                    default:
                        original.visitInsn(Opcodes.ICONST_0);
                        original.visitInsn(Opcodes.IRETURN);
                        original.visitMaxs(1, 8);
                        break;
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
}
