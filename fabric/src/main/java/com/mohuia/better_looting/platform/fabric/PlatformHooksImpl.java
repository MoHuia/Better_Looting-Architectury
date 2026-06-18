package com.mohuia.better_looting.platform.fabric;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class PlatformHooksImpl {
    private static final boolean KUBEJS_LOADED = Platform.isModLoaded("kubejs");

    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        if (KUBEJS_LOADED) {
            KubeJSCompat.firePickup(player, itemEntity, stack);
        }
    }

    /**
     * AUTO 模式：Fabric 端通过 Architectury 事件拦截拾取。
     * 由于 Fabric 没有类似 Forge 的优先级事件总线，AUTO 模式在此表现为与 ALWAYS 相同的行为。
     */
    public static void setupPickupInterception() {
        PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> EventResult.interruptFalse());
    }
}