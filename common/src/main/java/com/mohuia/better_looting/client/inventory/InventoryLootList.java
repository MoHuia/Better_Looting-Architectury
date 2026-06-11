package com.mohuia.better_looting.client.inventory;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.client.overlay.OverlayRenderer;
import com.mohuia.better_looting.client.overlay.OverlayState;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.mixin.ACSAccessor;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import com.mohuia.better_looting.network.C2S.PacketPlaceIntoSlot;
import com.mohuia.better_looting.network.NetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在玩家物品栏左侧渲染当前附近掉落物列表，支持滚轮滚动、滚动条拖拽，
 * 以及单击拾取 / 长按拖拽到物品栏指定槽位。
 */
public class InventoryLootList {
    public static final InventoryLootList INSTANCE = new InventoryLootList();
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_TRACK_X_OFFSET = Constants.LIST_X - SCROLLBAR_WIDTH - 2;
    private static final int ITEM_HEIGHT_TOTAL = Constants.ITEM_HEIGHT + 2;
    private static final int ENTRY_STAGGER_MS = 40;
    private static final float ENTRY_SPEED = 6.0f;
    private static final int DRAG_THRESHOLD_MS = 200;
    private static final double DRAG_THRESHOLD_PX = 4.0;

    private OverlayRenderer renderer;
    private List<VisualItemEntry> nearbyItems = List.of();

    // 滚动物理
    private OverlayState scrollState = new OverlayState();
    private float targetScroll = 0f;
    private boolean isDraggingScrollbar = false;

    // 物品入场动画
    private final Map<Integer, Long> entryTimes = new HashMap<>();
    private boolean justOpened = false;

    // 物品拖拽
    private int dragIndex = -1;
    private double dragStartX, dragStartY, dragCurrentX, dragCurrentY;
    private long dragStartMs;
    private boolean dragModeActive = false;
    private boolean isDraggingItem = false;

    // 缓存布局
    private int cachedPanelStartX;
    private int cachedTopPos;
    private int cachedImageHeight;
    private int cachedPanelWidth;
    private float cachedVisibleRows;
    private float cachedMaxScroll;

    private InventoryLootList() {}

