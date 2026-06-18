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

    public static void setupPickupInterception() {
        PlayerEvent.PICKUP_ITEM_PRE.register((player, itemEntity, stack) -> EventResult.interruptFalse());
    }
}