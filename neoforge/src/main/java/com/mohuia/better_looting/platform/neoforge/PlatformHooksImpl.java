// 注意：如果你正在做多平台架构，建议把这里的包名也从 forge 改为 neoforge
package com.mohuia.better_looting.platform.neoforge;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.BetterLootingConfig.PickupInterceptMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public class PlatformHooksImpl {
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(player, itemEntity, stack));
    }

    /**
     * AUTO 模式：以 HIGH 优先级的 NeoForge 事件拦截作为辅助防线。
     * 主防线在 ItemEntityMixin#playerTouch HEAD（跨平台统一拦截，确保事件根本不被触发）。
     * 此处仅处理绕过 playerTouch 直接触发 ItemEntityPickupEvent.Pre 的边缘情况。
     */
    public static void setupPickupInterception() {
        NeoForge.EVENT_BUS.register(new NeoForgePickupInterceptor());
    }

    private static class NeoForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onItemPickup(ItemEntityPickupEvent.Pre event) {
            if (BetterLootingConfig.get().pickupInterceptMode != PickupInterceptMode.AUTO) return;
            // 已被其他模组主动接管则不重复拦截
            if (event.canPickup() != TriState.DEFAULT) return;
            event.setCanPickup(TriState.FALSE);
        }
    }
}