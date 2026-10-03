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
package com.example.examplemod.mixin;

import com.example.examplemod.BlackGoldSword;
import com.example.examplemod.client.TooltipRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        if (!(stack.getItem() instanceof BlackGoldSword)) return;

        TooltipRenderer.renderFullTooltip((GuiGraphics)(Object)this, font, mouseX, mouseY);
        ci.cancel();
    }
}