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
     * AUTO 模式：在 Forge 事件总线上以 HIGH 优先级注册拾取拦截器。
     * 高于默认 NORMAL 优先级，确保在拾取提示类观察者模组之前取消事件。
     * receiveCanceled=true 配合 isCanceled() 检查，保证已被其他模组取消的事件不会被重复拦截，
     * 从而兼容精妙背包等模组的拾取升级（它们一般不通过 EntityItemPickupEvent 工作，但以防万一）。
     */
    public static void setupPickupInterception() {
        MinecraftForge.EVENT_BUS.register(new ForgePickupInterceptor());
    }

    private static class ForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
        public void onItemPickup(EntityItemPickupEvent event) {
            if (BetterLootingConfig.get().pickupInterceptMode != PickupInterceptMode.AUTO) return;
            // 如果已被其他模组取消（说明有模组正在主动接管拾取），则不重复拦截
            if (event.isCanceled()) return;
            // 以 HIGH 优先级取消，使得 NORMAL 优先级的观察者模组（如 pick-up-notifier）不再收到此事件
            event.setCanceled(true);
        }
    }
}