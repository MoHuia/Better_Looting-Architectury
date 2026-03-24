package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.config.BetterLootingConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * UI 选中状态管理器。
 * 负责管理附近物品的列表索引、处理玩家的滚轮事件，以及计算 HUD 滚动条的可视范围。
 */
public class SelectionManager {
    /** 当前扫描到并整合好的附近物品条目列表 */
    private List<VisualItemEntry> nearbyItems = new ArrayList<>();
    /** 当前玩家高亮选中的物品索引位置 */
    private int selectedIndex = 0;
    /** HUD 渲染时的顶部偏移量（用来实现长列表的“滑动窗口”滚动效果） */
    private int targetScrollOffset = 0;

    /**
     * 更新检测到的物品列表，并重新校验选中状态防止越界。
     * 通常由 LootScanner 扫描完毕后在 ClientTick 中调用。
     * @param items 最新的可视物品列表
     */
    public void updateItems(List<VisualItemEntry> items) {
        this.nearbyItems = items;
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
    public List<VisualItemEntry> getNearbyItems() { return nearbyItems; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getTargetScrollOffset() { return targetScrollOffset; }
}