package com.mohuia.better_looting.client.filter;

import com.mohuia.better_looting.mixin.ACSAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
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
    private static final int STRIP_HEIGHT = 10, STRIP_GAP = 1;

    public static final int PANEL_WIDTH = SCROLLBAR_WIDTH + COLS * SLOT_SIZE + (COLS - 1) * GAP;
    public static final int PANEL_HEIGHT = STRIP_HEIGHT + STRIP_GAP + BUTTON_HEIGHT + BUTTON_GAP + ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

    // 0 = 白名单, 1 = 黑名单
    private static int activeList = 0;

    public static void toggle() { isOpen = !isOpen; }
    public static void close() { isOpen = false; }
    public static boolean isOpen() { return isOpen; }
    public static boolean isWhitelistActive() { return activeList == 0; }

    /**
     * 核心渲染逻辑。
     * 包含背景、清空按钮、滚动视图（Scissor 裁剪）以及物品图标的绘制。
     */
    public static void render(GuiGraphics gui, int mouseX, int mouseY, AbstractContainerScreen<?> screen) {
        if (!isOpen) return;

        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - PANEL_HEIGHT) / 2;

        // === 0. 白名单/黑名单切换条 ===
        int stripY = startY;
        int halfW = PANEL_WIDTH / 2;
        boolean hoverWhite = mouseX >= startX && mouseX < startX + halfW && mouseY >= stripY && mouseY < stripY + STRIP_HEIGHT;
        boolean hoverBlack = mouseX >= startX + halfW && mouseX < startX + PANEL_WIDTH && mouseY >= stripY && mouseY < stripY + STRIP_HEIGHT;

        // 白名单条
        int whiteBg = (activeList == 0) ? 0xFFDDDDDD : (hoverWhite ? 0xFFBBBBBB : 0xFF888888);
        int whiteBorder = (activeList == 0) ? 0xFFFFFFFF : (hoverWhite ? 0xFFCCCCCC : 0xFF666666);
        gui.fill(startX, stripY, startX + halfW, stripY + STRIP_HEIGHT, whiteBg);
        gui.renderOutline(startX, stripY, halfW, STRIP_HEIGHT, whiteBorder);
        String whiteText = (activeList == 0) ? "W" : "w";
        gui.drawCenteredString(Minecraft.getInstance().font, whiteText, startX + halfW / 2, stripY + 1, activeList == 0 ? 0xFF000000 : 0xFFAAAAAA);
        if (hoverWhite) {
            gui.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui." + "better_looting" + ".filter.whitelist_tip"), mouseX, mouseY);
        }

        // 黑名单条
        int blackBg = (activeList == 1) ? 0xFF444444 : (hoverBlack ? 0xFF555555 : 0xFF222222);
        int blackBorder = (activeList == 1) ? 0xFFFFFFFF : (hoverBlack ? 0xFF888888 : 0xFF444444);
        gui.fill(startX + halfW, stripY, startX + PANEL_WIDTH, stripY + STRIP_HEIGHT, blackBg);
        gui.renderOutline(startX + halfW, stripY, halfW, STRIP_HEIGHT, blackBorder);
        String blackText = (activeList == 1) ? "B" : "b";
        gui.drawCenteredString(Minecraft.getInstance().font, blackText, startX + halfW + halfW / 2, stripY + 1, activeList == 1 ? 0xFFFFFFFF : 0xFF888888);
        if (hoverBlack) {
            gui.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("gui." + "better_looting" + ".filter.blacklist_tip"), mouseX, mouseY);
        }

        int contentStartY = startY + STRIP_HEIGHT + STRIP_GAP;

        // 1. 渲染 "Clear" 清空按钮
        boolean isHoveringBtn = mouseX >= startX && mouseX < startX + PANEL_WIDTH && mouseY >= contentStartY && mouseY < contentStartY + BUTTON_HEIGHT;
        gui.fill(startX, contentStartY, startX + PANEL_WIDTH, contentStartY + BUTTON_HEIGHT, isHoveringBtn ? 0xCC990000 : 0xAA222222);
        gui.renderOutline(startX, contentStartY, PANEL_WIDTH, BUTTON_HEIGHT, isHoveringBtn ? 0xFFFF5555 : 0xFF444444);
        gui.drawCenteredString(Minecraft.getInstance().font, "Clear", startX + PANEL_WIDTH / 2, contentStartY + (BUTTON_HEIGHT - 8) / 2, isHoveringBtn ? 0xFFFFFFFF : 0xFFAAAAAA);

        // 2. 准备滚动网格数据
        List<ItemStack> items = getActiveItems();
        int totalRows = (int) Math.ceil((double) items.size() / COLS) + 1;
        int maxScroll = Math.max(0, totalRows - ROWS);

        int gridStartY = contentStartY + BUTTON_HEIGHT + BUTTON_GAP;
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
                        try {
                            gui.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
                        } finally {
                            RenderSystem.enableScissor((int)(startX * guiScale), scY, (int)(PANEL_WIDTH * guiScale), scH);
                        }
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

        // 处理白/黑名单切换条点击
        int stripY = startY;
        if (mouseY >= stripY && mouseY < stripY + STRIP_HEIGHT) {
            int halfW = PANEL_WIDTH / 2;
            activeList = (mouseX >= startX + halfW) ? 1 : 0;
            playClickSound(1.0F);
            return true;
        }

        int contentStartY = startY + STRIP_HEIGHT + STRIP_GAP;

        // 处理 Clear 按钮点击
        if (mouseY >= contentStartY && mouseY < contentStartY + BUTTON_HEIGHT) {
            if (activeList == 0) FilterWhitelist.INSTANCE.clear();
            else FilterBlacklist.INSTANCE.clear();
            playClickSound(1.0F);
            return true;
        }

        // 处理网格区域点击
        int gridStartY = contentStartY + BUTTON_HEIGHT + BUTTON_GAP;
        int gridHeight = ROWS * SLOT_SIZE + (ROWS - 1) * GAP;

        if (mouseY >= gridStartY && mouseY < gridStartY + gridHeight) {
            double relX = mouseX - (startX + SCROLLBAR_WIDTH);
            double relY = mouseY - gridStartY + (scrollOffset % 1.0f) * (SLOT_SIZE + GAP);

            int col = (int) (relX / (SLOT_SIZE + GAP));
            int row = (int) (relY / (SLOT_SIZE + GAP));

            if (col >= 0 && col < COLS) {
                int dataIndex = ((int)scrollOffset + row) * COLS + col;
                List<ItemStack> items = getActiveItems();
                ItemStack cursorStack = screen.getMenu().getCarried();

                if (dataIndex < items.size()) {
                    if (cursorStack.isEmpty()) {
                        if (activeList == 0) FilterWhitelist.INSTANCE.remove(items.get(dataIndex));
                        else FilterBlacklist.INSTANCE.remove(items.get(dataIndex));
                        playClickSound(0.5F);
                    }
                } else if (dataIndex == items.size() && !cursorStack.isEmpty()) {
                    if (activeList == 0) FilterWhitelist.INSTANCE.add(cursorStack);
                    else FilterBlacklist.INSTANCE.add(cursorStack);
                    playClickSound(1.2F);
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
        int totalRows = (int) Math.ceil((double) getActiveItems().size() / COLS) + 1;
        float maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (float)delta, 0, maxScroll);
            return true;
        }
        return false;
    }

    private static List<ItemStack> getActiveItems() {
        return activeList == 0 ? FilterWhitelist.INSTANCE.getDisplayItems() : FilterBlacklist.INSTANCE.getDisplayItems();
    }

    private static void playClickSound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }
}