package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * 循环选择行：左侧标签，右侧当前值，两端各一个 ◄ ► 箭头。
 * 点击左箭头区域上一项，右箭头区域或行其他位置下一项。
 */
public class CycleButton extends AbstractButton {
    private final Component label;
    private final Supplier<Component> valueGetter;
    private final Runnable onNext;
    private final Runnable onPrev;

    private static final int ARROW_W = 12;

    public CycleButton(int x, int y, int width, int height, Component label,
                       Supplier<Component> valueGetter, Runnable onNext, Runnable onPrev, Tooltip tooltip) {
        super(x, y, width, height, label);
        this.label = label;
        this.valueGetter = valueGetter;
        this.onNext = onNext;
        this.onPrev = onPrev;
        if (tooltip != null) this.setTooltip(tooltip);
    }

    private boolean overLeftArrow(double mouseX) {
        int valueZoneLeft = getX() + width / 2;
        return mouseX >= valueZoneLeft && mouseX < valueZoneLeft + ARROW_W;
    }

    @Override
    public void onPress() {
        this.onNext.run();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (overLeftArrow(mouseX)) {
            this.onPrev.run();
        } else {
            this.onNext.run();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        int x = getX(), y = getY(), w = width, h = height;

        gui.fill(x, y, x + w, y + h, hovered ? GuiTheme.ACCENT_FAINT : GuiTheme.CARD_BG);
        if (hovered) {
            gui.fill(x, y, x + 2, y + h, GuiTheme.ACCENT);
        }

        var font = Minecraft.getInstance().font;
        int ty = y + (h - 8) / 2;

        // 左侧标签
        gui.drawString(font, label, x + 8, ty, hovered ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED, false);

        // 右侧值区：◄ value ►
        int valueZoneLeft = x + w / 2;
        int valueZoneRight = x + w - 8;
        boolean leftHover = hovered && overLeftArrow(mouseX);

        gui.drawString(font, "\u25C4", valueZoneLeft, ty, leftHover ? GuiTheme.ACCENT : GuiTheme.TEXT_DIM, false);
        gui.drawString(font, "\u25BA", valueZoneRight - font.width("\u25BA"), ty,
                (hovered && !leftHover) ? GuiTheme.ACCENT : GuiTheme.TEXT_DIM, false);

        // 居中显示当前值
        Component value = valueGetter.get();
        int innerLeft = valueZoneLeft + ARROW_W;
        int innerRight = valueZoneRight - ARROW_W;
        int vw = font.width(value);
        int vx = innerLeft + (innerRight - innerLeft - vw) / 2;
        gui.drawString(font, value, vx, ty, GuiTheme.TEXT_VALUE, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
