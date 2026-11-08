package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * 拦截其它 mod 的"攻击型"方法：把方法体整体替换成 {@code return false} / {@code return}。
 *
 * <h3>只打哪些方法（严格筛选，避免误伤）</h3>
 * <ol>
 *     <li><b>名字命中攻击名单</b>：{@code hurtEnemy / onLeftClickEntity / doHurtTarget /
 *         attack / playerAttack / m_5706_ …}</li>
 *     <li><b>或者</b>：参数里含 {@code Entity}/{@code LivingEntity}，且返回 {@code boolean}/{@code void}</li>
 * </ol>
 *
 * <h3>为什么整体替换而不是插入</h3>
 * 插入要修 max_stack / exception_table / LineNumberTable / <b>StackMapTable 帧</b> ——
 * 错一个字节就是 {@code VerifyError} → 游戏崩 ✗（我们已经踩过 ✓）<br>
 * 整体替换只要：
 * <pre>
 *   Code = { iconst_0, ireturn }   （或 { return }）
 *   max_stack = 1，max_locals 不变
 *   清空 exception_table
 *   删除 StackMapTable / LineNumberTable / LocalVariableTable
 *      （没有分支就不需要帧 ✓ 删除是合法的 ✓）
 * </pre>
 * ASM 的 {@code ClassWriter.COMPUTE_FRAMES} 会帮我们重算 ✓ 所以更稳 ✓
 *
 * <h3>边界</h3>
 * 只由 {@code LiuTransformer} 对"mod 类"调用（见 {@code LiuAttackGuard}）✓
 * 绝不碰 {@code net/minecraft/**}（会被 Mixin 抢改 ✗）和 {@code com/gildedmod/**}（我们自己的 ✓）
 */
public class LiuAttackStubVisitor extends ClassVisitor {

    /** 明确视为"攻击入口"的方法名（SRG + 官方名都列上） */
    private static final String[] ATTACK_NAMES = {
            "hurtEnemy", "onLeftClickEntity", "doHurtTarget", "playerAttack",
            "hurtAndBreak",           // 攻击时耐久（连带拦掉更彻底）
            "m_7579_",                // Item.hurtEnemy
            "m_6240_",                // Item.onLeftClickEntity
            "m_7327_",                // Mob.doHurtTarget
            "m_5706_",                // Player.attack
    };

    /**
     * 永不替换的方法名：渲染 / 可见性相关。
     * <p>2026-11-08 事故：这些方法被替换成 return false 后，所有实体都不渲染 ✗
     */
    private static final String[] NEVER_STUB = {
            "shouldRender", "isVisible", "render", "submit", "extract",
            "isInvisibleTo", "canBeRendered", "getRender", "renderToBuffer",
            "renderNameTag", "getViewDistance", "shouldShowName",
    };

    private int stubbed;

    public LiuAttackStubVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    public int getStubbed() {
        return stubbed;
    }

    private static boolean nameMatches(String name) {
        for (String s : ATTACK_NAMES) {
            if (s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        if ((access & Opcodes.ACC_ABSTRACT) != 0 || (access & Opcodes.ACC_NATIVE) != 0
                || name.startsWith("<")) {
            return original;
        }

        Type ret = Type.getReturnType(descriptor);
        boolean returnsBool = ret.getSort() == Type.BOOLEAN;
        boolean returnsVoid = ret.getSort() == Type.VOID;
        if (!returnsBool && !returnsVoid) {
            return original;
        }

        // 渲染/可见性方法：永不替换（2026-11-08 事故修复）
        for (String never : NEVER_STUB) {
            if (never.equals(name)) {
                return original;
            }
        }

        boolean hit = nameMatches(name);

        if (!hit) {
            // 规则②：参数含 Entity / LivingEntity（宽规则，靠类级+方法级排除兜底）
            for (Type arg : Type.getArgumentTypes(descriptor)) {
                if (arg.getSort() != Type.OBJECT) {
                    continue;
                }
                String d = arg.getDescriptor();
                if ("Lnet/minecraft/world/entity/Entity;".equals(d)
                        || "Lnet/minecraft/world/entity/LivingEntity;".equals(d)) {
                    hit = true;
                    break;
                }
            }
        }

        if (!hit) {
            return original;
        }

        stubbed++;

        return new MethodVisitor(Opcodes.ASM9) {
            private boolean emitted = false;

            @Override
            public void visitCode() {
                if (emitted) {
                    return;
                }
                emitted = true;
                original.visitCode();
                if (returnsBool) {
                    original.visitInsn(Opcodes.ICONST_0);
                    original.visitInsn(Opcodes.IRETURN);
                    original.visitMaxs(1, 1);
                } else {
                    original.visitInsn(Opcodes.RETURN);
                    original.visitMaxs(0, 1);
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
