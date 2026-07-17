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

    // 新物品标签文本（可自定义，留空则隐藏）
    public String newLabelText;

    // 悬浮窗物品行背景皮肤
    public String overlaySkin;

    // 物品栏左侧掉落物列表开关
    public boolean showInventoryLootList;

    // 物品栏掉落列表布局（独立配置界面编辑）
    public int inventoryListWidth;
    public float inventoryListXOffset;
    public float inventoryListYOffset;
    public float inventoryListScale;
    public float inventoryListAlpha;
    public int inventoryListHeight;

    // 物品信息预览开关
    public boolean enableTooltipPreview;

    // 左侧按键提示框开关
    public boolean showKeyPrompt;

    // 悬浮窗编辑状态
    public float indicatorX, indicatorY;
    public int indicatorRotation;

    public BetterLootingConfig.AnimationSpeed animationSpeed;
    public BetterLootingConfig.ActivationMode activationMode;
    public BetterLootingConfig.ScrollMode scrollMode;
    public BetterLootingConfig.PickupInterceptMode pickupInterceptMode;
    public BetterLootingConfig.LongPressMode longPressMode;
    public float lookDownAngle;

    // 白名单稀有物品过滤开关
    public boolean enableRareItemFilter;

    // 超大堆叠合并设置
    public boolean enableSuperMerge;
    public float mergeRangeXZ;
    public float mergeRangeY;
    public String mergeTransportBlacklist;

    // 拾取保护时间延迟
    public float pickupDelaySeconds;
    public int maxHoldTicks;

    // HUD 稳定过滤阈值
    public int stabilityThresholdTicks;

    // 掉落物上方数量文字显示
    public BetterLootingConfig.DisplayMode itemCountDisplayMode;
    public float itemCountScale;
    public int itemCountRenderDistance;

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
        this.animationSpeed = cfg.animationSpeed;
        this.activationMode = cfg.activationMode;
        this.scrollMode = cfg.scrollMode;
        this.pickupInterceptMode = cfg.pickupInterceptMode;
        this.longPressMode = cfg.longPressMode;
        this.lookDownAngle = cfg.lookDownAngle;
        this.showHotbarIndicator = cfg.showHotbarIndicator;

        // 读取自定义标题；做 null 保护，避免输入框或渲染阶段空指针
        this.customOverlayTitle = cfg.customOverlayTitle == null ? "" : cfg.customOverlayTitle;

        this.newLabelText = cfg.newLabelText == null ? "NEW" : cfg.newLabelText;

        this.overlaySkin = cfg.overlaySkin;

        this.showInventoryLootList = cfg.showInventoryLootList;
        this.inventoryListWidth = cfg.inventoryListWidth;
        this.inventoryListXOffset = cfg.inventoryListXOffset;
        this.inventoryListYOffset = cfg.inventoryListYOffset;
        this.inventoryListScale = cfg.inventoryListScale;
        this.inventoryListAlpha = cfg.inventoryListAlpha;
        this.inventoryListHeight = cfg.inventoryListHeight;
        this.enableTooltipPreview = cfg.enableTooltipPreview;
        this.showKeyPrompt = cfg.showKeyPrompt;

        this.indicatorX = cfg.indicatorX;
        this.indicatorY = cfg.indicatorY;
        this.indicatorRotation = cfg.indicatorRotation;

        this.pickupDelaySeconds = cfg.pickupDelaySeconds;
        this.maxHoldTicks = cfg.maxHoldTicks;
        this.stabilityThresholdTicks = cfg.stabilityThresholdTicks;

        this.enableRareItemFilter = cfg.enableRareItemFilter;
        this.enableSuperMerge = cfg.enableSuperMerge;
        this.mergeRangeXZ = cfg.mergeRangeXZ;
        this.mergeRangeY = cfg.mergeRangeY;
        this.mergeTransportBlacklist = cfg.mergeTransportBlacklist == null ? "belt,conveyor,chute,funnel,depot" : cfg.mergeTransportBlacklist;

        this.itemCountDisplayMode = cfg.itemCountDisplayMode;
        this.itemCountScale = cfg.itemCountScale;
        this.itemCountRenderDistance = cfg.itemCountRenderDistance;
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
        cfg.animationSpeed = this.animationSpeed;
        cfg.activationMode = this.activationMode;
        cfg.scrollMode = this.scrollMode;
        cfg.pickupInterceptMode = this.pickupInterceptMode;
        cfg.longPressMode = this.longPressMode;
        cfg.lookDownAngle = this.lookDownAngle;
        cfg.showHotbarIndicator = this.showHotbarIndicator;

        // 保存自定义标题；null 一律转为空字符串，保证配置稳定
        cfg.customOverlayTitle = this.customOverlayTitle == null ? "" : this.customOverlayTitle;

        cfg.newLabelText = this.newLabelText == null ? "NEW" : this.newLabelText;

        cfg.overlaySkin = this.overlaySkin;

        cfg.showInventoryLootList = this.showInventoryLootList;
        cfg.inventoryListWidth = this.inventoryListWidth;
        cfg.inventoryListXOffset = this.inventoryListXOffset;
        cfg.inventoryListYOffset = this.inventoryListYOffset;
        cfg.inventoryListScale = this.inventoryListScale;
        cfg.inventoryListAlpha = this.inventoryListAlpha;
        cfg.inventoryListHeight = this.inventoryListHeight;
        cfg.enableTooltipPreview = this.enableTooltipPreview;
        cfg.showKeyPrompt = this.showKeyPrompt;

        cfg.indicatorX = this.indicatorX;
        cfg.indicatorY = this.indicatorY;
        cfg.indicatorRotation = this.indicatorRotation;

        cfg.pickupDelaySeconds = this.pickupDelaySeconds;
        cfg.maxHoldTicks = this.maxHoldTicks;
        cfg.stabilityThresholdTicks = this.stabilityThresholdTicks;

        cfg.enableRareItemFilter = this.enableRareItemFilter;
        cfg.enableSuperMerge = this.enableSuperMerge;
        cfg.mergeRangeXZ = this.mergeRangeXZ;
        cfg.mergeRangeY = this.mergeRangeY;
        cfg.mergeTransportBlacklist = this.mergeTransportBlacklist;

        cfg.itemCountDisplayMode = this.itemCountDisplayMode;
        cfg.itemCountScale = this.itemCountScale;
        cfg.itemCountRenderDistance = this.itemCountRenderDistance;

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
        this.animationSpeed = defaults.animationSpeed;
        this.activationMode = defaults.activationMode;
        this.scrollMode = defaults.scrollMode;
        this.pickupInterceptMode = defaults.pickupInterceptMode;
        this.longPressMode = defaults.longPressMode;
        this.lookDownAngle = defaults.lookDownAngle;
        this.showHotbarIndicator = defaults.showHotbarIndicator;

        // 恢复默认标题；同样做 null 保护
        this.customOverlayTitle = defaults.customOverlayTitle == null ? "" : defaults.customOverlayTitle;

        this.newLabelText = defaults.newLabelText == null ? "NEW" : defaults.newLabelText;

        this.overlaySkin = defaults.overlaySkin;

        this.showInventoryLootList = defaults.showInventoryLootList;
        this.inventoryListWidth = defaults.inventoryListWidth;
        this.inventoryListXOffset = defaults.inventoryListXOffset;
        this.inventoryListYOffset = defaults.inventoryListYOffset;
        this.inventoryListScale = defaults.inventoryListScale;
        this.inventoryListAlpha = defaults.inventoryListAlpha;
        this.inventoryListHeight = defaults.inventoryListHeight;
        this.enableTooltipPreview = defaults.enableTooltipPreview;
        this.showKeyPrompt = defaults.showKeyPrompt;

        this.indicatorX = defaults.indicatorX;
        this.indicatorY = defaults.indicatorY;
        this.indicatorRotation = defaults.indicatorRotation;

        this.pickupDelaySeconds = defaults.pickupDelaySeconds;
        this.maxHoldTicks = defaults.maxHoldTicks;
        this.stabilityThresholdTicks = defaults.stabilityThresholdTicks;

        this.enableRareItemFilter = defaults.enableRareItemFilter;
        this.enableSuperMerge = defaults.enableSuperMerge;
        this.mergeRangeXZ = defaults.mergeRangeXZ;
        this.mergeRangeY = defaults.mergeRangeY;
        this.mergeTransportBlacklist = defaults.mergeTransportBlacklist;

        this.itemCountDisplayMode = defaults.itemCountDisplayMode;
        this.itemCountScale = defaults.itemCountScale;
        this.itemCountRenderDistance = defaults.itemCountRenderDistance;
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