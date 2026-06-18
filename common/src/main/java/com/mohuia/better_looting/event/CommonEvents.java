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
            PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> EventResult.interruptFalse());
        } else if (cfg.pickupInterceptMode == PickupInterceptMode.AUTO) {
            PlatformHooks.setupPickupInterception();
        }
    }
}