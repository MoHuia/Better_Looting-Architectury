package com.mohuia.better_looting.client.core.policy;

import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.core.pipeline.KeyTracker;
import com.mohuia.better_looting.client.core.pipeline.SelectionManager;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.ConfigScreen;
import net.minecraft.client.Minecraft;

/**
 * 激活策略，判断 HUD 是否应显示以及滚轮是否应被模组拦截。
 * 纯逻辑，无状态。
 */
public class ActivationPolicy {

    /**
     * 判断当前是否应该在屏幕上渲染拾取 HUD。
     */
    public static boolean isHudActive(SelectionManager selectionManager, KeyTracker keyTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || selectionManager.getNearbyItems().isEmpty()) return false;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        return switch (cfg.activationMode) {
            case ALWAYS -> true;
            case LOOK_DOWN -> mc.player.getXRot() >= cfg.lookDownAngle;
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001;
            case KEY_HOLD -> KeyInit.SHOW_OVERLAY.isDown();
            case KEY_TOGGLE -> keyTracker.isOverlayToggleActive();
        };
    }

    /**
     * 判断是否应该忽略鼠标滚轮事件。
     * @return true 表示忽略模组滚动（交还给原版），false 表示模组拦截滚动。
     */
    public static boolean shouldIgnoreScroll(SelectionManager selectionManager, KeyTracker keyTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof ConfigScreen)) return true;
        // 自动拾取模式下悬浮窗隐藏，滚轮无操作对象，应交还原版（保持与 Overlay 显示条件同步）
        if (ModeManager.INSTANCE.isAutoMode()) return true;
        if (!isHudActive(selectionManager, keyTracker)) return true;
        if (selectionManager.getNearbyItems().size() <= 1) return true;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        return switch (cfg.scrollMode) {
            case ALWAYS -> false;
            case INVERT_KEY -> KeyInit.SCROLL_MODIFIER.isDown();
            case KEY_BIND -> !KeyInit.SCROLL_MODIFIER.isDown();
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        };
    }
}
