package com.mohuia.better_looting.client.filter;

import com.mohuia.better_looting.mixin.ACSAccessor;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;


public class FilterEvents {
    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 150;

    public static void init() {
        FilterWhitelist.INSTANCE.init();

        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            lastClickTime = 0;
            FilterPanel.close();
        });

        ClientGuiEvent.RENDER_POST.register((screen, gui, mouseX, mouseY, delta) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                FilterPanel.render(gui, mouseX, mouseY, containerScreen);
            }
        });

        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (client.screen instanceof AbstractContainerScreen<?> && action == GLFW.GLFW_PRESS) {
                if (client.screen.getFocused() instanceof EditBox) {
                    return EventResult.pass();
                }

                if (keyCode == GLFW.GLFW_KEY_LEFT_ALT) {
                    FilterPanel.toggle();
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });

        ClientRawInputEvent.MOUSE_SCROLLED.register((client, scrollAmount) -> {
            if (client.screen instanceof AbstractContainerScreen<?>) {
                if (FilterPanel.isOpen() && FilterPanel.scroll(scrollAmount)) {
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });
    }

    public static boolean isMouseOverPanel(double mouseX, double mouseY, AbstractContainerScreen<?> screen) {
        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - FilterPanel.PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - FilterPanel.PANEL_HEIGHT) / 2;
        return mouseX >= startX && mouseX <= startX + FilterPanel.PANEL_WIDTH &&
                mouseY >= startY && mouseY <= startY + FilterPanel.PANEL_HEIGHT;
    }

    public static Slot getHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        ACSAccessor acc = (ACSAccessor) screen;
        double relX = mouseX - acc.getLeftPos();
        double relY = mouseY - acc.getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            if (relX >= slot.x - 1 && relX < slot.x + 17 &&
                    relY >= slot.y - 1 && relY < slot.y + 17) {
                return slot;
            }
        }
        return null;
    }
}