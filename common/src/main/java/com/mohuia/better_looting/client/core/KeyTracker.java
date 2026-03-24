package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.config.ConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键追踪器。
 * 用于实现按键的“边缘检测”（Edge Detection），确保长按时不会导致反复触发切换（Toggle）事件。
 */
public class KeyTracker {
    // 持续开启/关闭界面的切换状态
    private boolean toggleOverlayActive = false;

    // 记录上一帧的按键状态，用于边缘检测
    private boolean wasToggleKeyDown = false;
    private boolean wasFilterKeyDown = false;
    private boolean wasConfigKeyDown = false;
    private boolean wasAutoKeyDown = false;

    /**
     * 每一帧检测 HUD 切换键，玩家按下瞬间反转状态。
     */
    public void tickOverlayToggle() {
        boolean isCurrentToggleDown = KeyInit.SHOW_OVERLAY.isDown();
        // 只有在“当前帧按键按下，且上一帧未按下”时，才触发状态翻转
        if (isCurrentToggleDown && !wasToggleKeyDown) {
            toggleOverlayActive = !toggleOverlayActive;
        }
        wasToggleKeyDown = isCurrentToggleDown;
    }

    /**
     * 每一帧检测功能切换键（过滤器、打开配置、自动模式）。
     * 将具体的行为封装进 Runnable 回调中。
     * @param onToggleFilter 切换过滤器的回调
     * @param onToggleAuto   切换自动模式的回调
     */
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

    /**
     * 绕过 Minecraft 的 KeyMapping 冷却机制，直接向 GLFW 轮询物理按键真实状态。
     * 非常适合用于需要检测“一直按住”状态的逻辑（例如持续长按触发批量拾取）。
     * @param keyMapping 需要检测的键位映射
     * @return 物理键/鼠标键当前是否被真正按下
     */
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