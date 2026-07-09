package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.core.pipeline.KeyTracker;
import com.mohuia.better_looting.client.core.pipeline.PickupHandler;
import com.mohuia.better_looting.client.core.pipeline.SelectionManager;
import com.mohuia.better_looting.client.core.policy.ActivationPolicy;
import net.minecraft.client.Minecraft;

/**
 * 输入守卫，负责防止按键穿透以及管理拾取后的拦截缓冲期。
 * 单例模式，在 Core.init() 时注入依赖。
 */
public class InputGuard {
    public static final InputGuard INSTANCE = new InputGuard();

    private int interceptGraceTicks = 0;
    private boolean isHoldingInterceptedKey = false;

    private SelectionManager selectionManager;
    private KeyTracker keyTracker;
    private PickupHandler pickupHandler;

    private InputGuard() {}

    public void init(SelectionManager sm, KeyTracker kt, PickupHandler ph) {
        this.selectionManager = sm;
        this.keyTracker = kt;
        this.pickupHandler = ph;
    }

    /**
     * 每 tick 执行拦截逻辑：消耗原版冲突按键、管理长按锁定状态。
     */
    public void tick(boolean isPhysicalDown) {
        if (!isPhysicalDown) {
            isHoldingInterceptedKey = false;
        }

        if (shouldIntercept() || isHoldingInterceptedKey) {
            suppressVanillaOverlappingKeys();
            if (isPhysicalDown) {
                isHoldingInterceptedKey = true;
            }
        }

        if (interceptGraceTicks > 0) {
            interceptGraceTicks--;
        }
    }

    /**
     * 判断模组是否应该拦截特定游戏交互（例如防止在拾取时错误攻击/右键）。
     */
    public boolean shouldIntercept() {
        return ActivationPolicy.isHudActive(selectionManager, keyTracker)
                || pickupHandler.isInteracting()
                || interceptGraceTicks > 0;
    }

    public void setGraceTicks(int ticks) {
        this.interceptGraceTicks = ticks;
    }

    /**
     * 动态消耗与拾取键重合的原版按键事件，防止"按键穿透"。
     */
    private void suppressVanillaOverlappingKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (KeyInit.PICKUP.same(mc.options.keySwapOffhand) || KeyInit.PICKUP_ALT.same(mc.options.keySwapOffhand)) {
            while (mc.options.keySwapOffhand.consumeClick()) {}
        }
        if (KeyInit.PICKUP.same(mc.options.keyDrop) || KeyInit.PICKUP_ALT.same(mc.options.keyDrop)) {
            while (mc.options.keyDrop.consumeClick()) {}
        }
    }
}
