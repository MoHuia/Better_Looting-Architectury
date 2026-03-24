package com.mohuia.better_looting.client.core;

import net.minecraft.util.Mth;

public class PickupHandler {
    private static final int MAX_HOLD_TICKS = 12;      // 长按 0.6s 触发批量
    private static final int PRESS_THRESHOLD_TICKS = 1; // 只要按下就视为开始交互
    private static final int AUTO_COOLDOWN_MAX = 2;

    private int ticksHeld = 0;
    private int autoPickupCooldown = 0;
    private boolean wasKeyDown = false;
    private boolean batchPickupTriggered = false;

    public enum PickupAction { NONE, SINGLE, BATCH }

    public PickupAction tickInput(boolean isKeyDown, boolean hasTargets) {
        PickupAction action = PickupAction.NONE;
        if (autoPickupCooldown > 0) autoPickupCooldown--;

        if (isKeyDown) {
            if (!wasKeyDown) {
                ticksHeld = 0;
                batchPickupTriggered = false;
            } else if (hasTargets && !batchPickupTriggered) {
                ticksHeld++;
                if (ticksHeld >= MAX_HOLD_TICKS) {
                    action = PickupAction.BATCH;
                    batchPickupTriggered = true;
                }
            }
        } else {
            if (wasKeyDown && !batchPickupTriggered && hasTargets) {
                action = PickupAction.SINGLE;
            }
            ticksHeld = 0;
            batchPickupTriggered = false;
        }

        wasKeyDown = isKeyDown;
        return action;
    }

    public float getProgress() {
        if (ticksHeld < 2) return 0.0f;
        if (batchPickupTriggered) return 1.0f;
        return Mth.clamp((float)ticksHeld / MAX_HOLD_TICKS, 0.0f, 1.0f);
    }

    public boolean canAutoPickup() { return autoPickupCooldown <= 0; }
    public void resetAutoCooldown() { this.autoPickupCooldown = 0; }
    public void onAutoPickupTriggered() { this.autoPickupCooldown = AUTO_COOLDOWN_MAX; }
    public boolean isInteracting() { return ticksHeld > 0; }
}