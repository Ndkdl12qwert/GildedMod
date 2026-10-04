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
package com.gildedmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class TooltipRenderer {

    private static final ThreadLocal<ItemStack> CURRENT_STACK =
            ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    public static void setCurrentStack(ItemStack stack) {
        CURRENT_STACK.set(stack == null ? ItemStack.EMPTY : stack);
    }

    public static ItemStack getCurrentStack() {
        return CURRENT_STACK.get();
    }

    private static final int GOLD_BRIGHT = 0xFFFFD700;
    private static final int GOLD_MID    = 0xFFDAA520;
    private static final int GOLD_DARK   = 0xFF8B6914;
    private static final int GOLD_DEEP   = 0xFF4A3800;
    private static final int BLACK       = 0xFF0A0805;


    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int goldLerp(float t, int alpha) {
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        int r = (int) (0x8B + (0xFF - 0x8B) * t);
        int g = (int) (0x69 + (0xD7 - 0x69) * t);
        int b = (int) (0x14 + (0x00 - 0x14) * t);
        return argb(alpha, r, g, b);
    }

    // ============ 主入口 ============

    public static void renderFullTooltip(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int padding = 10;
        int lineHeight = 12;
        int iconSize = 16;

        // ---- 内容 ----
        String[] attributes = {
                "§e✦ §6神权·永恒    §7»  §a无限耐久",
                "§e✦ §6神权·不灭    §7»  §a免疫一切伤害",
                "§e✦ §6神权·翱翔    §7»  §a手持自动飞行",
                "§e✦ §6神权·抹除    §7»  §a左键直接删除",
                "§c✦ §6神权·秒杀    §7»  §c一击必杀",
        };
        String[] godhurt = {
                "§c+§6§l鎏金 §c攻击伤害",
                "§c+§6§l无以至穷 §c攻击速度",
        };
        String[] skills = {
                "§6✦ §l当前神权: §e§l天罚",
                "§7右键释放: §e天罚·雷霆万钧 §8[全屏抹除]",
        };
        String footer = "§7§o「吾持手中之太阿，至以无穷，鎏！」";

        // ---- 算宽 ----
        int titleW = font.width("GildedEX") + 10;
        int maxAttrW = 0;
        for (String s : attributes) maxAttrW = Math.max(maxAttrW, font.width(s));
        int maxGodhurtW = 0;
        for (String s : godhurt) maxGodhurtW = Math.max(maxGodhurtW, font.width(s));
        int maxSkillW = 0;
        for (String s : skills) maxSkillW = Math.max(maxSkillW, font.width(s));
        int footerW = font.width(footer);

        int contentW = Math.max(
                Math.max(titleW, maxAttrW),
                Math.max(Math.max(maxSkillW, footerW), maxGodhurtW)
        );
        contentW = Math.max(contentW, 180);

        // ---- 算高 ----
        int contentH = iconSize + 4
                + 4 + 1 + 4
                + attributes.length * lineHeight
                + 4 + 1 + 4
                + godhurt.length * lineHeight
                + 4 + 1 + 4
                + skills.length * lineHeight
                + 4 + 1 + 4
                + lineHeight;

        int totalW = contentW + padding * 2;
        int totalH = contentH + padding * 2;

        // ---- 定位 ----
        int x = mouseX + 12;
        int y = mouseY - 12;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (x + totalW > screenW) x = mouseX - 12 - totalW;
        if (y + totalH > screenH) y = mouseY - 12 - totalH;
        if (x < 0) x = 0;
        if (y < 0) y = 0;

        long t = System.currentTimeMillis();

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // ---- 背景：黑→暗金渐变 ----
        int bgTop = argb(0xE8, 0x0A, 0x08, 0x05);
        int bgBottom = argb(0xE8, 0x2A, 0x20, 0x08);
        g.fillGradient(x, y, x + totalW, y + totalH, bgTop, bgBottom);

        // ---- 外发光（3 层金色，越外越淡）----
        for (int layer = 3; layer >= 1; layer--) {
            int alpha = 0x20 / layer;
            drawGoldenBorder(g, x - layer, y - layer, totalW + layer * 2, totalH + layer * 2, t, alpha);
        }
        // ---- 主边框 ----
        drawGoldenBorder(g, x - 2, y - 2, totalW + 4, totalH + 4, t, 0xFF);

        // ---- 四角金色装饰 ----
        g.fill(x - 3, y - 3, x - 1, y - 1, GOLD_BRIGHT);
        g.fill(x + totalW + 1, y - 3, x + totalW + 3, y - 1, GOLD_BRIGHT);
        g.fill(x - 3, y + totalH + 1, x - 1, y + totalH + 3, GOLD_BRIGHT);
        g.fill(x + totalW + 1, y + totalH + 1, x + totalW + 3, y + totalH + 3, GOLD_BRIGHT);

        // ---- 内容 ----
        int cx = x + padding;
        int cy = y + padding;

        // 标题（金色，粗体）
        g.drawString(font, "§6§lGildedEX", cx + iconSize + 4, cy + 4, 0xFFFFFF, true);
        cy += iconSize + 4;
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 属性
        for (String line : attributes) {
            g.drawString(font, line, cx, cy, 0xFFFFFF, true);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // godhurt
        for (String line : godhurt) {
            g.drawString(font, line, cx, cy, 0xFFFFFF, true);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 技能
        for (String line : skills) {
            g.drawString(font, line, cx, cy, 0xFFFFFF, true);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 结语
        g.drawString(font, footer, cx, cy, 0xFFFFFF, true);

        g.pose().popPose();
    }

    // ============ 鎏金边框（明暗呼吸） ============
    private static void drawGoldenBorder(GuiGraphics g, int x, int y, int w, int h,
                                         long t, int alpha) {
        int step = 2;
        int thickness = 1;

        // 时间相位，让"金光"沿边框流动
        float timePhase = (t / 2500.0f) % 1.0f;

        // 上边框
        for (int i = 0; i < w; i += step) {
            float pos = (i / (float) w);
            // 波浪：sin 让亮度在 0.6~1.0 之间
            float brightness = 0.75f + 0.25f * (float) Math.sin((pos + timePhase) * Math.PI * 2);
            int color = goldLerp(brightness, alpha);
            g.fill(x + i, y, x + Math.min(i + step, w), y + thickness, color);
        }
        // 下边框
        for (int i = 0; i < w; i += step) {
            float pos = (i / (float) w);
            float brightness = 0.75f + 0.25f * (float) Math.sin((pos + timePhase + 0.5f) * Math.PI * 2);
            int color = goldLerp(brightness, alpha);
            g.fill(x + i, y + h - thickness, x + Math.min(i + step, w), y + h, color);
        }
        // 左边框
        for (int i = 0; i < h; i += step) {
            float pos = (i / (float) h);
            float brightness = 0.75f + 0.25f * (float) Math.sin((pos + timePhase + 0.25f) * Math.PI * 2);
            int color = goldLerp(brightness, alpha);
            g.fill(x, y + i, x + thickness, y + Math.min(i + step, h), color);
        }
        // 右边框
        for (int i = 0; i < h; i += step) {
            float pos = (i / (float) h);
            float brightness = 0.75f + 0.25f * (float) Math.sin((pos + timePhase + 0.75f) * Math.PI * 2);
            int color = goldLerp(brightness, alpha);
            g.fill(x + w - thickness, y + i, x + w, y + Math.min(i + step, h), color);
        }
    }

    // ============ 鎏金分割线 ============
    private static void drawGoldenDivider(GuiGraphics g, int x, int y, int width) {
        long t = System.currentTimeMillis();
        float timePhase = (t / 2500.0f) % 1.0f;

        // 左端到右端：暗金 → 亮金 → 暗金（对称渐变）
        for (int i = 0; i < width; i += 2) {
            float pos = (i / (float) width);
            float brightness = 0.5f + 0.5f * (float) Math.sin(pos * Math.PI);   // 0→1→0
            // 加时间波动
            brightness *= 0.85f + 0.15f * (float) Math.sin((pos + timePhase) * Math.PI * 2);
            int color = goldLerp(brightness, 0xC0);
            g.fill(x + i, y, x + Math.min(i + 2, width), y + 1, color);
        }
    }
}