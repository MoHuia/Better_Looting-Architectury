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

    public static void setupPickupInterception() {
        NeoForge.EVENT_BUS.register(new NeoForgePickupInterceptor());
    }

    private static class NeoForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onItemPickup(ItemEntityPickupEvent.Pre event) {
            if (BetterLootingConfig.get().pickupInterceptMode == PickupInterceptMode.AUTO) {
                // 仅在其他模组未做决定时才拦截（canPickup 仍为 DEFAULT）
                if (event.canPickup() == TriState.DEFAULT) {
                    event.setCanPickup(TriState.FALSE);
                }
            }
        }
    }
}