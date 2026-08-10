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
     * AUTO 模式：以 LOWEST 优先级的 NeoForge 事件拦截作为兜底。
     * <p>
     * 让默认 NORMAL 优先级的"拾取响应"模组（如精妙背包的拾取升级，它监听 ItemEntityPickupEvent.Pre
     * 把物品装入背包并 setCanPickup(FALSE)）先处理事件；若事件已被它们接管，BL 直接放行；
     * 若无人处理，BL 才设置 canPickup(FALSE) 阻止原版拾取，由 PacketBatchPickup 接管。
     */
    public static void setupPickupInterception() {
        NeoForge.EVENT_BUS.register(new NeoForgePickupInterceptor());
    }

    private static class NeoForgePickupInterceptor {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onItemPickup(ItemEntityPickupEvent.Pre event) {
            if (BetterLootingConfig.get().pickupInterceptMode != PickupInterceptMode.AUTO) return;
            // 已被其他模组主动接管（如精妙背包拾取升级已把物品装入背包）→ 放行
            if (event.canPickup() != TriState.DEFAULT) return;
            // 无人处理 → 兜底阻止原版拾取进入玩家背包
            event.setCanPickup(TriState.FALSE);
        }
    }
}