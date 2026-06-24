package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * 开关行控件：左侧标签文字，右侧绘制一个滑动开关（On/Off）。
 * 整行可点击，点击后触发回调切换状态。
 */
public class ToggleButton extends AbstractButton {
    private final Component label;
    private final BooleanSupplier stateGetter;
    private final Runnable onToggle;

    private static final int TRACK_W = 26;
    private static final int TRACK_H = 12;

    public ToggleButton(int x, int y, int width, int height, Component label,
                        BooleanSupplier stateGetter, Runnable onToggle, Tooltip tooltip) {
        super(x, y, width, height, label);
        this.label = label;
        this.stateGetter = stateGetter;
        this.onToggle = onToggle;
        if (tooltip != null) this.setTooltip(tooltip);
    }

    @Override
    public void onPress() {
        this.onToggle.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        boolean on = stateGetter.getAsBoolean();
        int x = getX(), y = getY(), w = width, h = height;

        // 行背景
        gui.fill(x, y, x + w, y + h, hovered ? GuiTheme.ACCENT_FAINT : GuiTheme.CARD_BG);
        if (hovered) {
            gui.fill(x, y, x + 2, y + h, GuiTheme.ACCENT); // 左侧高亮条
        }

        var font = Minecraft.getInstance().font;
        int textColor = hovered ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED;
        gui.drawString(font, label, x + 8, y + (h - 8) / 2, textColor, false);

        // 右侧开关
        int trackX = x + w - TRACK_W - 8;
        int trackY = y + (h - TRACK_H) / 2;
        int trackColor = on ? GuiTheme.TOGGLE_TRACK_ON : GuiTheme.TOGGLE_TRACK_OFF;
        gui.fill(trackX, trackY, trackX + TRACK_W, trackY + TRACK_H, trackColor);
        if (on) {
            gui.renderOutline(trackX, trackY, TRACK_W, TRACK_H, GuiTheme.ACCENT);
        }
        // 圆点
        int knobSize = TRACK_H - 4;
        int knobX = on ? (trackX + TRACK_W - knobSize - 2) : (trackX + 2);
        int knobY = trackY + 2;
        gui.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, on ? GuiTheme.TOGGLE_KNOB_ON : GuiTheme.TOGGLE_KNOB_OFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
