package com.gildedmod.mixin;

import com.gildedmod.BlackGoldSword;
import com.gildedmod.net.AltSync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 背包里的"护剑"逻辑：黑金剑<b>无法被拿走</b>，只能被<b>复制一份到鼠标</b>；
 * 只有按住 <b>Alt</b>（状态由 {@link AltSync} 从客户端同步过来）时才恢复正常操作。
 *
 * <p>{@code AbstractContainerMenu.clicked(...)} 是<b>双端都会执行</b>的入口
 * （客户端做预测、服务端做判定），所以在这里改不会产生不同步。
 * <p>覆盖所有点击类型：普通拿取、shift 快速移动、数字键交换、Q 丢出、拖拽…
 * —— 只要目标槽位里是这把剑，一律"复制给鼠标 + 原剑不动"。
 */
@Mixin(AbstractContainerMenu.class)
public class ContainerProtectMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void gilded$protectSword(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // ★ 双击（PICKUP_ALL）会把背包里所有同 id 的物品"吸"到鼠标上，
        //   而且它可以在【空处双击】触发（此时 slotId = -999）→ 绕过只检查槽位的护剑逻辑。
        //   这里统一处理：鼠标上拿的是黑金剑、又没按 Alt → 直接把这次点击吃掉。
        if (clickType == ClickType.PICKUP_ALL) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty() && carried.getItem() instanceof BlackGoldSword
                    && !AltSync.bypass(player)) {
                ci.cancel();
            }
            return;
        }

        // -999 = 鼠标在界面外（丢出窗外），越界直接放行给我们另一个 drop 保护兜底
        if (slotId < 0) {
            return;
        }

        AbstractContainerMenu self = menu;
        if (slotId >= self.slots.size()) {
            return;
        }

        // 按住 Alt → 完全按原版逻辑走
        if (AltSync.bypass(player)) {
            return;
        }

        Slot slot = self.slots.get(slotId);
        ItemStack inSlot = slot.getItem();
        if (inSlot.isEmpty() || !(inSlot.getItem() instanceof BlackGoldSword)) {
            return;
        }

        // 复制一份到鼠标上，槽位保持原样
        if (self.getCarried().isEmpty()) {
            self.setCarried(inSlot.copy());
        }
        ci.cancel();
    }
}
