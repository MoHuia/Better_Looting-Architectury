package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 简约黑白主题按钮：扁平半透明底色，悬停浮现淡白底纹。
 */
public class ThemedButton extends AbstractButton {
    private final Runnable onPress;
    private final HoverAnim hover = new HoverAnim();

    public ThemedButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
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

        gui.fill(x, y, x + w, y + h, GuiTheme.lerpColor(GuiTheme.WIDGET_BG, GuiTheme.ACCENT_SOFT, t));

        var font = Minecraft.getInstance().font;
        int textColor = GuiTheme.lerpColor(GuiTheme.TEXT_MUTED, GuiTheme.TEXT, t);
        int tw = font.width(getMessage());
        gui.drawString(font, getMessage(), x + (w - tw) / 2, y + (h - 8) / 2, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
