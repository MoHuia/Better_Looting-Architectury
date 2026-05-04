package com.mohuia.better_looting.platform.fabric;

import dev.architectury.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class PlatformHooksImpl {
    public static void fireItemPickupEvent(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        // Fabric API 原生没有对应的 Event。
        // 如果你需要兼容 Fabric 端的 KubeJS，建议检测其是否加载并使用反射/可选依赖触发：
        if (Platform.isModLoaded("kubejs")) {
            KubeJSCompat.firePickup(player, itemEntity, stack);
        }
    }
}