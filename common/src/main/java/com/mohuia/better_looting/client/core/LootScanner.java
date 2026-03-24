package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.config.BetterLootingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

import java.util.*;


public class LootScanner {

    private static final Comparator<VisualItemEntry> VISUAL_COMPARATOR = (e1, e2) -> {
        ItemStack s1 = e1.getItem();
        ItemStack s2 = e2.getItem();

        int rDiff = s2.getRarity().ordinal() - s1.getRarity().ordinal();
        if (rDiff != 0) return rDiff;

        boolean enc1 = s1.isEnchanted();
        boolean enc2 = s2.isEnchanted();
        if (enc1 != enc2) return enc1 ? -1 : 1;

        int nameDiff = s1.getHoverName().getString().compareTo(s2.getHoverName().getString());
        if (nameDiff != 0) return nameDiff;

        int countDiff = Integer.compare(e2.getTotalCount(), e1.getTotalCount());
        if (countDiff != 0) return countDiff;

        return Integer.compare(e1.getPrimaryId(), e2.getPrimaryId());
    };

    public static List<VisualItemEntry> scan(Minecraft mc, Core.FilterMode filterMode) {
        if (mc.player == null || mc.level == null) return new ArrayList<>();

        BetterLootingConfig cfg = BetterLootingConfig.get();
        double expandXZ = cfg.scanRangeXZ;
        double expandY = cfg.scanRangeY;

        AABB area = mc.player.getBoundingBox().inflate(expandXZ, expandY, expandXZ);

        List<ItemEntity> rawEntities = mc.level.getEntitiesOfClass(ItemEntity.class, area, entity ->
                entity.isAlive() && !entity.getItem().isEmpty()
        );

        List<VisualItemEntry> unstackableList = new ArrayList<>();
        Map<MergeKey, VisualItemEntry> mergedMap = new HashMap<>();

        for (ItemEntity entity : rawEntities) {
            ItemStack stack = entity.getItem();

            if (filterMode == Core.FilterMode.RARE_ONLY && shouldHide(stack)) {
                continue;
            }

            if (!stack.isStackable()) {
                unstackableList.add(new VisualItemEntry(entity));
            } else {
                MergeKey key = new MergeKey(stack);
                mergedMap.compute(key, (k, existingEntry) -> {
                    if (existingEntry == null) {
                        return new VisualItemEntry(entity);
                    } else {
                        existingEntry.tryMerge(entity);
                        return existingEntry;
                    }
                });
            }
        }

        List<VisualItemEntry> finalResult = new ArrayList<>(unstackableList.size() + mergedMap.size());
        finalResult.addAll(unstackableList);
        finalResult.addAll(mergedMap.values());
        finalResult.sort(VISUAL_COMPARATOR);

        return finalResult;
    }

    private static boolean shouldHide(ItemStack stack) {
        if (FilterWhitelist.INSTANCE.contains(stack)) return false;
        return stack.getRarity() == Rarity.COMMON
                && !stack.isEnchanted()
                && !Utils.shouldShowTooltip(stack);
    }

    private static class MergeKey {
        private final Item item;
        private final CompoundTag tag;

        public MergeKey(ItemStack stack) {
            this.item = stack.getItem();
            this.tag = stack.getTag();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MergeKey mergeKey = (MergeKey) o;
            return item == mergeKey.item && Objects.equals(tag, mergeKey.tag);
        }

        @Override
        public int hashCode() {
            if (tag == null) return item.hashCode();
            return 31 * item.hashCode() + tag.hashCode();
        }
    }
}