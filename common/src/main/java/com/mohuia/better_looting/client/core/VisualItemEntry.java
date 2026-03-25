package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.core.ISuperStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 视觉列表项包装类。
 * 用于在 HUD 上将多个相同的 ItemEntity 聚合显示为一条记录。
 */
public class VisualItemEntry {
    /** 组成该视觉项的所有原始掉落物实体列表，发包时将拾取这些实体 */
    private final List<ItemEntity> sourceEntities = new ArrayList<>();
    /** 用于在 UI 上渲染图标和 Tooltip 的代表性 ItemStack */
    private final ItemStack representativeStack;
    /** 合并后的物品总数量（包含超出原版限制的 ISuperStack 数量） */
    private int totalCount = 0;

    /**
     * 根据场景中的实际掉落物创建条目。
     */
    public VisualItemEntry(ItemEntity firstEntity) {
        this.sourceEntities.add(firstEntity);
        this.representativeStack = firstEntity.getItem().copy();

        // 计算总数时同时读取原版 Count 和 Mixin 注入的额外 Count
        ISuperStack superStack = (ISuperStack) firstEntity;
        this.totalCount = this.representativeStack.getCount() + superStack.betterlooting$getExtraCount();
    }

    /**
     * 仅用于 UI 预览或配置界面创建的假条目。
     */
    public VisualItemEntry(ItemStack stack) {
        this.representativeStack = stack.copy();
        this.totalCount = stack.getCount(); // 预览用的是纯 ItemStack，直接读普通数量即可
    }

    /**
     * 尝试将另一个实体合并入当前视觉条目中。
     * @param entity 要合并的掉落物实体
     * @return 如果是相同物品且合并成功，则返回 true；否则返回 false
     */
    public boolean tryMerge(ItemEntity entity) {
        ItemStack otherStack = entity.getItem();

        if (!this.representativeStack.isStackable()) {
            return false;
        }

        // 检查物品类型和 NBT 是否完全一致
        if (ItemStack.isSameItemSameComponents(this.representativeStack, otherStack)) {
            this.sourceEntities.add(entity);

            ISuperStack superStack = (ISuperStack) entity;
            this.totalCount += otherStack.getCount() + superStack.betterlooting$getExtraCount();
            return true;
        }

        return false;
    }

    public ItemStack getItem() { return representativeStack; }

    // getCount() 和 getTotalCount() 返回相同值，保留是为了兼容性
    public int getCount() { return totalCount; }
    public int getTotalCount() { return totalCount; }

    public List<ItemEntity> getSourceEntities() { return sourceEntities; }

    /**
     * 获取第一个关联实体的 ID，主要用于在排序时提供稳定的决胜条件。
     */
    public int getPrimaryId() {
        return sourceEntities.isEmpty() ? -1 : sourceEntities.get(0).getId();
    }
}