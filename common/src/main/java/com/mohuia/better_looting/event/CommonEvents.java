package com.mohuia.better_looting.event;

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
        // 拦截玩家拾取物品的预处理事件
        // 返回 interruptFalse() 会阻止原版的默认拾取行为，从而允许模组接管并使用自定义的拾取逻辑
        PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> {
            return EventResult.interruptFalse();
        });
    }
}