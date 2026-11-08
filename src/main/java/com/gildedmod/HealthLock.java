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

package com.gildedmod;

import com.gildedmod.mixin.LivingEntityHealthAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 血量锁：把目标的血量字段变成「只读 + 谁读都返回固定值」。
 *
 * <h3>思路来源</h3>
 * 用户观察到：我们改的是 Pig2 的<b>私有字段</b>（mLevelCallbackBackup / mIsJisatsu），
 * 每碰一次 JVM 就崩（`jvm.dll+0xc9f0`, `reading 0x4`）—— 因为那是它的内部状态，
 * 它自己的代码假设这些字段永远合法，我们一改就把它推进坏状态。
 *
 * <p>于是换一个层次：<b>不改它的字段，改 MC 自己的 {@code SynchedEntityData.DataItem}</b>。
 * 它把血量恢复成满，最终都要走 {@code entityData.set(DATA_HEALTH_ID, 20f)}，
 * 而我们换进去的 DataItem 会<b>丢弃这次写入</b>，并且任何 {@code getValue()} 都返回固定值。
 *
 * <h3>为什么不用「把内存页设为只读」</h3>
 * Windows 的内存保护是<b>页粒度</b>（4KB），一页里躺着几十个对象 ✗ ——
 * GC 写这些对象会立刻触发访问违例 → 崩。
 * 而 {@code DataItem} 是 public 且非 final 的类，override 它的两个 public 方法就够了 ✓。
 *
 * <h3>为什么这个方案不会崩</h3>
 * 我们只动：
 * <ul>
 *     <li>{@code SynchedEntityData.itemsById} 这个 Map 里的一项（put 一个子类实例）</li>
 *     <li>{@code DataItem} 的两个 public 方法（override，不改字段布局）</li>
 * </ul>
 * <b>完全不碰 {@code pig2mod} 的任何私有字段</b>，也不做字节码重载（retransform）。
 */
public final class HealthLock {

    /** 被锁的实体 UUID → 原来的 DataItem（解锁时放回去；值可能为 null，表示原本没有） */
    private static final Map<UUID, Object> ORIGINAL = new ConcurrentHashMap<>();

    /** 被锁的实体 UUID → 锁定的数值 */
    private static final Map<UUID, Float> LOCKED = new ConcurrentHashMap<>();

    // 只打印一次，避免每 tick 刷屏（accessor 失效时会每 tick 走到这里）
    private static boolean warnedNoAccessor = false;
    private static boolean warnedNoMap = false;

    private HealthLock() {
    }

    /**
     * 血量只读包装。
     *
     * <p>{@code DataItem<Float>} 在字节码层面是 {@code DataItem<T>}，
     * 编译器会为这两个 override 生成桥接方法，所以能正确覆盖 ✓
     */
    public static final class FrozenItem extends SynchedEntityData.DataItem<Float> {

        /**
         * 锁定的数值。
         *
         * <p>为什么是 {@code volatile} 而不是 final：玩家的最大生命值会随装备/属性/药水变化，
         * 每 tick 调用 {@link HealthLock#lock} 时需要把锁定值抬到新的上限。
         * 只改这个字段是安全的 —— 读它的只有 {@link #getValue()}，
         * 而 {@link #setValue} 永远是"丢弃"，所以不存在"写进去了"的问题。
         */
        private volatile float frozen;

        public FrozenItem(EntityDataAccessor<Float> accessor, float frozen) {
            super(accessor, frozen);
            this.frozen = frozen;
        }

        /** 更新锁定值（换装备导致最大生命变化时用） */
        public void setFrozen(float value) {
            this.frozen = value;
        }

        public float frozenValue() {
            return frozen;
        }

        /** 任何写入都被丢弃 —— 包括 setHealth、pig2mod 的 setHealth(max)、以及非虚调用 */
        @Override
        public void setValue(Float ignored) {
            try {
                // 只把"脏"标上，让 MC 把这个固定值同步给客户端；
                // 绝不写入真正的 value ✓
                this.setDirty(true);
            } catch (Throwable ignoredEx) {
                // 忽略
            }
        }

        /** 谁读都拿到固定值 */
        @Override
        public Float getValue() {
            return frozen;
        }
    }

