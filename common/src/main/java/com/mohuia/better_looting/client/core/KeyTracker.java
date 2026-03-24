package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.config.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyTracker {
    private boolean toggleOverlayActive = false;
    private boolean wasToggleKeyDown = false;
    private boolean wasFilterKeyDown = false;
    private boolean wasConfigKeyDown = false;
    private boolean wasAutoKeyDown = false;

    public void tickOverlayToggle() {
        boolean isCurrentToggleDown = KeyInit.SHOW_OVERLAY.isDown();
        if (isCurrentToggleDown && !wasToggleKeyDown) {
            toggleOverlayActive = !toggleOverlayActive;
        }
        wasToggleKeyDown = isCurrentToggleDown;
    }

    public void tickActionToggles(Runnable onToggleFilter, Runnable onToggleAuto) {
        boolean isCurrentFilterDown = KeyInit.TOGGLE_FILTER.isDown();
        if (isCurrentFilterDown && !wasFilterKeyDown) onToggleFilter.run();
        wasFilterKeyDown = isCurrentFilterDown;

        boolean isCurrentConfigDown = KeyInit.OPEN_CONFIG.isDown();
        if (isCurrentConfigDown && !wasConfigKeyDown) {
            Minecraft.getInstance().setScreen(new ConfigScreen());
        }
        wasConfigKeyDown = isCurrentConfigDown;

        boolean isCurrentAutoDown = KeyInit.TOGGLE_AUTO.isDown();
        if (isCurrentAutoDown && !wasAutoKeyDown) onToggleAuto.run();
        wasAutoKeyDown = isCurrentAutoDown;
    }

    public boolean isOverlayToggleActive() {
        return toggleOverlayActive;
    }

    public boolean isPhysicalKeyDown(KeyMapping keyMapping) {
        if (keyMapping.isUnbound()) return false;
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key boundKey = InputConstants.getKey(keyMapping.saveString());
        int keyCode = boundKey.getValue();

        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, keyCode) == GLFW.GLFW_PRESS;
        } else {
            return InputConstants.isKeyDown(windowHandle, keyCode);
        }
    }
}