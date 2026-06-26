package com.mohuia.better_looting.client.inventory;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.filter.FilterPanel;
import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.client.overlay.OverlayRenderer;
import com.mohuia.better_looting.client.overlay.OverlayState;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.mixin.ACSAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在玩家物品栏左侧渲染当前附近掉落物列表。
 * 鼠标交互逻辑已抽取到 {@link LootListInteraction}。
 */
public class InventoryLootList {
    public static final InventoryLootList INSTANCE = new InventoryLootList();

    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLLBAR_TRACK_X_OFFSET = Constants.LIST_X - SCROLLBAR_WIDTH - 2;
    private static final int ITEM_HEIGHT_TOTAL = Constants.ITEM_HEIGHT + 2;
    private static final int ENTRY_STAGGER_MS = 40;
    private static final float ENTRY_SPEED = 6.0f;
    // 默认面板宽度参照值：偏移为 0、宽度为默认值、缩放 1 时，列表右缘恰好贴合背包左侧
    public static final int DEFAULT_LIST_WIDTH = 100;

    // 包内可见，供 LootListInteraction 访问
    List<VisualItemEntry> nearbyItems = List.of();
    OverlayState scrollState = new OverlayState();

    int cachedPanelStartX;
    int cachedTopPos;
    int cachedImageHeight;
    int cachedPanelWidth;
    float cachedVisibleRows;
    float cachedMaxScroll;
    float cachedScale = 1.0f;

    private OverlayRenderer renderer;

    // 物品入场动画
    private final Map<Integer, Long> entryTimes = new HashMap<>();
    private boolean justOpened = false;

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
        entryTimes.clear();
        justOpened = true;
        LootListInteraction.INSTANCE.reset();
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

        LootListInteraction interaction = LootListInteraction.INSTANCE;

        // === 布局 ===
        BetterLootingConfig cfg = BetterLootingConfig.get();
        if (!cfg.showInventoryLootList) return;
        if (FilterPanel.isOpen()) return;
        ACSAccessor acc = (ACSAccessor) screen;
        int leftPos = acc.getLeftPos();
        int topPos = acc.getTopPos();
        int gap = 2;
        int panelWidth = cfg.inventoryListWidth;

        // 用户可调：相对默认贴合位置的偏移、整体缩放、独立透明度、面板高度
        float scale = cfg.inventoryListScale;
        float listAlpha = cfg.inventoryListAlpha;
        int panelHeight = cfg.inventoryListHeight; // 局部高度（缩放前）

        // 与 ConfigScreen 同构：左上角为锚点，仅由偏移决定（不随宽高/缩放变动），
        // 宽、高、缩放统一从锚点向右下方生长。默认锚点贴合背包左侧。
        int panelStartX = leftPos - gap - Constants.LIST_X - DEFAULT_LIST_WIDTH + Math.round(cfg.inventoryListXOffset);
        int panelTop = topPos + Math.round(cfg.inventoryListYOffset);

        // 局部高度直接为配置高度；可视行数由其决定
        float localHeight = panelHeight;
        float visibleRows = localHeight / ITEM_HEIGHT_TOTAL;

        this.cachedPanelStartX = panelStartX;
        this.cachedTopPos = panelTop;
        this.cachedImageHeight = Math.round(panelHeight * scale); // 屏幕像素高，供命中检测
        this.cachedPanelWidth = panelWidth;
        this.cachedVisibleRows = visibleRows;
        this.cachedMaxScroll = Math.max(0, nearbyItems.size() - visibleRows);
        this.cachedScale = scale;

        // === 滚动物理 ===
        scrollState.tick(true, interaction.getTargetScroll(), nearbyItems.size(), visibleRows);
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

        // === 渲染物品行（统一在锚点 pose 内按 scale 缩放绘制） ===
        int startIdx = Mth.floor(scrollValue);
        int endIdx = Mth.ceil(scrollValue + visibleRows);

        boolean isDraggingItem = interaction.isDraggingItem();
        int dragIndex = interaction.getDragIndex();

        gui.pose().pushPose();
        gui.pose().translate(panelStartX, panelTop, 0);
        gui.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < nearbyItems.size(); i++) {
            if (i < startIdx - 1 || i > endIdx + 1) continue;

            VisualItemEntry entry = nearbyItems.get(i);
            float relIdx = i - scrollValue;
            float itemAlpha = calculateEdgeAlpha(relIdx, visibleRows);
            if (itemAlpha <= 0.05f) continue;

            Long startMs = entryTimes.get(entry.getPrimaryId());
            if (startMs != null && now < startMs) continue;

            int baseY = Math.round(relIdx * ITEM_HEIGHT_TOTAL);
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

            renderer.renderItemRow(gui, Constants.LIST_X, drawY, panelWidth, entry,
                    false, rowBgAlpha * listAlpha, itemAlpha * listAlpha, isNew, true);
        }

        // === 滚动条（局部坐标，随 scale 一同缩放） ===
        if (nearbyItems.size() > visibleRows) {
            renderer.renderScrollBar(gui, nearbyItems.size(), visibleRows,
                    SCROLLBAR_TRACK_X_OFFSET, 0, Math.round(localHeight),
                    interaction.isDraggingScrollbar() ? 1.0f : 0.7f, scrollValue);
        }

        gui.pose().popPose();

        // === 槽位高亮：拖拽模式下，无效槽位标红（屏幕坐标，不随列表缩放） ===
        if (isDraggingItem && interaction.isDragModeActive() && dragIndex >= 0 && dragIndex < nearbyItems.size()) {
            renderSlotHighlights(gui, screen, nearbyItems.get(dragIndex).getItem());
        }

        // === 拖拽中的物品跟随鼠标（屏幕坐标） ===
        if (isDraggingItem && interaction.isDragModeActive() && dragIndex >= 0) {
            renderDragGhost(gui, nearbyItems.get(dragIndex), interaction.getDragCurrentX(), interaction.getDragCurrentY());
        }
    }

    private void renderDragGhost(GuiGraphics gui, VisualItemEntry entry, double dragCurrentX, double dragCurrentY) {
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
}
