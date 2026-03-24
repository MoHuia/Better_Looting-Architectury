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

/**
 * 负责渲染过滤器面板并处理其内部的鼠标交互（点击、滚动）。
 * 附着在任意实现了 AbstractContainerScreen 的原版或模组容器界面旁。
 */
public class FilterPanel {
    private static boolean isOpen = false;
    private static float scrollOffset = 0f;

    // UI 布局常量定义。集中管理以便于后期调整样式。
    private static final int COLS = 2, ROWS = 5, SLOT_SIZE = 18, GAP = 1;
    private static final int SCROLLBAR_WIDTH = 4, BUTTON_HEIGHT = 14, BUTTON_GAP = 3;

    public static final int PANEL_WIDTH = SCROLLBAR_WIDTH + COLS * SLOT_SIZE + (COLS - 1) * GAP;
    public static final int PANEL_HEIGHT = BUTTON_HEIGHT + BUTTON_GAP + ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

    public static void toggle() { isOpen = !isOpen; }
    public static void close() { isOpen = false; }
    public static boolean isOpen() { return isOpen; }

    /**
     * 核心渲染逻辑。
     * 包含背景、清空按钮、滚动视图（Scissor 裁剪）以及物品图标的绘制。
     */
    public static void render(GuiGraphics gui, int mouseX, int mouseY, AbstractContainerScreen<?> screen) {
        if (!isOpen) return;

        ACSAccessor acc = (ACSAccessor) screen;
        // 面板始终停靠在主界面的左侧。
        int startX = Math.max(2, acc.getLeftPos() - PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - PANEL_HEIGHT) / 2;

        // 1. 渲染 "Clear" 清空按钮
        boolean isHoveringBtn = mouseX >= startX && mouseX < startX + PANEL_WIDTH && mouseY >= startY && mouseY < startY + BUTTON_HEIGHT;
        gui.fill(startX, startY, startX + PANEL_WIDTH, startY + BUTTON_HEIGHT, isHoveringBtn ? 0xCC990000 : 0xAA222222);
        gui.renderOutline(startX, startY, PANEL_WIDTH, BUTTON_HEIGHT, isHoveringBtn ? 0xFFFF5555 : 0xFF444444);
        gui.drawCenteredString(Minecraft.getInstance().font, "Clear", startX + PANEL_WIDTH / 2, startY + (BUTTON_HEIGHT - 8) / 2, isHoveringBtn ? 0xFFFFFFFF : 0xFFAAAAAA);

        // 2. 准备滚动网格数据
        List<ItemStack> items = FilterWhitelist.INSTANCE.getDisplayItems();
        int totalRows = (int) Math.ceil((double) items.size() / COLS) + 1; // +1 是为了给添加按钮（+号）留位置
        int maxScroll = Math.max(0, totalRows - ROWS);

        int gridStartY = startY + BUTTON_HEIGHT + BUTTON_GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

        // OpenGL 剪裁区域 (Scissor) 计算。
        // 意图：因为我们要实现平滑滚动，物品图标可能会超出网格边界。
        // 注意：OpenGL 的原点在屏幕左下角，而 Minecraft GUI 原点在左上角，且必须乘以 GUI 缩放比例。
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        int scY = Minecraft.getInstance().getWindow().getHeight() - (int)((gridStartY + gridHeight) * guiScale);
        int scH = (int)(gridHeight * guiScale);
        RenderSystem.enableScissor((int)(startX * guiScale), scY, (int)(PANEL_WIDTH * guiScale), scH);

        int currentScrollRow = (int) Math.floor(scrollOffset);
        int pixelOffset = (int)((scrollOffset % 1.0f) * (SLOT_SIZE + GAP));

        // 3. 渲染物品网格 (+1 行用于处理滚动时的半截显示)
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

                    // 物品悬停提示 (Tooltip)
                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE && mouseY >= gridStartY && mouseY < gridStartY + gridHeight) {
                        gui.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FF0000); // 高亮遮罩

