package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

/**
 * 封装和预计算悬浮窗界面的布局参数、坐标映射以及裁剪区域 (Scissor)。
 * 该类会在每一帧基于当前的动画进度和屏幕缩放比例重新计算数据，确保渲染坐标精确无误。
 */
public class OverlayLayout {
    public final float baseX, baseY;
    public final float finalScale;
    public final float slideOffset;
    public final float globalAlpha;
    public final float visibleRows;
    public final int panelWidth;
    public final int startY;
    public final int itemHeightTotal;

    private final int scX, scW;
    private final int scY_strict, scH_strict;
    private final int scY_loose, scH_loose;

    public OverlayLayout(Minecraft mc, float popupProgress) {
        var cfg = BetterLootingConfig.get();
        this.globalAlpha = (float) cfg.globalAlpha;
        this.panelWidth = cfg.panelWidth;
        this.visibleRows = (float) cfg.visibleRows;

        Window win = mc.getWindow();
        double guiScale = win.getGuiScale();

        // 计算 UI 的基础锚点位置（屏幕中心点 + 配置文件偏移量）
        this.baseX = (float) (win.getGuiScaledWidth() / 2.0f + cfg.xOffset);
        this.baseY = (float) (win.getGuiScaledHeight() / 2.0f + cfg.yOffset);

        // 结合配置的缩放和弹出动画缓动曲线，计算当前帧的实际缩放比例
        this.finalScale = (float) (cfg.uiScale * Utils.easeOutBack(popupProgress));
        this.slideOffset = (1.0f - popupProgress) * 30.0f;

        this.startY = -(Constants.ITEM_HEIGHT / 2);
        this.itemHeightTotal = Constants.ITEM_HEIGHT + 2;

        /* ======================== 裁剪区域 (Scissor) 计算 ========================
         * RenderSystem.enableScissor 需要的是 OpenGL 底层的真实屏幕像素坐标，
         * 因此所有逻辑坐标都需要乘以 guiScale 进行转换，并处理坐标系翻转（OpenGL 原点在左下角）。
         */

        // 计算 X 轴的物理裁剪坐标与宽度
        double localLeft = Constants.LIST_X - 25.0;
        double currentXOnScreen = this.baseX + ((localLeft + slideOffset) * finalScale);
        this.scX = (int) (currentXOnScreen * guiScale);
        this.scW = (int) ((panelWidth + 30.0) * finalScale * guiScale);

        // 计算 Y 轴相关的物理高度参数
        double listTopOnScreen = this.baseY + (this.startY * finalScale);
        int listPhyH = (int) ((visibleRows * itemHeightTotal) * finalScale * guiScale);
        int topBuffer = (int) (itemHeightTotal * 1.5 * finalScale * guiScale);

        // 严格裁剪区域：主要用于截断滑动出界的列表元素
        // 注意：OpenGL 中 Y 轴是向上增长的，所以用窗口高度减去 GUI 坐标
        this.scY_strict = (int) (win.getHeight() - (listTopOnScreen * guiScale) - listPhyH);
        this.scH_strict = listPhyH + topBuffer;

        // 宽松裁剪区域：外扩了一定像素(looseExt)，允许一些装饰性元素（如选中框、按键提示）稍微超出列表边界
        int looseExt = (int) (50.0 * guiScale);
        this.scY_loose = scY_strict - looseExt;
        this.scH_loose = scH_strict + (looseExt * 2);
    }

    /**
     * 应用严格裁剪模式，适用于渲染滚动列表时的物品行限制。
     */
    public void applyStrictScissor() {
        setScissor(scY_strict, scH_strict);
    }

    /**
     * 应用宽松裁剪模式，适用于渲染按键提示或选中特效，防止它们被列表边界生硬截断。
     */
    public void applyLooseScissor() {
        setScissor(scY_loose, scH_loose);
    }

    /**
     * 启用底层 OpenGL 裁剪测试 (Scissor Test)，参数已被限制为正数以防崩溃。
     */
    private void setScissor(int y, int h) {
        RenderSystem.enableScissor(
                Math.max(0, scX),
                Math.max(0, y),
                Math.max(1, scW),
                Math.max(1, h)
        );
    }
}