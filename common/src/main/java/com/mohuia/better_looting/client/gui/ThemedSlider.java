package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 与深空蓝主题统一的现代滑块。
 * 自绘轨道、已填充进度与手柄，替代原版纹理外观；
 * 数值映射与文本格式化逻辑与 CommonSlider 保持一致。
 */
public class ThemedSlider extends AbstractSliderButton {
    private final double min, max;
    private final Consumer<Double> setter;
    private final Component prefix;
    private final int precision;
    private final String suffix;
    private final HoverAnim hover = new HoverAnim();

    public ThemedSlider(int x, int y, int width, int height, Component prefix, String suffix,
                        double min, double max, double current, int precision, Consumer<Double> setter) {
        this(x, y, width, height, prefix, suffix, min, max, current, precision, setter, null);
    }

    public ThemedSlider(int x, int y, int width, int height, Component prefix, String suffix,
                        double min, double max, double current, int precision, Consumer<Double> setter,
                        Tooltip tooltip) {
        super(x, y, width, height, prefix, (current - min) / (max - min));
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.precision = precision;
        this.suffix = suffix;
        this.setter = setter;
        if (tooltip != null) this.setTooltip(tooltip);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double val = min + (value * (max - min));
        String valueText = String.format("%." + precision + "f", val) + suffix;
        setMessage(prefix.copy().append(": ").append(Component.literal(valueText)));
    }

    @Override
    protected void applyValue() {
        setter.accept(min + (value * (max - min)));
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        float t = hover.update(hovered);
        int x = getX(), y = getY(), w = width, h = height;

        // 轨道背景
        gui.fill(x, y, x + w, y + h, GuiTheme.CARD_BG);
        gui.renderOutline(x, y, w, h, GuiTheme.lerpColor(GuiTheme.WIDGET_BORDER, GuiTheme.ACCENT, t));

        // 已填充进度（左侧淡白）
        int fillW = (int) (value * w);
        gui.fill(x, y, x + fillW, y + h, GuiTheme.ACCENT_FAINT);

        // 手柄
        int handleW = 4;
        int handleX = x + (int) (value * (w - handleW));
        gui.fill(handleX, y, handleX + handleW, y + h, GuiTheme.lerpColor(GuiTheme.TEXT_MUTED, GuiTheme.ACCENT, t));

        // 居中文字
        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        gui.drawString(font, getMessage(), x + (w - tw) / 2, y + (h - 8) / 2, GuiTheme.TEXT, false);
    }
}
