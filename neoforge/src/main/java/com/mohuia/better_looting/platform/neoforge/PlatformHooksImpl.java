// 注意：如果你正在做多平台架构，建议把这里的包名也从 forge 改为 neoforge
package com.mohuia.better_looting.platform.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent; // 替换了旧的 PlayerEvent 导入

public class PlatformHooksImpl {
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        // 在较新的 NeoForge 中，拾取完成事件被重构为 ItemEntityPickupEvent.Post
        // 参数依然是 (player, itemEntity, originalStack) 完美对应
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(player, itemEntity, stack));
    }
}