                        // 渲染 Tooltip 前必须暂时关闭剪裁，否则 Tooltip 如果过长会被切断
                        RenderSystem.disableScissor();
                        gui.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                        RenderSystem.enableScissor((int)(startX * guiScale), scY, (int)(PANEL_WIDTH * guiScale), scH);
                    }
                } else if (index == items.size()) {
                    // 渲染列表末尾的 "+" 号，提示玩家可以放入物品
                    gui.drawCenteredString(Minecraft.getInstance().font, "+", x + 9, y + 5, 0xFF555555);
                }
            }
        }
        RenderSystem.disableScissor();

        // 4. 渲染滚动条
        if (maxScroll > 0) {
            int thumbH = Math.max(10, (int)(gridHeight * ((float)ROWS / totalRows)));
            int thumbY = gridStartY + (int)((gridHeight - thumbH) * (scrollOffset / maxScroll));
            gui.fill(startX, gridStartY, startX + 2, gridStartY + gridHeight, 0xFF222222);
            gui.fill(startX, thumbY, startX + 2, thumbY + thumbH, 0xFF888888);
        }
    }

    /**
     * 处理面板内的鼠标点击事件。
     * @return 如果点击被面板消费（拦截），则返回 true。
     */
    public static boolean click(double mouseX, double mouseY, AbstractContainerScreen<?> screen) {
        if (!isOpen) return false;

        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - PANEL_HEIGHT) / 2;

        if (mouseX < startX || mouseX > startX + PANEL_WIDTH || mouseY < startY || mouseY > startY + PANEL_HEIGHT) return false;

        // 处理 Clear 按钮点击
        if (mouseY >= startY && mouseY < startY + BUTTON_HEIGHT) {
            FilterWhitelist.INSTANCE.clear();
            playClickSound(1.0F);
            return true;
        }

        // 处理网格区域点击
        int gridStartY = startY + BUTTON_HEIGHT + BUTTON_GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

        if (mouseY >= gridStartY && mouseY < gridStartY + gridHeight) {
            // 将绝对坐标转换为受平滑滚动影响的网格相对坐标
            double relX = mouseX - (startX + SCROLLBAR_WIDTH);
            double relY = mouseY - gridStartY + (scrollOffset % 1.0f) * (SLOT_SIZE + GAP);

            int col = (int) (relX / (SLOT_SIZE + GAP));
            int row = (int) (relY / (SLOT_SIZE + GAP));

            if (col >= 0 && col < COLS) {
                int dataIndex = ((int)scrollOffset + row) * COLS + col;
                List<ItemStack> items = FilterWhitelist.INSTANCE.getDisplayItems();
                ItemStack cursorStack = screen.getMenu().getCarried(); // 获取鼠标上抓取的物品

                if (dataIndex < items.size()) {
                    // 点击已有物品：如果鼠标是空的，则移除该物品
                    if (cursorStack.isEmpty()) {
                        FilterWhitelist.INSTANCE.remove(items.get(dataIndex));
                        playClickSound(0.5F); // 音调较低，表示移除
                    }
                } else if (dataIndex == items.size() && !cursorStack.isEmpty()) {
                    // 点击末尾的 "+" 号：如果鼠标上有物品，则将其加入白名单
                    FilterWhitelist.INSTANCE.add(cursorStack);
                    playClickSound(1.2F); // 音调较高，表示添加
                }
            }
        }
        return true;
    }

    /**
     * 处理鼠标滚轮滑动逻辑。
     */
    public static boolean scroll(double delta) {
        if (!isOpen) return false;
        int totalRows = (int) Math.ceil((double) FilterWhitelist.INSTANCE.getDisplayItems().size() / COLS) + 1;
        float maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll > 0) {
            // 在 Minecraft 1.20.1 中，滚轮 delta 通常是 1.0（向上）或 -1.0（向下）
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