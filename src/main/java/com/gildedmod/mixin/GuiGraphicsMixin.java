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
package com.gildedmod.mixin;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.GildedMod;
import com.gildedmod.client.TooltipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void liuCustomTooltip(Font font,
                                  List<ClientTooltipComponent> components,
                                  int mouseX, int mouseY,
                                  ClientTooltipPositioner positioner,
                                  CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = ItemStack.EMPTY;

        if (mc.screen instanceof AbstractContainerScreen<?> container) {
            Slot slot = container.getSlotUnderMouse();
            if (slot != null) {
                stack = slot.getItem();
            }
        }

        if (stack.isEmpty()) {
            return;
        }

        // 只接管【本 mod 自己的物品】（命名空间 gildedmod）；其他模组/原版物品保持原版 Tooltip
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!GildedMod.MODID.equals(id.getNamespace())) {
            return;
        }

        // 只给【装备类】上面板：工具（剑/镐/斧都继承 TieredItem）+ 盔甲（ArmorItem）。
        // 材料（锭/粒/鎏金锭/升级模板/星/星系/黑洞/哈勃体/压缩下界合金/黑金块）保持原版 tooltip。
        boolean equipment = stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof ArmorItem;
        if (!equipment) {
            return;
        }

        GuiGraphics self = (GuiGraphics) (Object) this;

        // 黑金剑：内容是他那套定制的神权介绍
        if (stack.getItem() instanceof BlackGoldSword) {
            TooltipRenderer.renderFullTooltip(self, font, mouseX, mouseY);
            ci.cancel();
            return;
        }

        // 其他物品：套同一套 GLSL 面板，内容是它自己的 tooltip 文字
        TooltipFlag flag = mc.options.advancedItemTooltips
                ? TooltipFlag.Default.ADVANCED
                : TooltipFlag.Default.NORMAL;
        List<String> lines = new ArrayList<>();
        for (Component line : stack.getTooltipLines(mc.player, flag)) {
            // getString() 会丢掉 § 颜色码 —— 没关系，颜色由着色器的流光接管
            lines.add(line.getString());
        }
        TooltipRenderer.renderPanel(self, font, lines, mouseX, mouseY);
        ci.cancel();
    }
}
