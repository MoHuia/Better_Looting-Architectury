package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 快捷栏旁的过滤器状态指示器 (HUD Overlay)。
 * 负责在游戏主界面渲染当前过滤器的模式（全部拾取 / 仅拾取稀有物品），
 * 让玩家无需打开特定界面即可一目了然。
 */
public class HotbarIndicator {
    public static final HotbarIndicator INSTANCE = new HotbarIndicator();

    /**
     * HUD 的核心渲染逻辑。
     * 需要被挂载到 Architectury 的 HudRenderEvent 渲染事件中。
     */
    public void render(GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // 意图 1：如果玩家按下了 F1 隐藏了 GUI，或者当前打开了任何界面（如物品栏、暂停菜单），则不渲染指示器。
        if (mc.options.hideGui || mc.screen != null) return;

        // 意图 2：如果玩家处于旁观者模式 (Spectator)，原版快捷栏会被隐藏或替换，此时我们的指示器也应该隐藏。
        if (mc.gameMode != null && !mc.gameMode.canHurtPlayer() && mc.player != null && mc.player.isSpectator()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 坐标计算：基于屏幕中心点进行相对定位。
        // 原版快捷栏的总宽度是 182 像素。91 是一半的宽度。
        // 加上 6 像素的边距，意味着指示器会紧贴在快捷栏最右侧稍微偏外的位置。
        int x = screenWidth / 2 + 91 + 6;

        // 快捷栏加上底部的默认边距通常占用 22 像素的高度。
        // 减去 22 后加上 4 像素的微调，让指示器与快捷栏的槽位在视觉上垂直居中对齐。
        int startY = screenHeight - 22 + 4;

        // 获取当前的核心过滤器模式（假设定义在 Core 模块中）
        Core.FilterMode mode = Core.INSTANCE.getFilterMode();

        // 准备 OpenGL 渲染状态：开启透明度混合，确保带 Alpha 通道的颜色能正确显示。
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制两个模式的指示灯。
        // 如果当前是 ALL 模式，上方的白色灯亮起；如果当前是 RARE_ONLY，下方的金色灯亮起。
        drawIndicator(gui, x, startY, mode == Core.FilterMode.ALL, 0xFFFFFFFF);
        drawIndicator(gui, x, startY + 8, mode == Core.FilterMode.RARE_ONLY, 0xFFFFD700); // 0xFFFFD700 为标准的金色 (Gold)

        // 重置渲染状态，防止影响后续原版或其他模组的 UI 渲染
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 绘制单个状态指示灯。
     * @param active 当前模式是否处于激活状态
     * @param color  指示灯的主题色
     */
    private void drawIndicator(GuiGraphics gui, int x, int y, boolean active, int color) {
        int size = 6;

        // 颜色计算：
        // 如果激活，背景使用带有 50% 透明度的原色 (0x80)；未激活则使用 25% 透明度的纯黑 (0x40000000)。
        int bgColor = active ? (color & 0x00FFFFFF) | 0x80000000 : 0x40000000;

        // 边框颜色：激活时为不透明原色，未激活时调用自定义 Utils 工具类应用半透明效果。
        int borderColor = active ? color : Utils.colorWithAlpha(color, 120);

        // 1. 绘制带有 1 像素“倒角”效果的圆角矩形背景
        renderRoundedRect(gui, x, y, size, size, bgColor);

        // 2. 绘制 1 像素宽的描边
        gui.renderOutline(x, y, size, size, borderColor);

        // 3. 视觉点缀：如果处于激活状态，在指示灯右侧额外绘制一条短小的高亮竖线，增强激活状态的视觉反馈。
        if (active) {
            gui.fill(x + size + 1, y + 1, x + size + 2, y + size - 1, color);
        }
    }

    /**
     * 使用两个交叠的矩形拼凑出一个简单的圆角矩形。
     * 这是一种非常轻量级且聪明的像素级 UI 绘制技巧，避免了复杂的 Shader 或纹理调用。
     */
    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        // 绘制一个横向稍窄、纵向充满的矩形
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        // 绘制一个横向充满、纵向稍窄的矩形，二者交叠后四角就会空出 1 像素，形成圆角效果
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }
}