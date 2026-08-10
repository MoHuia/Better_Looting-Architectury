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
     * AUTO 模式：在 Forge 事件总线上以 LOWEST 优先级注册拾取拦截器（兜底）。
     * <p>
     * 让默认 NORMAL 优先级的"拾取响应"模组（如精妙背包的拾取升级，它监听 EntityItemPickupEvent
     * 把物品装入背包并取消事件）先处理事件；若事件已被它们取消，BL 直接放行；
     * 若无人处理，BL 才取消事件阻止原版拾取，由 PacketBatchPickup 接管。
     */
    public static void setupPickupInterception() {
        MinecraftForge.EVENT_BUS.register(new ForgePickupInterceptor());
    }

    private static class ForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void onItemPickup(EntityItemPickupEvent event) {
            if (BetterLootingConfig.get().pickupInterceptMode != PickupInterceptMode.AUTO) return;
            // 已被其他模组取消（如精妙背包拾取升级已把物品装入背包）→ 放行，不重复拦截
            if (event.isCanceled()) return;
            // 无人处理 → 兜底取消，阻止原版拾取进入玩家背包
            event.setCanceled(true);
        }
    }
}