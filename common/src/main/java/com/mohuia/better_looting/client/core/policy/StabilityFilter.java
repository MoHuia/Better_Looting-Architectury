package com.mohuia.better_looting.client.core.policy;

import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.config.BetterLootingConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HUD 渲染层的稳定过滤器。
 * 目的：防止被其他模组（如精妙背包磁吸）快速抢走的物品在悬浮窗中闪一下。
 * 物品必须连续存在于扫描范围内达到配置的 tick 阈值，才会被放行到 HUD 渲染。
 * 不影响 LootScanner 的全量扫描和自动拾取逻辑。
 */
public class StabilityFilter {
    /** entityId → 已连续存在的 tick 数 */
    private final Map<Integer, Integer> presenceTicks = new HashMap<>();

    /**
     * 每 tick 调用一次。更新实体存活计数，返回通过过滤的条目。
     * @param items LootScanner 的全量扫描结果
     * @return 存活时间 ≥ 阈值的稳定条目（阈值 ≤ 0 时原样返回）
     */
    public List<VisualItemEntry> tick(List<VisualItemEntry> items) {
        int threshold = BetterLootingConfig.get().stabilityThresholdTicks;
        if (threshold <= 0) return items;

        // 收集本 tick 在范围内的实体 ID
        Set<Integer> currentIds = new HashSet<>();
        for (VisualItemEntry entry : items) {
            currentIds.add(entry.getPrimaryId());
        }

        // 递增存活计数
        for (int id : currentIds) {
            presenceTicks.merge(id, 1, Integer::sum);
        }

        // 清理已离开扫描范围的实体
        presenceTicks.keySet().removeIf(id -> !currentIds.contains(id));

        // 过滤：仅保留主实体存活时间达标的条目
        return items.stream()
                .filter(e -> presenceTicks.getOrDefault(e.getPrimaryId(), 0) >= threshold)
                .collect(Collectors.toList());
    }

    /** 清空所有状态（用于世界切换等场景） */
    public void reset() {
        presenceTicks.clear();
    }
}
