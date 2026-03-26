package com.mohuia.better_looting.config;

import com.mohuia.better_looting.client.Constants;
import net.minecraft.util.Mth;

/**
 * 配置界面的视图模型（ViewModel）。
 * 负责在内存中暂存用户修改的配置项，处理缩放、位移和行列数的数学计算，
 * 并与核心配置类（BetterLootingConfig）进行数据交互。
 */
public class ConfigViewModel {

    // ==========================================
    // 当前编辑状态 (Current Editing State)
    // ==========================================
    public float xOffset, yOffset, uiScale;
    public int panelWidth;
    public float visibleRows;
    public float globalAlpha;
    public boolean showHotbarIndicator;

    // 悬浮窗标题文本（可自定义，也可留空）
    public String customOverlayTitle;

    // 悬浮窗编辑状态
    public float indicatorX, indicatorY;
    public int indicatorRotation;

    public BetterLootingConfig.ActivationMode activationMode;
    public BetterLootingConfig.ScrollMode scrollMode;
    public float lookDownAngle;

    // 超大堆叠合并设置
    public boolean enableSuperMerge;
    public float mergeRangeXZ;
    public float mergeRangeY;

    // ==========================================
    // 拖拽快照状态 (Drag Snapshot State)
    // 用于记录鼠标按下瞬间的数值，配合差值计算避免累加误差
    // ==========================================
    private float initX, initY, initScale, initRows;
    private int initWidth;

    public ConfigViewModel() {
        loadFromConfig();
    }

    /**
     * 从全局配置读取当前值，初始化编辑状态。
     */
    public void loadFromConfig() {
        BetterLootingConfig cfg = BetterLootingConfig.get();
        this.xOffset = cfg.xOffset;
        this.yOffset = cfg.yOffset;
        this.uiScale = cfg.uiScale;
        this.panelWidth = cfg.panelWidth;
        this.visibleRows = cfg.visibleRows;
        this.globalAlpha = cfg.globalAlpha;
        this.activationMode = cfg.activationMode;
        this.scrollMode = cfg.scrollMode;
        this.lookDownAngle = cfg.lookDownAngle;
        this.showHotbarIndicator = cfg.showHotbarIndicator;

        // 读取自定义标题；做 null 保护，避免输入框或渲染阶段空指针
        this.customOverlayTitle = cfg.customOverlayTitle == null ? "" : cfg.customOverlayTitle;

        this.indicatorX = cfg.indicatorX;
        this.indicatorY = cfg.indicatorY;
        this.indicatorRotation = cfg.indicatorRotation;

        this.enableSuperMerge = cfg.enableSuperMerge;
        this.mergeRangeXZ = cfg.mergeRangeXZ;
        this.mergeRangeY = cfg.mergeRangeY;
    }

    /**
     * 将当前的编辑状态写回全局配置并保存到本地文件。
     */
    public void saveToConfig() {
        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.xOffset = this.xOffset;
        cfg.yOffset = this.yOffset;
        cfg.uiScale = this.uiScale;
        cfg.panelWidth = this.panelWidth;
        cfg.visibleRows = this.visibleRows;
        cfg.globalAlpha = this.globalAlpha;
        cfg.activationMode = this.activationMode;
        cfg.scrollMode = this.scrollMode;
        cfg.lookDownAngle = this.lookDownAngle;
        cfg.showHotbarIndicator = this.showHotbarIndicator;

        // 保存自定义标题；null 一律转为空字符串，保证配置稳定
        cfg.customOverlayTitle = this.customOverlayTitle == null ? "" : this.customOverlayTitle;

        cfg.indicatorX = this.indicatorX;
        cfg.indicatorY = this.indicatorY;
        cfg.indicatorRotation = this.indicatorRotation;

        cfg.enableSuperMerge = this.enableSuperMerge;
        cfg.mergeRangeXZ = this.mergeRangeXZ;
        cfg.mergeRangeY = this.mergeRangeY;

        cfg.validate(); // 保存前最后校验一次，确保数据绝对安全
        BetterLootingConfig.save();
    }

