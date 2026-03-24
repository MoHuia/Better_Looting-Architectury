package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.filter.FilterPanel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ACSDragMixin {

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, screen)) {
            cir.setReturnValue(true);
        }
    }
}