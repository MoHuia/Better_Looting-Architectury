package com.mohuia.better_looting.network;

import com.mohuia.better_looting.config.BetterLootingConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** 服务端拾取边界；不读取客户端同步覆盖值，也不接受客户端提供的距离。 */
public final class PickupRangeRules {
    public static final int MAX_ENTITY_IDS = 1024;

    private PickupRangeRules() {}

    public static boolean isValid(float xz, float y) {
        return Float.isFinite(xz) && Float.isFinite(y)
                && xz >= 0.5f && xz <= 8.0f && y >= 0.5f && y <= 5.0f;
    }

    public static boolean contains(AABB playerBox, AABB itemBox, float xz, float y) {
        return isValid(xz, y) && playerBox.inflate(xz, y, xz).intersects(itemBox);
    }

    public static boolean canReach(ServerPlayer player, ItemEntity item) {
        if (!player.isAlive() || player.isSpectator() || !item.isAlive() || item.level() != player.level()) return false;
        BetterLootingConfig config = BetterLootingConfig.get();
        return contains(player.getBoundingBox(), item.getBoundingBox(), config.scanRangeXZ, config.scanRangeY);
    }
}
