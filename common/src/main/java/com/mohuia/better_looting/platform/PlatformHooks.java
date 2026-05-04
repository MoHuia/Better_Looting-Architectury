package com.mohuia.better_looting.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class PlatformHooks {
    /**
     * 手动触发物品拾取事件，兼容其他模组
     */
    @ExpectPlatform
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        throw new AssertionError();
    }
}