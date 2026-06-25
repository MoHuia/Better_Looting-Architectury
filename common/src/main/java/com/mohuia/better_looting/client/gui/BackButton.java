package com.mohuia.better_looting.client.gui;

import com.mohuia.better_looting.BetterLooting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

/**
 * 左上角返回箭头按钮：高清贴图箭头，悬停浮现淡白圆底。
 */
public class BackButton extends AbstractButton {

    private static final ResourceLocation ARROW = new ResourceLocation(BetterLooting.MODID, "textures/gui/back_arrow.png");
    private static final int TEX_SIZE = 64;

    private final Runnable onPress;
    private final HoverAnim hover = new HoverAnim();

    public BackButton(int x, int y, int size, Runnable onPress) {
        super(x, y, size, size, CommonComponents.GUI_BACK);
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

        if (t > 0.01f) {
            gui.fill(x, y, x + w, y + h, GuiTheme.lerpColor(0x00FFFFFF, GuiTheme.ACCENT_SOFT, t));
        }

        // 线性过滤降采样，贴图高清不糊
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(ARROW);
        tex.setFilter(true, false);

        int pad = Math.max(2, w / 5);
        int dw = w - pad * 2;
        int dh = h - pad * 2;
        gui.blit(ARROW, x + pad, y + pad, dw, dh, 0f, 0f, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
