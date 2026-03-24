package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.filter.FilterPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 容器界面的鼠标拖拽事件 Mixin
 * 用于处理自定义过滤面板（FilterPanel）与原版界面的交互冲突
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ACSDragMixin {

    /**
     * 拦截鼠标拖拽事件
     * 如果玩家正在操作我们的自定义过滤面板，则取消原版的拖拽逻辑，防止误触背后的物品槽。
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        // 如果过滤面板已打开，并且鼠标正悬停在面板上方，则消耗此事件
        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, screen)) {
            cir.setReturnValue(true);
        }
    }
}