    /** 通过扫描字段类型拿 itemsById（避免硬编码 SRG 名 / 官方名不一致） */
    @SuppressWarnings("unchecked")
    private static Int2ObjectMap<Object> itemsById(SynchedEntityData data) {
        for (Field f : SynchedEntityData.class.getDeclaredFields()) {
            if (Int2ObjectMap.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    return (Int2ObjectMap<Object>) f.get(data);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 锁住目标血量（<b>幂等</b>，可以每 tick 反复调用）。
     *
     * <p>每 tick 调用时做三件事：
     * <ol>
     *     <li>如果已经是我们的 {@link FrozenItem} → 只更新数值（最大生命可能变）</li>
     *     <li>如果被人<b>换回了原生 DataItem</b>（直写 / 重登 / 换维度重建）→ <b>重新装回去</b></li>
     *     <li>首次锁定 → 记下原始 DataItem，装上我们的</li>
     * </ol>
     *
     * <p>之所以能"每 tick 调"：成功路径只有一次 map 查找 + 一个 float 写，
     * 没有分配、没有反射、没有 JNI 往返。
     *
     * @param target 目标实体
     * @param value  锁定值（护甲用 {@code getMaxHealth()}，斩杀用 0）
     * @return true = 当前处于锁定状态
     */
    public static boolean lock(LivingEntity target, float value) {
        if (target == null) {
            return false;
        }
        try {
            UUID id = target.getUUID();

            EntityDataAccessor<Float> accessor = LivingEntityHealthAccessor.getDataHealthId();
            if (accessor == null) {
                if (!warnedNoAccessor) {
                    warnedNoAccessor = true;
                    System.out.println("[HPLOCK] 拿不到 DATA_HEALTH_ID（Mixin accessor 未生效？）");
                }
                return false;
            }

            SynchedEntityData data = target.getEntityData();
            Int2ObjectMap<Object> map = itemsById(data);
            if (map == null) {
                if (!warnedNoMap) {
                    warnedNoMap = true;
                    System.out.println("[HPLOCK] 拿不到 itemsById");
                }
                return false;
            }

            int key = accessor.getId();
            Object current = map.get(key);

            // ① 已经是我们的 → 更新锁定值即可（每 tick 走这条，零分配）
            if (current instanceof FrozenItem ours) {
                if (ours.frozenValue() != value) {
                    ours.setFrozen(value);
                }
                LOCKED.put(id, value);
                return true;
            }

            // ② 不是我们的（首次 / 被人换回来 / 重登后新建）→ 先把原始项的值清零，再换掉
            //
            //   为什么要清原始项：只替换不动原件，一旦有人把我们换回去，
            //   原件里那个旧值（例如 10）会立刻复活 ✗。
            //   所以先把它自己的 value 写成目标值（0），再换 —— 这样即使被还原也是 0 ✓
            if (current instanceof net.minecraft.network.syncher.SynchedEntityData.DataItem<?> vanilla) {
                try {
                    @SuppressWarnings("unchecked")
                    var asFloat = (net.minecraft.network.syncher.SynchedEntityData.DataItem<Float>) vanilla;
                    asFloat.setValue(value);      // 原件清零（同时标脏 → 客户端同步）
                } catch (Throwable ignored) {
                    // 类型不符就跳过，不影响后面的替换
                }
            }

            if (current != null && !ORIGINAL.containsKey(id)) {
                ORIGINAL.put(id, current);   // 只记第一次的原始值
            }

            map.put(key, new FrozenItem(accessor, value));
            LOCKED.put(id, value);

            if (!ORIGINAL.containsKey(id)) {
                ORIGINAL.put(id, null);      // 占位，表示"本次会话已锁过"
            }

            System.out.println("[HPLOCK] 已锁住 " + target.getClass().getSimpleName()
                    + " id=" + target.getId()
                    + " 血量=" + value
                    + (current == null ? "（原本没有 DataItem）"
                                       : "（被替换的是 " + current.getClass().getSimpleName() + "）"));
            return true;
        } catch (Throwable t) {
            System.out.println("[HPLOCK] 锁定失败: " + t);
            return false;
        }
    }

    /**
     * 检查锁定是否<b>仍然有效</b>（没被人换回原生 DataItem）。
     *
     * <p>这是"每 tick 检查"的核心：返回 false 就说明有人动过手，
     * 要么直接改了 {@code entityData}，要么把整个 DataItem 换掉了。
     */
    public static boolean verify(LivingEntity target) {
        if (target == null) {
            return false;
        }
        try {
            EntityDataAccessor<Float> accessor = LivingEntityHealthAccessor.getDataHealthId();
            if (accessor == null) {
                return false;
            }
            Int2ObjectMap<Object> map = itemsById(target.getEntityData());
            if (map == null) {
                return false;
            }
            return map.get(accessor.getId()) instanceof FrozenItem;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 取当前锁定值；未锁定返回 NaN */
    public static float lockedValue(LivingEntity target) {
        if (target == null) {
            return Float.NaN;
        }
        Float v = LOCKED.get(target.getUUID());
        return v == null ? Float.NaN : v;
    }

    /**
     * 解锁：把原来的 DataItem 放回去。
     *
     * <p>如果原本没有原始 DataItem（正常不会发生），退化为"装一个普通 DataItem"，
     * 绝不留下没有血量项的实体（那会让 {@code entityData.get} 抛
     * {@code IllegalArgumentException}，进而把服务端 tick 打断）。
     */
    public static void unlock(LivingEntity target) {
        if (target == null) {
            return;
        }
        try {
            UUID id = target.getUUID();
            boolean everLocked = LOCKED.containsKey(id) || ORIGINAL.containsKey(id);
            Object old = ORIGINAL.remove(id);
            LOCKED.remove(id);
            if (!everLocked) {
                return;   // 从来没锁过
            }

            EntityDataAccessor<Float> accessor = LivingEntityHealthAccessor.getDataHealthId();
            if (accessor == null) {
                return;
            }
            Int2ObjectMap<Object> map = itemsById(target.getEntityData());
            if (map == null) {
                return;
            }

            if (old != null) {
                map.put(accessor.getId(), old);
            } else {
                // 没有原始项 → 退回普通 DataItem，值是当前最大生命
                map.put(accessor.getId(), new SynchedEntityData.DataItem<Float>(accessor, target.getMaxHealth()));
            }

            System.out.println("[HPLOCK] 已解锁 " + target.getClass().getSimpleName()
                    + " id=" + target.getId());
        } catch (Throwable t) {
            System.out.println("[HPLOCK] 解锁失败: " + t);
        }
    }

    /** 解锁时判断"之前确实锁过"（ORIGINAL 里可能是 null 占位） */
    private static boolean wasLockedEver(UUID id) {
        return LOCKED.containsKey(id);
    }

    /** 是否已锁（登记表里有记录） */
    public static boolean isLocked(LivingEntity target) {
        return target != null && ORIGINAL.containsKey(target.getUUID());
    }

    public static int lockedCount() {
        return ORIGINAL.size();
    }

    public static void clear() {
        ORIGINAL.clear();
        LOCKED.clear();
    }
}
