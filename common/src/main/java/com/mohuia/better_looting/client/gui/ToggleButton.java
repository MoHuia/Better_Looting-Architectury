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
    private final HoverAnim hover = new HoverAnim();
    private final HoverAnim knobAnim = new HoverAnim();

    private static final int TRACK_W = 26;
    private static final int TRACK_H = 12;

    public ToggleButton(int x, int y, int width, int height, Component label,
                        BooleanSupplier stateGetter, Runnable onToggle, Tooltip tooltip) {
        super(x, y, width, height, label);
        this.label = label;
        this.stateGetter = stateGetter;
        this.onToggle = onToggle;
        this.knobAnim.snap(stateGetter.getAsBoolean()); // 避免首次渲染时滑块从OFF动画到ON
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
        float t = hover.update(hovered);
        float k = knobAnim.update(on); // 圆点位置进度：关 0 → 开 1
        int x = getX(), y = getY(), w = width, h = height;

        // 行背景
        gui.fill(x, y, x + w, y + h, GuiTheme.lerpColor(GuiTheme.CARD_BG, GuiTheme.ACCENT_FAINT, t));
        if (t > 0.01f) {
            gui.fill(x, y, x + 2, y + h, GuiTheme.lerpColor(0x00FFFFFF, GuiTheme.ACCENT, t)); // 左侧高亮条渐显
        }

        var font = Minecraft.getInstance().font;
        gui.drawString(font, label, x + 8, y + (h - 8) / 2, GuiTheme.lerpColor(GuiTheme.TEXT_MUTED, GuiTheme.TEXT, t), false);

        // 右侧开关：轨道颜色随状态插值
        int trackX = x + w - TRACK_W - 8;
        int trackY = y + (h - TRACK_H) / 2;
        int trackColor = GuiTheme.lerpColor(GuiTheme.TOGGLE_TRACK_OFF, GuiTheme.TOGGLE_TRACK_ON, k);
        gui.fill(trackX, trackY, trackX + TRACK_W, trackY + TRACK_H, trackColor);
        if (k > 0.5f) {
            gui.renderOutline(trackX, trackY, TRACK_W, TRACK_H, GuiTheme.ACCENT);
        }
        // 圆点：在两端之间滑动
        int knobSize = TRACK_H - 4;
        int knobLeft = trackX + 2;
        int knobRight = trackX + TRACK_W - knobSize - 2;
        int knobX = Math.round(knobLeft + (knobRight - knobLeft) * k);
        int knobY = trackY + 2;
        int knobColor = GuiTheme.lerpColor(GuiTheme.TOGGLE_KNOB_OFF, GuiTheme.TOGGLE_KNOB_ON, k);
        gui.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