    public void init() {
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            if (screen instanceof InventoryScreen) {
                resetScroll();
            }
        });

        ClientGuiEvent.RENDER_POST.register((screen, gui, mouseX, mouseY, delta) -> {
            if (screen instanceof InventoryScreen invScreen) {
                render(gui, invScreen, (int) mouseX, (int) mouseY);
            }
        });
    }

    private void resetScroll() {
        scrollState = new OverlayState();
        targetScroll = 0f;
        isDraggingScrollbar = false;
        entryTimes.clear();
        justOpened = true;
        dragIndex = -1;
        isDraggingItem = false;
        dragModeActive = false;
    }

    private void render(GuiGraphics gui, InventoryScreen screen, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Core core = Core.INSTANCE;
        nearbyItems = core.getNearbyItems();
        if (nearbyItems == null || nearbyItems.isEmpty()) return;

        if (this.renderer == null) {
            this.renderer = new OverlayRenderer(mc);
        }

        // === 布局 ===
        BetterLootingConfig cfg = BetterLootingConfig.get();
        ACSAccessor acc = (ACSAccessor) screen;
        int leftPos = acc.getLeftPos();
        int topPos = acc.getTopPos();
        int imageHeight = acc.getImageHeight();
        int gap = 2;
        int maxPanelWidth = leftPos - 2 - gap - Constants.LIST_X;
        if (maxPanelWidth < 80) return;
        int panelWidth = Math.min(cfg.inventoryListWidth, maxPanelWidth);
        int panelStartX = leftPos - Constants.LIST_X - panelWidth - gap;
        float visibleRows = (float) imageHeight / ITEM_HEIGHT_TOTAL;

        this.cachedPanelStartX = panelStartX;
        this.cachedTopPos = topPos;
        this.cachedImageHeight = imageHeight;
        this.cachedPanelWidth = panelWidth;
        this.cachedVisibleRows = visibleRows;
        this.cachedMaxScroll = Math.max(0, nearbyItems.size() - visibleRows);

        // === 滚动物理 ===
        scrollState.tick(true, targetScroll, nearbyItems.size(), visibleRows);
        float scrollValue = scrollState.currentScroll;

        // === 入场动画 ===
        long now = Util.getMillis();
        Set<Integer> currentIds = nearbyItems.stream()
                .map(VisualItemEntry::getPrimaryId)
                .collect(Collectors.toSet());

        if (justOpened) {
            for (int i = 0; i < nearbyItems.size(); i++) {
                entryTimes.putIfAbsent(nearbyItems.get(i).getPrimaryId(), now + (long) i * ENTRY_STAGGER_MS);
            }
            justOpened = false;
        } else {
            for (VisualItemEntry entry : nearbyItems) {
                entryTimes.putIfAbsent(entry.getPrimaryId(), now);
            }
        }
        entryTimes.keySet().retainAll(currentIds);

        // === 渲染物品行 ===
        int startIdx = Mth.floor(scrollValue);
        int endIdx = Mth.ceil(scrollValue + visibleRows);

        for (int i = 0; i < nearbyItems.size(); i++) {
            if (i < startIdx - 1 || i > endIdx + 1) continue;

            VisualItemEntry entry = nearbyItems.get(i);
            float relIdx = i - scrollValue;
            float itemAlpha = calculateEdgeAlpha(relIdx, visibleRows);
            if (itemAlpha <= 0.05f) continue;

            Long startMs = entryTimes.get(entry.getPrimaryId());
            if (startMs != null && now < startMs) continue;

            int baseY = topPos + (int) (relIdx * ITEM_HEIGHT_TOTAL);
            float entryYOffset = 0f;
            if (startMs != null) {
                float elapsed = (now - startMs) / 1000f;
                float progress = Mth.clamp(elapsed * ENTRY_SPEED, 0f, 1f);
                entryYOffset = (1f - Utils.easeOutCubic(progress)) * ITEM_HEIGHT_TOTAL;
            }
            int drawY = baseY + (int) entryYOffset;

            // 拖拽中的物品：变暗
            float rowBgAlpha = (isDraggingItem && dragIndex == i) ? 0.3f : itemAlpha;
            boolean isNew = !core.isItemInInventory(entry.getItem().getItem());

            renderer.renderItemRow(gui, panelStartX + Constants.LIST_X, drawY, panelWidth, entry,
                    false, rowBgAlpha, itemAlpha, isNew);
        }

        // === 槽位高亮：拖拽模式下，无效槽位标红 ===
        if (isDraggingItem && dragModeActive && dragIndex >= 0 && dragIndex < nearbyItems.size()) {
            renderSlotHighlights(gui, screen, nearbyItems.get(dragIndex).getItem());
        }

        // === 拖拽中的物品跟随鼠标 ===
        if (isDraggingItem && dragModeActive && dragIndex >= 0) {
            renderDragGhost(gui, nearbyItems.get(dragIndex));
        }

        // === 滚动条 ===
        if (nearbyItems.size() > visibleRows) {
            int trackX = panelStartX + SCROLLBAR_TRACK_X_OFFSET;
            renderer.renderScrollBar(gui, nearbyItems.size(), visibleRows,
                    trackX, topPos, imageHeight,
                    isDraggingScrollbar ? 1.0f : 0.7f, scrollValue);
        }
    }

    private void renderDragGhost(GuiGraphics gui, VisualItemEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        var stack = entry.getItem();
        int count = entry.getCount();

        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f);
        int mx = (int) dragCurrentX - 8;
        int my = (int) dragCurrentY - 8;
        gui.renderItem(stack, mx, my);
        String countText = count > 1 ? String.valueOf(count) : null;
        gui.renderItemDecorations(mc.font, stack, mx, my, countText);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderSlotHighlights(GuiGraphics gui, InventoryScreen screen, net.minecraft.world.item.ItemStack draggedStack) {
        ACSAccessor acc = (ACSAccessor) screen;
        int leftPos = acc.getLeftPos();
        int topPos = acc.getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            if (!slot.mayPlace(draggedStack)) {
                int sx = leftPos + slot.x;
                int sy = topPos + slot.y;
                gui.renderOutline(sx, sy, 16, 16, 0xFFFF3333);
            }
        }
    }

    /**
     * 底部淡出。
     */
    private float calculateEdgeAlpha(float relIdx, float visibleRows) {
        if (relIdx < 0) return Mth.clamp(1.0f + relIdx, 0f, 1f);
        if (relIdx > visibleRows - 1.0f) {
            return Mth.clamp(1.0f - (relIdx - (visibleRows - 1.0f)), 0f, 1f);
        }
        return 1.0f;
    }

    // ==========================================
    // 鼠标交互（由 MouseHandlerMixin 调用）
    // ==========================================

    public boolean isMouseOverList(double mouseX, double mouseY) {
        if (cachedImageHeight == 0) return false;
        return mouseX >= cachedPanelStartX && mouseX <= cachedPanelStartX + Constants.LIST_X + cachedPanelWidth
                && mouseY >= cachedTopPos && mouseY <= cachedTopPos + cachedImageHeight;
    }

    public boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (cachedImageHeight == 0) return false;
        int hitMargin = 3;
        return mouseX >= cachedPanelStartX + SCROLLBAR_TRACK_X_OFFSET - hitMargin
                && mouseX <= cachedPanelStartX + SCROLLBAR_TRACK_X_OFFSET + SCROLLBAR_WIDTH + hitMargin
                && mouseY >= cachedTopPos && mouseY <= cachedTopPos + cachedImageHeight;
    }

    public void handleScroll(double delta) {
        if (cachedMaxScroll <= 0) return;
        targetScroll = Mth.clamp(targetScroll - (float) delta, 0, cachedMaxScroll);
    }

    public void onScrollbarPress(double mouseX, double mouseY) {
        isDraggingScrollbar = true;
        updateScrollFromMouse(mouseY);
    }

    public void onScrollbarDrag(double mouseX, double mouseY) {
        if (!isDraggingScrollbar) return;
        updateScrollFromMouse(mouseY);
    }

    public void onScrollbarRelease() {
        isDraggingScrollbar = false;
        targetScroll = scrollState.currentScroll;
    }

    public boolean isDraggingScrollbar() {
        return isDraggingScrollbar;
    }

    // === 物品拖拽 ===

    /**
     * @return true 如果按到了列表物品（开始追踪）
     */
    public boolean onItemPress(double mouseX, double mouseY) {
        int idx = getItemAtMouse(mouseX, mouseY);
        if (idx < 0) return false;
        dragIndex = idx;
        dragStartX = dragCurrentX = mouseX;
        dragStartY = dragCurrentY = mouseY;
        dragStartMs = Util.getMillis();
        dragModeActive = false;
        isDraggingItem = true;
        return true;
    }

    public void onItemDrag(double mouseX, double mouseY) {
        if (!isDraggingItem) return;
        dragCurrentX = mouseX;
        dragCurrentY = mouseY;
        if (!dragModeActive) {
            double dist = (mouseX - dragStartX) * (mouseX - dragStartX) + (mouseY - dragStartY) * (mouseY - dragStartY);
            long elapsed = Util.getMillis() - dragStartMs;
            if (dist > DRAG_THRESHOLD_PX * DRAG_THRESHOLD_PX || elapsed > DRAG_THRESHOLD_MS) {
                dragModeActive = true;
            }
        }
    }

    public void onItemRelease(InventoryScreen screen) {
        if (!isDraggingItem) return;
        isDraggingItem = false;

        if (dragIndex < 0 || dragIndex >= nearbyItems.size()) {
            dragIndex = -1;
            dragModeActive = false;
            return;
        }

        VisualItemEntry entry = nearbyItems.get(dragIndex);

        if (dragModeActive) {
            // 拖拽模式：放到指定槽位
            Slot slot = getHoveredSlot(screen, dragCurrentX, dragCurrentY);
            if (slot != null && slot.mayPlace(entry.getItem())) {
                List<Integer> ids = new ArrayList<>();
                for (ItemEntity e : entry.getSourceEntities()) {
                    if (e.isAlive()) ids.add(e.getId());
                }
                if (!ids.isEmpty()) {
                    NetworkHandler.sendToServer(new PacketPlaceIntoSlot(ids, slot.index));
                }
            }
        } else {
            // 单击模式：自动拾取
            List<Integer> ids = new ArrayList<>();
            for (ItemEntity e : entry.getSourceEntities()) {
                if (e.isAlive()) ids.add(e.getId());
            }
            if (!ids.isEmpty()) {
                NetworkHandler.sendToServer(new PacketBatchPickup(ids, false, true));
            }
        }

        dragIndex = -1;
        dragModeActive = false;
    }

    public boolean isDraggingItem() {
        return isDraggingItem;
    }

    /**
     * 检测鼠标是否在某物品行上，返回行索引或 -1。
     */
    private int getItemAtMouse(double mouseX, double mouseY) {
        if (cachedImageHeight == 0 || nearbyItems.isEmpty()) return -1;
        int itemLeft = cachedPanelStartX + Constants.LIST_X;
        int itemRight = itemLeft + cachedPanelWidth;
        if (mouseX < itemLeft || mouseX > itemRight) return -1;

        int listTop = cachedTopPos;
        int listBottom = listTop + cachedImageHeight;
        if (mouseY < listTop || mouseY > listBottom) return -1;

        float relY = (float) (mouseY - listTop) / ITEM_HEIGHT_TOTAL;
        int idx = Mth.floor(scrollState.currentScroll + relY);
        if (idx < 0 || idx >= nearbyItems.size()) return -1;
        return idx;
    }

    /**
     * 检测鼠标悬停的物品栏槽位。
     */
    public static Slot getHoveredSlot(InventoryScreen screen, double mouseX, double mouseY) {
        ACSAccessor acc = (ACSAccessor) screen;
        double relX = mouseX - acc.getLeftPos();
        double relY = mouseY - acc.getTopPos();
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            if (relX >= slot.x - 1 && relX < slot.x + 17 && relY >= slot.y - 1 && relY < slot.y + 17) {
                return slot;
            }
        }
        return null;
    }

    // === 滚动条 ===

    private void updateScrollFromMouse(double mouseY) {
        if (cachedVisibleRows <= 0 || cachedMaxScroll <= 0) return;
        int itemCount = (int) (cachedVisibleRows + cachedMaxScroll);
        float thumbH = Math.max(10, cachedImageHeight * (cachedVisibleRows / itemCount));
        float relY = (float) (mouseY - cachedTopPos);
        float fraction = Mth.clamp((relY - thumbH / 2f) / (cachedImageHeight - thumbH), 0f, 1f);
        scrollState.currentScroll = fraction * cachedMaxScroll;
    }
}
