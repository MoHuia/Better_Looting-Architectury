package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.config.BetterLootingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 掉落物扫描器。
 * 负责获取玩家周围的 ItemEntity，将其合并为用于 UI 渲染的 VisualItemEntry，并根据规则进行过滤和排序。
 */
public class LootScanner {

    /**
     * UI 显示项的排序比较器。
     * 排序权重依次为：稀有度(高在前) -> 是否附魔(是则靠前) -> 物品名称(字母序) -> 总数量(多在前) -> 实体ID(用于同类物品的稳定排序)
     */
    private static final Comparator<VisualItemEntry> VISUAL_COMPARATOR = (e1, e2) -> {
        ItemStack s1 = e1.getItem();
        ItemStack s2 = e2.getItem();

        // 1. 根据物品稀有度排序
        int rDiff = s2.getRarity().ordinal() - s1.getRarity().ordinal();
        if (rDiff != 0) return rDiff;

        // 2. 将有附魔的物品排在前面
        boolean enc1 = s1.isEnchanted();
        boolean enc2 = s2.isEnchanted();
        if (enc1 != enc2) return enc1 ? -1 : 1;

        // 3. 根据物品显示名称按字典序排序
        int nameDiff = s1.getHoverName().getString().compareTo(s2.getHoverName().getString());
        if (nameDiff != 0) return nameDiff;

        // 4. 将数量最多的堆叠排在前面
        int countDiff = Integer.compare(e2.getTotalCount(), e1.getTotalCount());
        if (countDiff != 0) return countDiff;

        // 5. 最终保障：按第一个对应实体的 ID 排序，确保 UI 列表不闪烁跳动
        return Integer.compare(e1.getPrimaryId(), e2.getPrimaryId());
    };

    /**
     * 执行环境扫描，提取并合并玩家周围的可拾取掉落物。
     *
     * @param mc         当前 Minecraft 客户端实例
     * @param filterMode 过滤器模式（全部扫描 / 仅稀有）
     * @return 经过处理并排序后的可视化列表项
     */
    public static List<VisualItemEntry> scan(Minecraft mc, Core.FilterMode filterMode) {
        if (mc.player == null || mc.level == null) return new ArrayList<>();

        BetterLootingConfig cfg = BetterLootingConfig.get();
        double expandXZ = cfg.scanRangeXZ;
        double expandY = cfg.scanRangeY;

        // 以玩家的碰撞箱为中心向外扩散扫描范围
        AABB area = mc.player.getBoundingBox().inflate(expandXZ, expandY, expandXZ);

        // 获取该范围内所有存活且物品不为空的 ItemEntity
        List<ItemEntity> rawEntities = mc.level.getEntitiesOfClass(ItemEntity.class, area, entity ->
                entity.isAlive() && !entity.getItem().isEmpty()
        );

        List<VisualItemEntry> unstackableList = new ArrayList<>();
        // 使用哈希表对可堆叠物品进行合并（例如将 3 个分散的圆石合并为一个显示项）
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
                        existingEntry.tryMerge(entity); // 尝试累加数量和记录实体源
                        return existingEntry;
                    }
                });
            }
        }

        // 汇总不可堆叠和已合并的可堆叠项，并执行最终排序
        List<VisualItemEntry> finalResult = new ArrayList<>(unstackableList.size() + mergedMap.size());
        finalResult.addAll(unstackableList);
        finalResult.addAll(mergedMap.values());
        finalResult.sort(VISUAL_COMPARATOR);

        return finalResult;
    }

    /**
     * 判定某个物品在“仅稀有模式”下是否应该被隐藏。
     */
    private static boolean shouldHide(ItemStack stack) {
        if (FilterWhitelist.INSTANCE.contains(stack)) return false;

        // 隐藏那些稀有度为普通、没有附魔、也没有特殊 Tooltip（比如带词缀/属性修改）的物品
        return stack.getRarity() == Rarity.COMMON
                && !stack.isEnchanted()
                && !Utils.shouldShowTooltip(stack);
    }

    /**
     * 内部辅助类：作为哈希键值对合并可堆叠的物品实体。
     * 只比较物品类型（Item）和其 NBT 标签（CompoundTag）。
     */
    private static class MergeKey {
        private final Item item;
        private final DataComponentPatch components;

        public MergeKey(ItemStack stack) {
            this.item = stack.getItem();
            this.components = stack.getComponentsPatch();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MergeKey mergeKey = (MergeKey) o;
            return item == mergeKey.item && Objects.equals(components, mergeKey.components);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, components);
        }
    }
}