// 注意：forge 必须在 platform 后面！
package com.mohuia.better_looting.platform.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class PlatformHooksImpl {
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        // 抛出 Forge 的标准物品拾取事件
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemPickupEvent(player, itemEntity, stack));
    }
}