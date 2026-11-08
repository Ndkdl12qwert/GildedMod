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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处决名单：被鎏金剑命中的实体，在限定时间内<b>拒绝一切回血</b>。
 *
 * <h3>为什么需要它</h3>
 * 实测现象：打穿其他"神器"模组盔甲的玩家时，他们的血条会瞬间清空，
 * 但约 0.1 秒（2 tick）内又回满 —— 说明我们的击杀<b>命中了</b>，
 * 但对方 mod 在 tick 里把血量写回去了。
 *
 * <h3>怎么拦</h3>
 * 不碰 C++，只用 agent 已经存在的 {@code setHealth} 钩子
 * （{@link LiuArmorHook#shouldBlockSetHealth(Object, float)}）：
 *
 * <pre>
 *   对方：setHealth(maxHealth)      → newHealth &gt; 0 且 &gt;= 当前血量 → 拦下
 *   伤害：setHealth(当前 - 伤害)     → newHealth &lt; 当前血量      → 放行
 *   死亡：setHealth(0)              → newHealth == 0            → 放行（★ 必须放行）
 * </pre>
 *
 * <h3>性能</h3>
 * 这个判定会被<b>每一个实体</b>的每一次 {@code setHealth} 调用触发，
 * 所以第一行必须是 {@link #isEmpty()} 的快速返回 —— 名单为空时零开销。
 *
 * <h3>设计要点</h3>
 * <ul>
 *     <li>只存 {@link UUID}，不存实体引用 —— 否则实体永远不能被 GC 回收 ✗</li>
 *     <li>带 tick 计数，到期自动移除 —— 否则名单无限增长 + 永久免疫 ✗</li>
 *     <li>{@link ConcurrentHashMap} —— 钩子可能在任意线程被调用</li>
 * </ul>
 */
public final class LiuDoom {

    /** 处决中的实体 UUID */
    private static final Set<UUID> DOOMED = ConcurrentHashMap.newKeySet();

    /** 剩余 tick 数 */
    private static final Map<UUID, Integer> TICKS = new ConcurrentHashMap<>();

    /** 拦截日志只打前几次，避免刷屏 */
    private static final int LOG_LIMIT = 5;
    private static int logCount = 0;

    private LiuDoom() {
    }

    /**
     * 名单是否为空 —— <b>热路径的第一道判断</b>。
     * 钩子里先问这个，为空就直接跳过，不碰任何哈希表。
     */
    public static boolean isEmpty() {
        return DOOMED.isEmpty();
    }

    /** 标记一个实体：接下来 ticks 个 tick 内不许回血。 */
    public static void mark(UUID id, int ticks) {
        if (id == null || ticks <= 0) {
            return;
        }
        DOOMED.add(id);
        TICKS.put(id, ticks);
        System.out.println("[DOOM] 标记 " + id + " → " + ticks + " ticks（期间拒绝回血）");
    }

    /** 判断这个实体是否在处决名单里。 */
    public static boolean isDoomed(UUID id) {
        if (DOOMED.isEmpty()) {
            return false;
        }
        return DOOMED.contains(id);
    }

    /**
     * 每 tick 调一次：递减计数，到期移除。
     * 名单为空时直接返回（绝大多数时间都是空的）。
     */
    public static void tick() {
        if (TICKS.isEmpty()) {
            return;
        }
        TICKS.entrySet().removeIf(entry -> {
            int left = entry.getValue() - 1;
            if (left > 0) {
                entry.setValue(left);
                return false;
            }
            DOOMED.remove(entry.getKey());
            System.out.println("[DOOM] 解除 " + entry.getKey());
            return true;
        });
    }

    /** 拦下一次回血时打日志（只打前 LOG_LIMIT 次）。 */
    public static void logIntercept(float newHealth, float current) {
        if (logCount >= LOG_LIMIT) {
            return;
        }
        logCount++;
        System.out.println("[DOOM] 拦下回血 new=" + newHealth + " current=" + current
                + (logCount == LOG_LIMIT ? "（后续不再打印）" : ""));
    }

    /** 清空名单（换世界 / 调试用）。 */
    public static void clear() {
        DOOMED.clear();
        TICKS.clear();
        logCount = 0;
        System.out.println("[DOOM] 已清空");
    }
}
