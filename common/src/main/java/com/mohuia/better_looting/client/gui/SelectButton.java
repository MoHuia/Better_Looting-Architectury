package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 单选行控件：左侧单选圆点指示器，右接选项文字。
 * 选中态填充强调色描边圆点 + 高亮文字；未选中为空心圈 + 灰字。
 */
public class SelectButton extends AbstractButton {
    private final Component label;
    private final boolean selected;
    private final Runnable onSelect;

    public SelectButton(int x, int y, int width, int height, Component label,
                        boolean selected, Runnable onSelect, Tooltip tooltip) {
        super(x, y, width, height, label);
        this.label = label;
        this.selected = selected;
        this.onSelect = onSelect;
        if (tooltip != null) this.setTooltip(tooltip);
    }

    @Override
    public void onPress() {
        if (!selected) this.onSelect.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        int x = getX(), y = getY(), w = width, h = height;

        int bg = selected ? GuiTheme.ACCENT_SOFT : (hovered ? GuiTheme.ACCENT_FAINT : GuiTheme.CARD_BG);
        gui.fill(x, y, x + w, y + h, bg);
        if (selected) {
            gui.fill(x, y, x + 2, y + h, GuiTheme.ACCENT);
        }

        // 单选圆点（用方形近似，保持像素锐利）
        int dotSize = 6;
        int dotX = x + 8;
        int dotY = y + (h - dotSize) / 2;
        gui.renderOutline(dotX, dotY, dotSize, dotSize, selected ? GuiTheme.ACCENT : GuiTheme.TEXT_DIM);
        if (selected) {
            gui.fill(dotX + 2, dotY + 2, dotX + dotSize - 2, dotY + dotSize - 2, GuiTheme.ACCENT);
        }

        var font = Minecraft.getInstance().font;
        int textColor = selected ? GuiTheme.TEXT : (hovered ? GuiTheme.TEXT_MUTED : GuiTheme.TEXT_DIM);
        gui.drawString(font, label, dotX + dotSize + 8, y + (h - 8) / 2, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
