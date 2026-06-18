package com.mohuia.better_looting.client.core.policy;

import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.config.BetterLootingConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HUD 渲染层的稳定过滤器。
 * 防止被其他模组快速抢走的物品在悬浮窗中闪一下。
 */
public class StabilityFilter {
    private final Map<Integer, Integer> presenceTicks = new HashMap<>();

    public List<VisualItemEntry> tick(List<VisualItemEntry> items) {
        int threshold = BetterLootingConfig.get().stabilityThresholdTicks;
        if (threshold <= 0) return items;

        Set<Integer> currentIds = new HashSet<>();
        for (VisualItemEntry entry : items) {
            currentIds.add(entry.getPrimaryId());
        }

        for (int id : currentIds) {
            presenceTicks.merge(id, 1, Integer::sum);
        }

        presenceTicks.keySet().removeIf(id -> !currentIds.contains(id));

        return items.stream()
                .filter(e -> presenceTicks.getOrDefault(e.getPrimaryId(), 0) >= threshold)
                .collect(Collectors.toList());
    }

    public void reset() {
        presenceTicks.clear();
    }
}
