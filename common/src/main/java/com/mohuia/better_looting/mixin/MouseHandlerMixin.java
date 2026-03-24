package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.filter.FilterPanel;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.client.jei.JeiCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 500)
public class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique
    private static long better_looting$lastClickTime = 0;


    @Unique
    private double better_looting$getScaledMouseX() {
        return this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
    }

    @Unique
    private double better_looting$getScaledMouseY() {
        return this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (!com.mohuia.better_looting.client.Core.INSTANCE.shouldIgnoreScroll()) {
            com.mohuia.better_looting.client.Core.INSTANCE.performScroll(yOffset);

            ci.cancel();
            return;
        }

        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, screen)) {
            if (FilterPanel.scroll(yOffset)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, containerScreen)) {
            ci.cancel();
            if (action == 1) {
                FilterPanel.click(mouseX, mouseY, containerScreen);
            }
            return;
        }

        if (FilterPanel.isOpen() && (button == 0 || button == 1)) {
            if (!containerScreen.getMenu().getCarried().isEmpty()) return;

            ItemStack jeiStack = JeiCompat.getHoveredItem();
            Slot hoveredSlot = FilterEvents.getHoveredSlot(containerScreen, mouseX, mouseY);

            boolean hasTarget = (hoveredSlot != null && hoveredSlot.hasItem()) || (jeiStack != null && !jeiStack.isEmpty());

            if (hasTarget) {
                ci.cancel();

                if (action == 1) {
                    if (System.currentTimeMillis() - better_looting$lastClickTime < 200) return;
                    better_looting$lastClickTime = System.currentTimeMillis();

                    ItemStack target = (hoveredSlot != null && hoveredSlot.hasItem()) ? hoveredSlot.getItem() : jeiStack;

                    if (button == 0) {
                        FilterWhitelist.INSTANCE.add(target);
                    } else {
                        FilterWhitelist.INSTANCE.remove(target);
                    }

                    float pitch = button == 0 ? 1.0f : 0.5f;
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
                }
            }
        }
    }
}