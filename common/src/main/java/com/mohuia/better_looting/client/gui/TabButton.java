package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 顶部导航标签按钮。
 * 选中态：填充强调色底纹 + 底部高亮条 + 亮白文字；
 * 未选中：透明背景，悬停时浮现淡蓝底纹。
 */
public class TabButton extends AbstractButton {
    private final Runnable onPress;
    private final boolean selected;
    private final HoverAnim hover = new HoverAnim();

    public TabButton(int x, int y, int width, int height, Component message, boolean selected, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.selected = selected;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        float t = hover.update(hovered);
        int x = getX(), y = getY(), w = width, h = height;

        if (selected) {
            gui.fill(x, y, x + w, y + h, GuiTheme.ACCENT_SOFT);
            // 底部高亮条
            gui.fill(x, y + h - 2, x + w, y + h, GuiTheme.ACCENT);
        } else if (t > 0.01f) {
            // 未选中：悬停底纹随 hover 渐显
            gui.fill(x, y, x + w, y + h, GuiTheme.lerpColor(0x00FFFFFF, GuiTheme.ACCENT_FAINT, t));
        }

        var font = Minecraft.getInstance().font;
        int textColor = selected ? GuiTheme.TEXT : GuiTheme.lerpColor(GuiTheme.TEXT_MUTED, GuiTheme.TEXT, t);
        int tw = font.width(getMessage());
        gui.drawString(font, getMessage(), x + (w - tw) / 2, y + (h - 8) / 2, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
