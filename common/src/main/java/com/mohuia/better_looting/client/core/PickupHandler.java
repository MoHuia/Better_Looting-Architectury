package com.mohuia.better_looting.client.core;

import net.minecraft.util.Mth;

/**
 * 拾取输入状态机。
 * 负责区分玩家是“短按”（单次拾取）还是“长按”（批量拾取），并管理自动拾取的冷却。
 */
public class PickupHandler {
    private static final int MAX_HOLD_TICKS = 12;      // 长按阈值：12 ticks (约 0.6 秒) 触发批量拾取
    private static final int PRESS_THRESHOLD_TICKS = 1; // 按下即视为开始交互
    private static final int AUTO_COOLDOWN_MAX = 2;     // 自动拾取冷却时间 (ticks)

    private int ticksHeld = 0;
    private int autoPickupCooldown = 0;
    private boolean wasKeyDown = false;
    private boolean batchPickupTriggered = false;

    public enum PickupAction { NONE, SINGLE, BATCH }

    /**
     * 每帧更新输入状态。
     * @param isKeyDown 当前拾取键是否被按下
     * @param hasTargets 当前周围是否有可拾取目标（HUD是否激活）
     * @return 当前帧应该执行的拾取动作
     */
    public PickupAction tickInput(boolean isKeyDown, boolean hasTargets) {
        PickupAction action = PickupAction.NONE;
        if (autoPickupCooldown > 0) autoPickupCooldown--;

        if (isKeyDown) {
            // 刚按下的第一帧
            if (!wasKeyDown) {
                ticksHeld = 0;
                batchPickupTriggered = false;
            } else if (hasTargets && !batchPickupTriggered) {
                // 持续按住，增加进度
                ticksHeld++;
                if (ticksHeld >= MAX_HOLD_TICKS) {
                    action = PickupAction.BATCH;
                    batchPickupTriggered = true; // 确保一次长按只触发一次批量
                }
            }
        } else {
            // 松开按键时，如果没有触发过批量，且有目标，则执行单次拾取
            if (wasKeyDown && !batchPickupTriggered && hasTargets) {
                action = PickupAction.SINGLE;
            }
            // 重置状态
            ticksHeld = 0;
            batchPickupTriggered = false;
        }

        wasKeyDown = isKeyDown;
        return action;
    }

    /**
     * @return 获取当前长按的进度 (0.0 到 1.0)，可用于渲染 UI 进度条（如圆环）
     */
    public float getProgress() {
        if (ticksHeld < 2) return 0.0f; // 轻微防抖，避免刚点按就闪烁进度条
        if (batchPickupTriggered) return 1.0f;
        return Mth.clamp((float)ticksHeld / MAX_HOLD_TICKS, 0.0f, 1.0f);
    }

    public boolean canAutoPickup() { return autoPickupCooldown <= 0; }
    public void resetAutoCooldown() { this.autoPickupCooldown = 0; }
    public void onAutoPickupTriggered() { this.autoPickupCooldown = AUTO_COOLDOWN_MAX; }
    public boolean isInteracting() { return ticksHeld > 0; }
}