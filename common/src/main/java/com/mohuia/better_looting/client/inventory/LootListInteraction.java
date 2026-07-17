package com.mohuia.better_looting.client.inventory;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.mixin.ACSAccessor;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import com.mohuia.better_looting.network.C2S.PacketPlaceIntoSlot;
import com.mohuia.better_looting.network.NetworkHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理物品栏左侧掉落物列表的所有鼠标交互逻辑：
 * 滚轮滚动、滚动条拖拽、物品单击拾取 / 长按拖拽到物品栏槽位。
 */
public class LootListInteraction {
    public static final LootListInteraction INSTANCE = new LootListInteraction();

    private static final int DRAG_THRESHOLD_MS = 200;
    private static final double DRAG_THRESHOLD_PX = 4.0;

    // 物品拖拽
    private int dragIndex = -1;
    private double dragStartX, dragStartY, dragCurrentX, dragCurrentY;
    private long dragStartMs;
    private boolean dragModeActive = false;
    private boolean isDraggingItem = false;

    // 滚动条拖拽
    private boolean isDraggingScrollbar = false;
    private float targetScroll = 0f;

    private LootListInteraction() {}

    void reset() {
        targetScroll = 0f;
        isDraggingScrollbar = false;
        dragIndex = -1;
        isDraggingItem = false;
        dragModeActive = false;
    }

    float getTargetScroll() {
        return targetScroll;
    }

    public boolean isDraggingItem() {
        return isDraggingItem;
    }

    public boolean isDraggingScrollbar() {
        return isDraggingScrollbar;
    }

    int getDragIndex() {
        return dragIndex;
    }

    boolean isDragModeActive() {
        return dragModeActive;
    }

    double getDragCurrentX() {
        return dragCurrentX;
    }

    double getDragCurrentY() {
        return dragCurrentY;
    }

    // ==========================================
    // 鼠标检测
    // ==========================================

    public boolean isMouseOverList(double mouseX, double mouseY) {
        InventoryLootList list = InventoryLootList.INSTANCE;
        if (list.cachedImageHeight == 0) return false;
        float scale = list.cachedScale;
        return mouseX >= list.cachedPanelStartX
                && mouseX <= list.cachedPanelStartX + (Constants.LIST_X + list.cachedPanelWidth) * scale
                && mouseY >= list.cachedTopPos && mouseY <= list.cachedTopPos + list.cachedImageHeight;
    }

    public boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        InventoryLootList list = InventoryLootList.INSTANCE;
        if (list.cachedImageHeight == 0) return false;
        float scale = list.cachedScale;
        int hitMargin = 3;
        float scrollbarX = list.cachedPanelStartX + (Constants.LIST_X - 2 - 2) * scale;
        return mouseX >= scrollbarX - hitMargin
                && mouseX <= scrollbarX + 2 * scale + hitMargin
                && mouseY >= list.cachedTopPos && mouseY <= list.cachedTopPos + list.cachedImageHeight;
    }

    public void handleScroll(double delta) {
        InventoryLootList list = InventoryLootList.INSTANCE;
        if (list.cachedMaxScroll <= 0) return;
        targetScroll = Mth.clamp(targetScroll - (float) delta, 0, list.cachedMaxScroll);
    }

    // ==========================================
    // 滚动条交互
    // ==========================================

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
        targetScroll = InventoryLootList.INSTANCE.scrollState.currentScroll;
    }

    // ==========================================
    // 物品拖拽
    // ==========================================

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

        InventoryLootList list = InventoryLootList.INSTANCE;

        // 直接查 GLFW 原始按键状态，绕过 KeyMapping.isDown() 的 tick 更新时序问题
        // saveString() 返回当前绑定键名，经 InputConstants.getKey 解析回 Key，无需 Accessor mixin
        long w = Minecraft.getInstance().getWindow().getWindow();
        var key = InputConstants.getKey(KeyInit.PICKUP_ALL_MODIFIER.saveString());
        boolean pickupAllMod = key.getValue() != -1 && InputConstants.isKeyDown(w, key.getValue());

        if (dragIndex < 0 || dragIndex >= list.nearbyItems.size()) {
            dragIndex = -1;
            dragModeActive = false;
            return;
        }

        VisualItemEntry entry = list.nearbyItems.get(dragIndex);

        if (pickupAllMod) {
            List<Integer> ids = new ArrayList<>();
            for (ItemEntity e : entry.getSourceEntities()) {
                if (e.isAlive()) ids.add(e.getId());
            }
            if (!ids.isEmpty()) {
                NetworkHandler.sendToServer(new PacketBatchPickup(ids, false, false));
            }
        } else if (dragModeActive) {
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

    // ==========================================
    // 内部辅助
    // ==========================================

    private int getItemAtMouse(double mouseX, double mouseY) {
        InventoryLootList list = InventoryLootList.INSTANCE;
        if (list.cachedImageHeight == 0 || list.nearbyItems.isEmpty()) return -1;
        float scale = list.cachedScale;
        int itemLeft = list.cachedPanelStartX + (int) (Constants.LIST_X * scale);
        int itemRight = itemLeft + (int) (list.cachedPanelWidth * scale);
        if (mouseX < itemLeft || mouseX > itemRight) return -1;

        int listTop = list.cachedTopPos;
        int listBottom = listTop + list.cachedImageHeight;
        if (mouseY < listTop || mouseY > listBottom) return -1;

        float relY = (float) (mouseY - listTop) / ((Constants.ITEM_HEIGHT + 2) * scale);
        int idx = Mth.floor(list.scrollState.currentScroll + relY);
        if (idx < 0 || idx >= list.nearbyItems.size()) return -1;
        return idx;
    }

    static Slot getHoveredSlot(InventoryScreen screen, double mouseX, double mouseY) {
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

    private void updateScrollFromMouse(double mouseY) {
        InventoryLootList list = InventoryLootList.INSTANCE;
        if (list.cachedVisibleRows <= 0 || list.cachedMaxScroll <= 0) return;
        int itemCount = (int) (list.cachedVisibleRows + list.cachedMaxScroll);
        float thumbH = Math.max(10, list.cachedImageHeight * (list.cachedVisibleRows / itemCount));
        float relY = (float) (mouseY - list.cachedTopPos);
        float fraction = Mth.clamp((relY - thumbH / 2f) / (list.cachedImageHeight - thumbH), 0f, 1f);
        list.scrollState.currentScroll = fraction * list.cachedMaxScroll;
    }
}
