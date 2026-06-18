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

    /**
     * 注册平台特定的拾取拦截策略（用于 AUTO 模式）。
     * Forge: 注册低优先级 EntityItemPickupEvent 监听器，仅在无其他模组处理时拦截。
     * Fabric: 注册 Architectury PICKUP_ITEM_PRE 事件，无条件拦截。
     */
    @ExpectPlatform
    public static void setupPickupInterception() {
        throw new AssertionError();
    }
}