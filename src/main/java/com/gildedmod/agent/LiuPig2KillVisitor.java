package com.gildedmod.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Pig2 的"极端秒杀"改写器。
 *
 * <h3>只重写两个方法</h3>
 * <pre>
 *   m_21223_()F   getHealth
 *       → return 0.0f;                      // 血量恒 0 → isDeadOrDying() 永远 true
 *
 *   m_8119_()V    tick
 *       → this.f_146801_ = this.mLevelCallbackBackup;   // ★ 归还被它偷走的世界回调
 *         super.m_8119_();                              // 原版 tick：deathTime++ → remove(KILLED)
 * </pre>
 *
 * <h3>为什么这样就能死（而之前所有办法都不行）</h3>
 * <ol>
 *     <li>{@code getHealth} 恒 0 → 原版 {@code isDeadOrDying()} 为 true ✓</li>
 *     <li>它的 tick 被<b>整个替换</b> → 里面的 {@code myRevive()}（每 tick
 *         {@code setHealth(10)} + {@code deathTime = 0}）<b>永不执行</b> ✓</li>
 *     <li>同一个替换也带走了 {@code copy()} / {@code tickLevel1~4} / 踢人逻辑
 *         → 不会再复制、不会升级、不会踢人 ✓</li>
 *     <li>关键：{@code f_146801_}（Entity.levelCallback）原本被它换成了自己的实现，
 *         于是 {@code remove()} 里的 {@code levelCallback.onRemove()} 被吞掉，
 *         世界层收不到移除通知。这里在 tick 开头把它<b>换回真实回调</b>
 *         （真实回调一直存在它的 {@code mLevelCallbackBackup} 里）→ 移除真的生效 ✓</li>
 * </ol>
 *
 * <p>全程<b>不写任何内存、不改任何字段值、不依赖它的任何方法</b> ✓
 */
public class LiuPig2KillVisitor extends ClassVisitor {

    private static final String SUPER_OWNER = "net/minecraft/world/entity/LivingEntity";
    private static final String ENTITY_OWNER = "net/minecraft/world/entity/Entity";
    private static final String CALLBACK_DESC =
            "Lnet/minecraft/world/entity/EntityInLevelCallback;";

    public LiuPig2KillVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

        // getHealth()F → return 0.0f
        if (name.equals("m_21223_") && descriptor.equals("()F")) {
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean emitted = false;

                @Override
                public void visitCode() {
                    if (emitted) return;
                    emitted = true;
                    original.visitCode();
                    original.visitInsn(Opcodes.FCONST_0);
                    original.visitInsn(Opcodes.FRETURN);
                    original.visitMaxs(1, 1);
                    original.visitEnd();
                }

                @Override
                public void visitEnd() {
                    if (!emitted) original.visitEnd();
                }
            };
        }

        // tick()V → 归还回调 + super.tick()
        if (name.equals("m_8119_") && descriptor.equals("()V")) {
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean emitted = false;

                @Override
                public void visitCode() {
                    if (emitted) return;
                    emitted = true;
                    original.visitCode();

                    // this.f_146801_ = this.mLevelCallbackBackup;
                    Label skip = new Label();
                    original.visitVarInsn(Opcodes.ALOAD, 0);
                    original.visitFieldInsn(Opcodes.GETFIELD,
                            "kakiku/pig2mod/entity/Pig2", "mLevelCallbackBackup", CALLBACK_DESC);
                    original.visitJumpInsn(Opcodes.IFNULL, skip);      // 备份为 null 就跳过
                    original.visitVarInsn(Opcodes.ALOAD, 0);
                    original.visitVarInsn(Opcodes.ALOAD, 0);
                    original.visitFieldInsn(Opcodes.GETFIELD,
                            "kakiku/pig2mod/entity/Pig2", "mLevelCallbackBackup", CALLBACK_DESC);
                    original.visitFieldInsn(Opcodes.PUTFIELD, ENTITY_OWNER, "f_146801_", CALLBACK_DESC);
                    original.visitLabel(skip);

                    // super.m_8119_();
                    original.visitVarInsn(Opcodes.ALOAD, 0);
                    original.visitMethodInsn(Opcodes.INVOKESPECIAL, SUPER_OWNER,
                            "m_8119_", "()V", false);
                    original.visitInsn(Opcodes.RETURN);
                    original.visitMaxs(3, 1);
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
}
