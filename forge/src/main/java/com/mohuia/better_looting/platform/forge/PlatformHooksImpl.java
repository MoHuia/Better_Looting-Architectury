// 注意：forge 必须在 platform 后面！
package com.mohuia.better_looting.platform.forge;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.BetterLootingConfig.PickupInterceptMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlatformHooksImpl {
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        // 抛出 Forge 的标准物品拾取事件
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemPickupEvent(player, itemEntity, stack));
    }

    /**
     * AUTO 模式：在 Forge 事件总线上以最低优先级注册拾取拦截器。
     * 仅在其他模组均未取消拾取事件时才拦截，从而兼容精妙背包等模组的拾取升级。
     */
    public static void setupPickupInterception() {
        MinecraftForge.EVENT_BUS.register(new ForgePickupInterceptor());
    }

    private static class ForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onItemPickup(EntityItemPickupEvent event) {
            // 仅在 AUTO 模式下生效，且 receiveCanceled=false（默认）保证只有未被取消的事件才会触发此方法
            if (BetterLootingConfig.get().pickupInterceptMode == PickupInterceptMode.AUTO) {
                event.setCanceled(true);
            }
        }
    }
}