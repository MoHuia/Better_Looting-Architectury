package com.mohuia.better_looting.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * ItemEntity 的 Accessor 接口
 * 用于获取掉落物的拾取冷却时间，在自定义拾取逻辑中判断物品是否可以被捡起。
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

    /**
     * 获取物品当前的拾取延迟（Tick）
     */
    @Accessor("pickupDelay")
    int getPickupDelay();
}