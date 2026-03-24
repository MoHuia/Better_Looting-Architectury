package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.core.ISuperStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class VisualItemEntry {
    private final List<ItemEntity> sourceEntities = new ArrayList<>();
    private final ItemStack representativeStack;
    private int totalCount = 0;

    public VisualItemEntry(ItemEntity firstEntity) {
        this.sourceEntities.add(firstEntity);
        this.representativeStack = firstEntity.getItem().copy();

        ISuperStack superStack = (ISuperStack) firstEntity;
        this.totalCount = this.representativeStack.getCount() + superStack.betterlooting$getExtraCount();
    }


    public VisualItemEntry(ItemStack stack) {
        this.representativeStack = stack.copy();
        this.totalCount = stack.getCount(); // 预览用的是纯 ItemStack，直接读普通数量即可
    }

    public boolean tryMerge(ItemEntity entity) {
        ItemStack otherStack = entity.getItem();

        if (!this.representativeStack.isStackable()) {
            return false;
        }

        if (ItemStack.isSameItemSameTags(this.representativeStack, otherStack)) {
            this.sourceEntities.add(entity);

            ISuperStack superStack = (ISuperStack) entity;
            this.totalCount += otherStack.getCount() + superStack.betterlooting$getExtraCount();
            return true;
        }

        return false;
    }

    public ItemStack getItem() { return representativeStack; }

    public int getCount() { return totalCount; }
    public int getTotalCount() { return totalCount; }

    public List<ItemEntity> getSourceEntities() { return sourceEntities; }

    public int getPrimaryId() {
        return sourceEntities.isEmpty() ? -1 : sourceEntities.get(0).getId();
    }
}