package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HotbarIndicator {
    public static final HotbarIndicator INSTANCE = new HotbarIndicator();

    public void render(GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui || mc.screen != null) return;

        if (mc.gameMode != null && !mc.gameMode.canHurtPlayer() && mc.player != null && mc.player.isSpectator()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 + 91 + 6;
        int startY = screenHeight - 22 + 4;

        Core.FilterMode mode = Core.INSTANCE.getFilterMode();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawIndicator(gui, x, startY, mode == Core.FilterMode.ALL, 0xFFFFFFFF);
        drawIndicator(gui, x, startY + 8, mode == Core.FilterMode.RARE_ONLY, 0xFFFFD700);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawIndicator(GuiGraphics gui, int x, int y, boolean active, int color) {
        int size = 6;

        int bgColor = active ? (color & 0x00FFFFFF) | 0x80000000 : 0x40000000;
        int borderColor = active ? color : Utils.colorWithAlpha(color, 120);

        renderRoundedRect(gui, x, y, size, size, bgColor);

        gui.renderOutline(x, y, size, size, borderColor);

        if (active) {
            gui.fill(x + size + 1, y + 1, x + size + 2, y + size - 1, color);
        }
    }

    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }
}