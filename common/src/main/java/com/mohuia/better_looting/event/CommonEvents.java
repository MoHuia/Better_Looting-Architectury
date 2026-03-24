package com.mohuia.better_looting.event;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent; // 注意这里导入的是 PlayerEvent
import net.minecraft.world.entity.player.Player;

public class CommonEvents {
    public static void init() {
        PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> {
            return EventResult.interruptFalse();
        });
    }
}