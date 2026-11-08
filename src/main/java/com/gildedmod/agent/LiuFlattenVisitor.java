package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * 把目标类<b>自己声明的每一个方法体都清空</b>，只返回该类型的默认值。
 *
 * <h3>为什么不用"逐个挑方法挖"</h3>
 * 之前挑着挖 {@code myRevive / revive / startJisatsu} → 它发现"复活失败"，
 * 于是走另一条补偿路径：{@code copy()} + {@code addFreshEntity(自己)} ✗
 * → 同一个实体被反复加回世界 → 每帧 {@code PersistentEntitySectionManager} 警告
 * → 线程耗尽 + OOM ✗（实测过）
 *
 * <p>它的状态机是<b>互相兜底</b>的：挖一个洞，别的分支会顶上。所以要挖就全挖 ✓
 *
 * <h3>清空之后它会变成什么</h3>
 * <ul>
 *     <li>{@code tick} 空 → 不动、不 AI、不寻路 ✓</li>
 *     <li>{@code getHealth} 返回 0f → 血量恒 0 ✓</li>
 *     <li>{@code setHealth} 空 → 谁也改不了 ✓</li>
 *     <li>{@code hurt} 返回 false → 免疫 ✓</li>
 *     <li>{@code copy / revive / startJisatsu} 空 → 不能复制、不能复活、不能踢人 ✓</li>
 *     <li>构造器【保持原样】✓ → 它仍会注册进它的静态集合（那是数据，无害）</li>
 * </ul>
 * 结果：一个<b>什么都不做的雕像</b> ✓ —— 这是我们能做到的最强形态。
 *
 * <p>⚠ 不碰 {@code <init>} / {@code <clinit>}：构造逻辑必须保留，否则实例化直接失败。
 */
public class LiuFlattenVisitor extends ClassVisitor {

    public LiuFlattenVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        // ★ 白名单：这三个决定实体能不能被移除，必须保留原版逻辑。
        //   全挖空时它们也被清空了 → remove() 从未登记 → 实体永远留在实体表里 ✗
        //   （实测：模型没了、但 Forge 实体列表里还在）
        //   m_8119_（tick）也保留：挖空它会让 xOld 停住 → 模型画在原点 → 看不见 ✗
        //   它的子方法（myRevive/revive/copy/tickLevel*）是空的 ✓ 所以 tick 只是走流程 ✓
        if (name.equals("m_142467_") || name.equals("m_142687_")
                || name.equals("m_213877_") || name.equals("m_8119_")) {
            return original;
        }

        // 构造器 / 静态初始化 / 抽象 / native：一律不动
        if (name.equals("<init>") || name.equals("<clinit>")
                || (access & Opcodes.ACC_ABSTRACT) != 0
                || (access & Opcodes.ACC_NATIVE) != 0) {
            return original;
        }

        // ★ static 一律不动：实体注册的工厂（m_29503_ 返回 AttributeSupplier.Builder）
        //   和别处的静态入口都在这一类，返回 null 会让注册阶段直接崩 ✗
        if ((access & Opcodes.ACC_STATIC) != 0) {
            return original;
        }

        final Type returnType = Type.getReturnType(descriptor);

        // ★ 返回【对象/数组】的也不动：
        //   这些是 Mob 的契约（createNavigation / createLookControl / createMoveControl /
        //   createPathNavigator / createBrain / getSensing …），Mob 构造器就会调用它们，
        //   返回 null 会立刻 NPE ✗（上一次崩溃就是这个原因）
        int sort = returnType.getSort();
        if (sort == Type.OBJECT || sort == Type.ARRAY) {
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

                switch (returnType.getSort()) {
                    case Type.VOID:
                        original.visitInsn(Opcodes.RETURN);
                        break;
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        original.visitInsn(Opcodes.ICONST_0);
                        original.visitInsn(Opcodes.IRETURN);
                        break;
                    case Type.LONG:
                        original.visitInsn(Opcodes.LCONST_0);
                        original.visitInsn(Opcodes.LRETURN);
                        break;
                    case Type.FLOAT:
                        original.visitInsn(Opcodes.FCONST_0);
                        original.visitInsn(Opcodes.FRETURN);
                        break;
                    case Type.DOUBLE:
                        original.visitInsn(Opcodes.DCONST_0);
                        original.visitInsn(Opcodes.DRETURN);
                        break;
                    default:
                        original.visitInsn(Opcodes.ACONST_NULL);
                        original.visitInsn(Opcodes.ARETURN);
                        break;
                }
                original.visitMaxs(2, 16);
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
