package com.mohuia.better_looting.client.filter;

import com.mohuia.better_looting.mixin.ACSAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FilterPanel {
    private static boolean isOpen = false;
    private static float scrollOffset = 0f;

    private static final int COLS = 2, ROWS = 5, SLOT_SIZE = 18, GAP = 1;
    private static final int SCROLLBAR_WIDTH = 4, BUTTON_HEIGHT = 14, BUTTON_GAP = 3;

    public static final int PANEL_WIDTH = SCROLLBAR_WIDTH + COLS * SLOT_SIZE + (COLS - 1) * GAP;
    public static final int PANEL_HEIGHT = BUTTON_HEIGHT + BUTTON_GAP + ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

    public static void toggle() { isOpen = !isOpen; }
    public static void close() { isOpen = false; }
    public static boolean isOpen() { return isOpen; }

    public static void render(GuiGraphics gui, int mouseX, int mouseY, AbstractContainerScreen<?> screen) {
        if (!isOpen) return;

        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - PANEL_HEIGHT) / 2;

        boolean isHoveringBtn = mouseX >= startX && mouseX < startX + PANEL_WIDTH && mouseY >= startY && mouseY < startY + BUTTON_HEIGHT;
        gui.fill(startX, startY, startX + PANEL_WIDTH, startY + BUTTON_HEIGHT, isHoveringBtn ? 0xCC990000 : 0xAA222222);
        gui.renderOutline(startX, startY, PANEL_WIDTH, BUTTON_HEIGHT, isHoveringBtn ? 0xFFFF5555 : 0xFF444444);
        gui.drawCenteredString(Minecraft.getInstance().font, "Clear", startX + PANEL_WIDTH / 2, startY + (BUTTON_HEIGHT - 8) / 2, isHoveringBtn ? 0xFFFFFFFF : 0xFFAAAAAA);

        List<ItemStack> items = FilterWhitelist.INSTANCE.getDisplayItems();
        int totalRows = (int) Math.ceil((double) items.size() / COLS) + 1;
        int maxScroll = Math.max(0, totalRows - ROWS);

        int gridStartY = startY + BUTTON_HEIGHT + BUTTON_GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

        int scY = Minecraft.getInstance().getWindow().getHeight() - (int)((gridStartY + gridHeight) * guiScale);
        int scH = (int)(gridHeight * guiScale);
        RenderSystem.enableScissor((int)(startX * guiScale), scY, (int)(PANEL_WIDTH * guiScale), scH);

        int currentScrollRow = (int) Math.floor(scrollOffset);
        int pixelOffset = (int)((scrollOffset % 1.0f) * (SLOT_SIZE + GAP));

        for (int r = 0; r < ROWS + 1; r++) {
            int dataRow = currentScrollRow + r;
            if (dataRow < 0) continue;

            for (int c = 0; c < COLS; c++) {
                int index = dataRow * COLS + c;
                int x = startX + SCROLLBAR_WIDTH + c * (SLOT_SIZE + GAP);
                int y = gridStartY + r * (SLOT_SIZE + GAP) - pixelOffset;

                gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF333333);
                gui.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFF777777);

                if (index < items.size()) {
                    ItemStack stack = items.get(index);
                    gui.renderItem(stack, x + 1, y + 1);

                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE && mouseY >= gridStartY && mouseY < gridStartY + gridHeight) {
                        gui.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FF0000);
                        RenderSystem.disableScissor();
                        gui.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                        RenderSystem.enableScissor((int)(startX * guiScale), scY, (int)(PANEL_WIDTH * guiScale), scH);
                    }
                } else if (index == items.size()) {
                    gui.drawCenteredString(Minecraft.getInstance().font, "+", x + 9, y + 5, 0xFF555555);
                }
            }
        }
        RenderSystem.disableScissor();

        if (maxScroll > 0) {
            int thumbH = Math.max(10, (int)(gridHeight * ((float)ROWS / totalRows)));
            int thumbY = gridStartY + (int)((gridHeight - thumbH) * (scrollOffset / maxScroll));
            gui.fill(startX, gridStartY, startX + 2, gridStartY + gridHeight, 0xFF222222);
            gui.fill(startX, thumbY, startX + 2, thumbY + thumbH, 0xFF888888);
        }
    }

    public static boolean click(double mouseX, double mouseY, AbstractContainerScreen<?> screen) {
        if (!isOpen) return false;

        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - PANEL_HEIGHT) / 2;

        if (mouseX < startX || mouseX > startX + PANEL_WIDTH || mouseY < startY || mouseY > startY + PANEL_HEIGHT) return false;

        // 点击 Clear
        if (mouseY >= startY && mouseY < startY + BUTTON_HEIGHT) {
            FilterWhitelist.INSTANCE.clear();
            playClickSound(1.0F);
            return true;
        }

        // 点击网格
        int gridStartY = startY + BUTTON_HEIGHT + BUTTON_GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

        if (mouseY >= gridStartY && mouseY < gridStartY + gridHeight) {
            // 重要优化：点击判定必须考虑平滑滚动的偏移
            double relX = mouseX - (startX + SCROLLBAR_WIDTH);
            double relY = mouseY - gridStartY + (scrollOffset % 1.0f) * (SLOT_SIZE + GAP);

            int col = (int) (relX / (SLOT_SIZE + GAP));
            int row = (int) (relY / (SLOT_SIZE + GAP));

            if (col >= 0 && col < COLS) {
                int dataIndex = ((int)scrollOffset + row) * COLS + col;
                List<ItemStack> items = FilterWhitelist.INSTANCE.getDisplayItems();
                ItemStack cursorStack = screen.getMenu().getCarried();

                if (dataIndex < items.size()) {
                    if (cursorStack.isEmpty()) {
                        FilterWhitelist.INSTANCE.remove(items.get(dataIndex));
                        playClickSound(0.5F);
                    }
                } else if (dataIndex == items.size() && !cursorStack.isEmpty()) {
                    FilterWhitelist.INSTANCE.add(cursorStack);
                    playClickSound(1.2F);
                }
            }
        }
        return true;
    }

    public static boolean scroll(double delta) {
        if (!isOpen) return false;
        int totalRows = (int) Math.ceil((double) FilterWhitelist.INSTANCE.getDisplayItems().size() / COLS) + 1;
        float maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll > 0) {
            // delta 在 1.20.1 中通常是 1.0 或 -1.0
            scrollOffset = Mth.clamp(scrollOffset - (float)delta, 0, maxScroll);
            return true;
        }
        return false;
    }

    private static void playClickSound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }
}