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

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

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

        // ---- 内容 ----（恢复原版配色）
        String[] attributes = {
                "§e✦ 无限耐久",
                "§e✦ 免疫一切伤害",
                "§e✦ 手持自动飞行",
                "§e✦ 左键直接寂湮",
                "§c✦ 一击必杀",
        };
        String[] godhurt = {
                "§c+§6§l鎏金 §c攻击伤害",
                "§c+§6§l无以至穷 §c攻击速度",
        };
        String[] skills = {
                "§7右键释放: §e鎏·罚 §8[全屏抹除]",
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

        // ---- 背景：GLSL 金液流动（第一版着色器，黑金配色）----
        drawFlowingBlackGold(g, x, y, totalW, totalH, t);

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

        // 标题（黑金流光）
        drawFlowingText(g, font, "§lGildedEX", cx + iconSize + 4, cy + 4, t);
        cy += iconSize + 4;
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 属性
        for (String line : attributes) {
            drawFlowingText(g, font, line, cx, cy, t);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // godhurt
        for (String line : godhurt) {
            drawFlowingText(g, font, line, cx, cy, t);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 技能
        for (String line : skills) {
            drawFlowingText(g, font, line, cx, cy, t);
            cy += lineHeight;
        }
        drawGoldenDivider(g, cx, cy, contentW);
        cy += 4 + 1 + 4;

        // 结语
        drawFlowingText(g, font, footer, cx, cy, t);

        g.pose().popPose();
    }

    // ============ 通用面板（本 mod 的其他物品用） ============
    /**
     * 通用版：外观（GLSL 金液背景 + 流动金边 + 逐字流光文字）和剑的 Tooltip 完全一致，
     * 但<b>内容是传进来的任意文本行</b>（调用方从 {@code ItemStack.getTooltipLines} 取）。
     * <p>高度按行数自适应 —— 这样鎏金锭、盔甲、材料等物品都能直接套上同一套皮肤。
     */
    public static void renderPanel(GuiGraphics g, Font font, List<String> lines,
                                   int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) {
            return;
        }

        int padding = 8;
        int lineHeight = 12;

        int contentW = 0;
        for (String s : lines) {
            contentW = Math.max(contentW, font.width(s));
        }
        contentW = Math.max(contentW, 120);

        int totalW = contentW + padding * 2;
        int totalH = lines.size() * lineHeight + padding * 2;

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

        // 背景：同一套 GLSL 金液
        drawFlowingBlackGold(g, x, y, totalW, totalH, t);

        // 外发光 + 主边框
        for (int layer = 2; layer >= 1; layer--) {
            drawGoldenBorder(g, x - layer, y - layer, totalW + layer * 2, totalH + layer * 2, t, 0x20 / layer);
        }
        drawGoldenBorder(g, x - 2, y - 2, totalW + 4, totalH + 4, t, 0xFF);

        // 内容：逐字流光
        int cy = y + padding;
        for (String line : lines) {
            drawFlowingText(g, font, line, x + padding, cy, t);
            cy += lineHeight;
        }

        g.pose().popPose();
    }

    // ============ 黑金金液背景（GLSL） ============
    /*
     * 用 {@code gildedmod:tooltip} 着色器画整个面板背景。
     * <p>着色器里是海面涌浪：方向几乎一致的正弦波叠加 + 浪脊高光 + 碎光荡漾，
     * 这是纯 {@code g.fill} 方块拼不出来的连续效果。
     * <p>顺序很关键：先 {@code shader.apply()} 把程序绑上，**再**写 uniform，
     * 最后画 quad 并 flush —— 这样 uniform 才会落到正确的程序上。
     */

    /** 面板配色：0/1 = 碧波，2 = 黑金（判断在 tooltip.fsh 里，改这个值要重新编译）。 */
    private static final float PANEL_PALETTE = 1.0F;

    private static void drawFlowingBlackGold(GuiGraphics g, int x, int y, int w, int h, long t) {
        net.minecraft.client.renderer.ShaderInstance shader = GildedModClient.tooltipShader;
        if (shader == null) {
            // 着色器还没就绪（理论上只在极早期）→ 退回纯色，别画成黑框
            g.fillGradient(x, y, x + w, y + h, argb(0xF2, 0x07, 0x05, 0x02), argb(0xF2, 0x2A, 0x20, 0x08));
            return;
        }

        shader.apply();
        var timeUniform = shader.safeGetUniform("time");
        if (timeUniform != null) {
            timeUniform.set((System.currentTimeMillis() % 1000000L) / 1000.0F);
        }
        var sizeUniform = shader.safeGetUniform("panelSize");
        if (sizeUniform != null) {
            sizeUniform.set((float) w, (float) h);
        }
        var paletteUniform = shader.safeGetUniform("palette");
        if (paletteUniform != null) {
            paletteUniform.set(PANEL_PALETTE);
        }

        MultiBufferSource.BufferSource buffers = g.bufferSource();
        VertexConsumer vc = buffers.getBuffer(TooltipRenderType.TOOLTIP);
        Matrix4f m = g.pose().last().pose();
        // 覆盖整个面板的一个 quad，UV 走 0..1（着色器用它当面板坐标）
        vc.vertex(m, x, y, 0).color(255, 255, 255, 255).uv(0.0F, 0.0F).endVertex();
        vc.vertex(m, x, y + h, 0).color(255, 255, 255, 255).uv(0.0F, 1.0F).endVertex();
        vc.vertex(m, x + w, y + h, 0).color(255, 255, 255, 255).uv(1.0F, 1.0F).endVertex();
        vc.vertex(m, x + w, y, 0).color(255, 255, 255, 255).uv(1.0F, 0.0F).endVertex();
        buffers.endBatch(TooltipRenderType.TOOLTIP);
    }

    // ============ 黑金流光文字 ============
    /**
     * 逐字绘制：颜色在 暗金 ⇄ 亮金 之间沿横向流动，整行字因此有"光在流"的感觉。
     * <p>会保留原文里的 §l（粗体）/ §o（斜体）/ §n（下划线）样式码；
     * 颜色码被忽略 —— 颜色改由流光计算，所以内容里的 §6/§e/§8 影响不到最终颜色，
     * 文字本身一字未动。
     */
    private static void drawFlowingText(GuiGraphics g, Font font, String text, int x, int y, long t) {
        float phase = (t % 4000L) / 4000.0f;               // 4 秒一轮
        int cx = x;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                switch (code) {
                    case 'l' -> bold = true;
                    case 'o' -> italic = true;
                    case 'n' -> underline = true;
                    case 'r' -> {
                        bold = false;
                        italic = false;
                        underline = false;
                    }
                    default -> {
                        // 颜色码忽略，交由流光决定
                    }
                }
                continue;
            }
            if (c == ' ') {
                cx += font.width(" ");
                continue;
            }

            // 更长的渐变 + smoothstep 平滑 + 更低的对比度 → 不再是生硬的"一亮一暗"
            float p = ((cx - x) / 260.0f + phase) % 1.0f;
            float wave = 0.5f + 0.5f * (float) Math.sin(p * Math.PI * 2.0);
            float soft = wave * wave * (3.0f - 2.0f * wave);
            int color = goldLerp(0.42f + 0.58f * soft, 0xFF);

            StringBuilder sb = new StringBuilder(4);
            if (bold) sb.append("§l");
            if (italic) sb.append("§o");
            if (underline) sb.append("§n");
            sb.append(c);
            g.drawString(font, sb.toString(), cx, y, color, true);
            cx += font.width(String.valueOf(c));
        }
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
