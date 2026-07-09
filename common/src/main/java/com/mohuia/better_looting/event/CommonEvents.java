package com.mohuia.better_looting.event;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.BetterLootingConfig.PickupInterceptMode;
import com.mohuia.better_looting.platform.PlatformHooks;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.entity.player.Player;

/**
 * 通用事件注册类
 * 负责在服务端和客户端共同运行的逻辑
 */
public class CommonEvents {

    /**
     * 初始化通用事件
     */
    public static void init() {
        BetterLootingConfig cfg = BetterLootingConfig.get();

        if (cfg.pickupInterceptMode == PickupInterceptMode.ALWAYS) {
            // 始终拦截：无条件阻止原版拾取，完全由模组接管（当前行为，可能与背包类模组冲突）
            PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> EventResult.interruptFalse());
        } else if (cfg.pickupInterceptMode == PickupInterceptMode.AUTO) {
            // 智能模式：使用平台特定的策略，仅在无其他模组处理时才拦截
            PlatformHooks.setupPickupInterception();
        }
    }
}