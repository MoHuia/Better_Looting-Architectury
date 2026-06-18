package com.mohuia.better_looting.client.core.pipeline;

import com.mohuia.better_looting.client.core.policy.StabilityFilter;
import com.mohuia.better_looting.config.BetterLootingConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * UI 选中状态管理器。
 * 负责管理附近物品的列表索引、处理玩家的滚轮事件，以及计算 HUD 滚动条的可视范围。
 * 内置 StabilityFilter，确保进入 HUD 渲染的物品已经稳定存在一段时间。
 */
public class SelectionManager {
    private final StabilityFilter stabilityFilter = new StabilityFilter();
    /** 全量扫描结果（未经过滤，用于自动拾取等需要即时响应的逻辑） */
    private List<VisualItemEntry> unfilteredItems = new ArrayList<>();
    /** HUD 显示用的稳定过滤结果 */
    private List<VisualItemEntry> nearbyItems = new ArrayList<>();
    /** 当前玩家高亮选中的物品索引位置 */
    private int selectedIndex = 0;
    /** HUD 渲染时的顶部偏移量（用来实现长列表的”滑动窗口”滚动效果） */
    private int targetScrollOffset = 0;

    /**
     * 更新检测到的物品列表，并重新校验选中状态防止越界。
     * 扫描结果经过 StabilityFilter 过滤后才进入 HUD 渲染管线。
     * @param items 最新的全量扫描结果
     */
    public void updateItems(List<VisualItemEntry> items) {
        this.unfilteredItems = items;
        this.nearbyItems = stabilityFilter.tick(items);
        validateSelection();
    }

    /**
     * 处理来自客户端鼠标滚轮的输入信号。
     * @param delta 滚轮的滚动量（通常 delta > 0 为向上滚，delta < 0 为向下滚）
     */
    public void performScroll(double delta) {
        if (nearbyItems.isEmpty()) return;

        // 向上滚(delta > 0)时索引减小（往上走），向下滚时索引增加（往下走）
        selectedIndex += (delta > 0) ? -1 : 1;
        validateSelection();
    }

    /**
     * 核心校验逻辑：
     * 1. 处理空列表。
     * 2. 处理循环滚动（Wrap Around）：滚到顶继续滚会跳到底部，反之亦然。
     * 3. 视野窗口（View Window）跟踪：确保当前选中的项目始终在 HUD 的可视行数范围内。
     */
    private void validateSelection() {
        int size = nearbyItems.size();

        // 当周围没有可拾取物品时，重置所有状态
        if (size == 0) {
            selectedIndex = 0;
            targetScrollOffset = 0;
            return;
        }

        // 允许循环滚动逻辑
        if (selectedIndex < 0) selectedIndex = size - 1;   // 越过顶部，跳到底部
        if (selectedIndex >= size) selectedIndex = 0;      // 越过底部，跳到顶部

        // 获取用户配置中允许 HUD 显示的最大行数
        double visibleRows = BetterLootingConfig.get().visibleRows;

        // 核心滚动视图逻辑：当列表项超出一页显示范围时，调整 offset 让选中项始终可见
        if (size > visibleRows) {
            // 如果选中项超出了屏幕底端，向下移动视野窗口（增加 offset）
            if (selectedIndex + 1 > targetScrollOffset + visibleRows) {
                targetScrollOffset = (int) Math.ceil(selectedIndex - visibleRows + 1);
            }
            // 如果选中项超出了屏幕顶端，向上移动视野窗口（减小 offset）
            if (selectedIndex < targetScrollOffset) {
                targetScrollOffset = selectedIndex;
            }
        } else {
            // 如果总物品数少于最大显示行数，不需要滚动，偏移量归零
            targetScrollOffset = 0;
        }
    }

    // --- Getter 方法，主要供 HUD 渲染层 (GUI) 读取状态 ---
    /** 返回经 StabilityFilter 过滤后的稳定条目列表（HUD 渲染用） */
    public List<VisualItemEntry> getNearbyItems() { return nearbyItems; }
    /** 返回全量未过滤条目列表（自动拾取/F键拾取用，确保即时响应） */
    public List<VisualItemEntry> getUnfilteredItems() { return unfilteredItems; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getTargetScrollOffset() { return targetScrollOffset; }
}