    /**
     * 恢复所有视觉和交互设置为默认值。
     */
    public void resetToDefault() {
        BetterLootingConfig defaults = new BetterLootingConfig();
        this.xOffset = defaults.xOffset;
        this.yOffset = defaults.yOffset;
        this.uiScale = defaults.uiScale;
        this.panelWidth = defaults.panelWidth;
        this.visibleRows = defaults.visibleRows;
        this.globalAlpha = defaults.globalAlpha;
        this.activationMode = defaults.activationMode;
        this.scrollMode = defaults.scrollMode;
        this.lookDownAngle = defaults.lookDownAngle;
        this.showHotbarIndicator = defaults.showHotbarIndicator;

        // 恢复默认标题；同样做 null 保护
        this.customOverlayTitle = defaults.customOverlayTitle == null ? "" : defaults.customOverlayTitle;

        this.indicatorX = defaults.indicatorX;
        this.indicatorY = defaults.indicatorY;
        this.indicatorRotation = defaults.indicatorRotation;

        this.enableSuperMerge = defaults.enableSuperMerge;
        this.mergeRangeXZ = defaults.mergeRangeXZ;
        this.mergeRangeY = defaults.mergeRangeY;
    }

    /**
     * 用于打包返回边界坐标的 Record 类。
     */
    public record PreviewBounds(float left, float top, float right, float bottom) {}

    /**
     * 计算预览面板在屏幕上的实际像素边界（考虑了偏移量和缩放比例）。
     * 用于确定剪裁区域（Scissor）以及鼠标拖拽的热区检测。
     */
    public PreviewBounds calculatePreviewBounds(int screenWidth, int screenHeight) {
        // 1. 计算基准原点（屏幕中心点 + 用户自定义偏移）
        float baseX = (float) (screenWidth / 2.0f + this.xOffset);
        float baseY = (float) (screenHeight / 2.0f + this.yOffset);
        float scale = this.uiScale;

        // 2. 计算相对坐标
        float itemHeight = Constants.ITEM_HEIGHT;
        float startY = -(itemHeight / 2);
        float localMinY = startY - 14; // 顶部留出标题空间
        float localHeight = (this.visibleRows * (itemHeight + 2)) + 14; // 基于可见行数计算总高度

        // 3. 应用缩放并映射到绝对坐标
        float left = baseX + (Constants.LIST_X * scale);
        float right = left + (this.panelWidth * scale);
        float top = baseY + (localMinY * scale);
        float bottom = top + (localHeight * scale);

        return new PreviewBounds(left, top, right, bottom);
    }

    // --- 拖拽参数更新逻辑 ---

    /**
     * 在每次拖拽开始前调用，记录面板当前的各种属性，作为基准。
     */
    public void captureSnapshot() {
        this.initX = xOffset;
        this.initY = yOffset;
        this.initScale = uiScale;
        this.initWidth = panelWidth;
        this.initRows = visibleRows;
    }

    public void updatePosition(double deltaX, double deltaY) {
        this.xOffset = initX + (float) deltaX;
        this.yOffset = initY + (float) deltaY;
    }

    public void updateWidth(double deltaX) {
        // 宽度调整需消除缩放比例的影响，确保鼠标移动距离与视觉变化一致
        float scaledDelta = (float) deltaX / uiScale;
        this.panelWidth = (int) Mth.clamp(initWidth + scaledDelta, 80, 500);
    }

    public void updateRows(double deltaY) {
        float itemTotalHeight = Constants.ITEM_HEIGHT + 2;
        float scaledDelta = (float) deltaY / uiScale;
        float rowDelta = scaledDelta / itemTotalHeight; // 将像素差值转换为行数差值
        this.visibleRows = Mth.clamp(initRows + rowDelta, 1.0f, 20.0f);
    }

    public void updateScale(double deltaX, double deltaY) {
        float sensitivity = 0.005f; // 调整缩放的灵敏度
        this.uiScale = Mth.clamp(initScale + (float) (deltaX + deltaY) * sensitivity, 0.1f, 4.0f);
    }
}