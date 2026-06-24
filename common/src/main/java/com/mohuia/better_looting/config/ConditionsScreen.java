package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.gui.CycleButton;
import com.mohuia.better_looting.client.gui.GuiTheme;
import com.mohuia.better_looting.client.gui.SelectButton;
import com.mohuia.better_looting.client.gui.TabButton;
import com.mohuia.better_looting.client.gui.ThemedSlider;
import com.mohuia.better_looting.client.gui.ToggleButton;
import com.mohuia.better_looting.config.BetterLootingConfig.ActivationMode;
import com.mohuia.better_looting.config.BetterLootingConfig.AnimationSpeed;
import com.mohuia.better_looting.config.BetterLootingConfig.DisplayMode;
import com.mohuia.better_looting.config.BetterLootingConfig.PickupInterceptMode;
import com.mohuia.better_looting.config.BetterLootingConfig.ScrollMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 触发与配置界面。顶部现代标签栏，主区域为纵向滚动列表，简约黑白主题。
 */
public class ConditionsScreen extends Screen {

    private final Screen parent;
    private final ConfigViewModel viewModel;

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 6;
    private static final int TAB_H = 24;
    private static final int BACK_SIZE = 24;   // 左上角返回箭头尺寸
    private static final int PANEL_PAD = 12;    // 边框内边距（组件与边框间距）

    // 圆角面板 9-slice 纹理
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "textures/gui/panel_bg.png");
    private static final int PANEL_SLICE = 48;   // 纹理中角块大小（48px，含 12px 圆角 + 边框，按超采样降采样到 GUI）
    private static final int PANEL_TEX_SIZE = 256; // 纹理边长
    private static final int PANEL_GUI_SLICE = 12; // 角块在 GUI 上的绘制尺寸（决定圆角视觉半径）

    // --- 布局变量 ---
    // mainX/mainWidth 固定；mainHeight = 内容总高（随组件增减）；
    // 边框顶部实际渲染 Y = mainBaseY - scrollAmount（整个边框带组件一起滚动）。
    private int mainX, mainBaseY, mainWidth, mainHeight;
    private int mainCenterX;
    private int contentStartY;       // 第一个组件相对"边框内"的起始 Y（含内边距），不含滚动偏移
    private int contentX, contentWidth;
    private int tabBarY;
    private int viewportTop, viewportBottom; // 可视区（边框允许出现的纵向范围）

    // --- 滚动相关 ---
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> originalYMap = new HashMap<>(); // 组件"无滚动"时的绝对 Y
    private double scrollAmount = 0;     // 当前渲染用滚动量（缓动逼近目标）
    private double targetScroll = 0;     // 目标滚动量（滚轮/拖动直接设置）
    private int maxScroll = 0;
    private long lastScrollFrameMs = 0;  // 上一帧时间戳，用于帧率无关缓动
    // 缓动时间常数（毫秒）：越大越慢越顺，越小越跟手
    private static final double SCROLL_SMOOTH_TAU = 70.0;
    // 滚动条拖动状态
    private boolean draggingScrollbar = false;
    private double dragStartMouseY = 0;
    private double dragStartScroll = 0;

    // 分组标题（前置式：标题在其分组之上）
    private record Section(int y, Component label) {}
    private final List<Section> sections = new ArrayList<>();

    // --- 横向滑动过渡 ---
    private final com.mohuia.better_looting.client.gui.SlideAnimator slideAnim =
            new com.mohuia.better_looting.client.gui.SlideAnimator();
    private Page outgoingPage = null; // 滑出中的旧页快照（仅用于渲染，不接收输入）

    /** 一页的渲染快照：保存绘制该页所需的全部状态。 */
    private static final class Page {
        final List<AbstractWidget> widgets;
        final List<Section> sections;
        final boolean showCustomTitleLabel;
        final int customTitleLabelY;
        final boolean showNewLabelLabel;
        final int newLabelLabelY;
        final int keyInfoY;
        final Category category;
        final double scrollAmount;
        final int mainHeight;

        Page(List<AbstractWidget> widgets, List<Section> sections,
             boolean showCustomTitleLabel, int customTitleLabelY,
             boolean showNewLabelLabel, int newLabelLabelY,
             int keyInfoY, Category category, double scrollAmount, int mainHeight) {
            this.widgets = widgets;
            this.sections = sections;
            this.showCustomTitleLabel = showCustomTitleLabel;
            this.customTitleLabelY = customTitleLabelY;
            this.showNewLabelLabel = showNewLabelLabel;
            this.newLabelLabelY = newLabelLabelY;
            this.keyInfoY = keyInfoY;
            this.category = category;
            this.scrollAmount = scrollAmount;
            this.mainHeight = mainHeight;
        }
    }

    /** 用当前界面状态生成一份页面快照（拷贝列表，避免后续 init 清空时受影响）。 */
    private Page snapshotCurrentPage() {
        return new Page(new ArrayList<>(scrollableWidgets), new ArrayList<>(sections),
                showCustomTitleLabel, customTitleLabelY, showNewLabelLabel, newLabelLabelY,
                keyInfoY, currentCategory, scrollAmount, mainHeight);
    }

    // 文本框标题坐标
    private int customTitleLabelY;
    private boolean showCustomTitleLabel = false;
    private int newLabelLabelY;
    private boolean showNewLabelLabel = false;
    private int keyInfoY; // 键位提示在面板内的起始 Y（无滚动绝对坐标）

    private enum Category {
        APPEARANCE("hud_appearance"),
        TRIGGER("trigger_conditions"),
        ADVANCED("advanced");

        final String langKey;
        Category(String langKey) { this.langKey = langKey; }
        Component getDisplayName() { return Component.translatable("gui." + BetterLooting.MODID + "." + langKey); }
    }

    private Category currentCategory = Category.APPEARANCE;

    public ConditionsScreen(Screen parent, ConfigViewModel viewModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".conditions_title"));
        this.parent = parent;
        this.viewModel = viewModel;
    }

    private void calculateLayout() {
        // Tab 栏：顶部，左侧给返回箭头让位
        this.tabBarY = 0;

        // 内容面板水平布局（固定）
        this.mainX = 8;
        this.mainWidth = this.width - 16;
        this.mainCenterX = mainX + mainWidth / 2;

        // 可视区：标签栏下方到屏幕底部之间，边框可在此范围内滚动
        this.viewportTop = tabBarY + TAB_H + 4;
        this.viewportBottom = this.height - 8;

        // 边框基准顶部（scrollAmount=0 时的位置）
        this.mainBaseY = viewportTop;

        // 内容起点：边框内顶部 + 内边距
        this.contentStartY = mainBaseY + PANEL_PAD;

        // 内容区居中，宽屏时上限更大
        this.contentWidth = Math.min(560, mainWidth - PANEL_PAD * 2 - 12);
        this.contentX = mainCenterX - contentWidth / 2;
    }

    @Override
    protected void init() {
        this.scrollableWidgets.clear();
        this.originalYMap.clear();
        this.sections.clear();

        calculateLayout();
        this.showCustomTitleLabel = false;
        this.showNewLabelLabel = false;

        // 左上角返回箭头（贴角，从 (0,0) 开始）
        this.addRenderableWidget(new com.mohuia.better_looting.client.gui.BackButton(
                0, tabBarY + (TAB_H - BACK_SIZE) / 2, BACK_SIZE,
                () -> this.minecraft.setScreen(parent)));

        buildTopTabBar();

        int startY = contentStartY;
        switch (currentCategory) {
            case APPEARANCE -> buildAppearanceTab(startY);
            case TRIGGER -> buildTriggerTab(startY);
            case ADVANCED -> buildAdvancedTab(startY);
        }

        // 计算内容底部（所有组件 / 标题 / 标签的最大底边）
        int contentBottom = contentStartY;
        for (AbstractWidget w : scrollableWidgets) {
            contentBottom = Math.max(contentBottom, originalYMap.get(w) + w.getHeight());
        }
        if (showCustomTitleLabel) contentBottom = Math.max(contentBottom, customTitleLabelY + 10);
        if (showNewLabelLabel) contentBottom = Math.max(contentBottom, newLabelLabelY + 10);
        for (Section s : sections) contentBottom = Math.max(contentBottom, s.y + font.lineHeight + 6);

        // 键位提示作为面板内最后内容，占额外高度
        this.keyInfoY = contentBottom + 6;
        int keyInfoLines = countKeyInfoLines();
        if (keyInfoLines > 0) {
            contentBottom = keyInfoY + keyInfoLines * 12;
        }

        // 边框高度 = 内容总高 + 上下内边距；可视区放不下时按可视区裁顶部并启用滚动
        int desiredHeight = (contentBottom - mainBaseY) + PANEL_PAD;
        int viewportH = viewportBottom - viewportTop;
        this.mainHeight = desiredHeight;
        this.maxScroll = Math.max(0, desiredHeight - viewportH);

        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll));
        this.targetScroll = Math.max(0, Math.min(this.targetScroll, this.maxScroll));
        updateWidgetPositions();
    }

    /** 当前分类下键位提示的行数（用于把它纳入面板高度）。 */
    private int countKeyInfoLines() {
        if (currentCategory != Category.TRIGGER) return 0;
        int lines = 0;
        boolean activationHadKey = (viewModel.activationMode == ActivationMode.KEY_HOLD || viewModel.activationMode == ActivationMode.KEY_TOGGLE);
        if (activationHadKey) lines++;
        if (viewModel.scrollMode == ScrollMode.KEY_BIND || viewModel.scrollMode == ScrollMode.INVERT_KEY) lines++;
        return lines;
    }

    // --- 滚动管理 ---

    private <T extends AbstractWidget> T addScrollableWidget(T widget) {
        this.addWidget(widget);
        this.scrollableWidgets.add(widget);
        this.originalYMap.put(widget, widget.getY());
        return widget;
    }

    /** 添加前置式分组标题，返回其占用的垂直高度。 */
    private int addSectionHeader(int y, String sectionKey) {
        sections.add(new Section(y, Component.translatable("gui." + BetterLooting.MODID + ".config.section." + sectionKey)));
        return font.lineHeight + 12;
    }

    private void updateWidgetPositions() {
        for (AbstractWidget widget : scrollableWidgets) {
            int newY = (int) (originalYMap.get(widget) - scrollAmount);
            widget.setY(newY);
            // 仅在可视区内可见
            widget.visible = (newY + widget.getHeight() > viewportTop) && (newY < viewportBottom);
        }
    }

    /** 每帧将 scrollAmount 帧率无关地缓动逼近 targetScroll，实现丝滑滚动。 */
    private void stepScrollEasing() {
        long now = System.currentTimeMillis();
        long dt = (lastScrollFrameMs == 0) ? 16 : (now - lastScrollFrameMs);
        lastScrollFrameMs = now;
        if (dt <= 0) return;

        double diff = targetScroll - scrollAmount;
        if (Math.abs(diff) < 0.5) {
            // 足够接近则吸附，避免长尾抖动
            if (scrollAmount != targetScroll) {
                scrollAmount = targetScroll;
                updateWidgetPositions();
            }
            return;
        }
        // 指数缓动：alpha = 1 - e^(-dt/tau)，dt 越大步进越多，帧率无关
        double alpha = 1.0 - Math.exp(-dt / SCROLL_SMOOTH_TAU);
        scrollAmount += diff * alpha;
        updateWidgetPositions();
    }

    /** 滑动动画进行中时屏蔽内容区交互。 */
    private boolean isSliding() {
        return slideAnim.isAnimating() && outgoingPage != null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isSliding()) return true;
        if (mouseY >= viewportTop && mouseY <= viewportBottom) {
            if (maxScroll > 0) {
                // 只改目标值，由 render 每帧缓动逼近，实现丝滑滚动
                this.targetScroll -= scrollY * 40.0;
                this.targetScroll = Math.max(0, Math.min(this.targetScroll, this.maxScroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 滑动动画期间，吞掉内容区点击；标签栏区域放行以便继续切换
        if (isSliding()) {
            if (mouseY >= viewportTop && mouseY <= viewportBottom) return true;
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0 && maxScroll > 0) {
            int[] sb = scrollbarBounds();
            int handleY = scrollbarHandleY(sb[1], sb[3]);
            int handleH = scrollbarHandleHeight(sb[3]);
            if (mouseX >= sb[0] && mouseX <= sb[0] + sb[2]
                    && mouseY >= handleY && mouseY <= handleY + handleH) {
                this.draggingScrollbar = true;
                this.dragStartMouseY = mouseY;
                this.dragStartScroll = scrollAmount;
                return true;
            }
        }
        // 屏蔽可视区外（如被标签栏遮挡处）对滚动组件的误触：仅在标签栏带内放行非滚动控件
        if (mouseY < viewportTop || mouseY > viewportBottom) {
            for (AbstractWidget w : scrollableWidgets) {
                if (w.visible && w.isMouseOver(mouseX, mouseY)) {
                    return true; // 命中被裁出可视区的滚动组件 → 吞掉点击
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && maxScroll > 0) {
            int[] sb = scrollbarBounds();
            int handleH = scrollbarHandleHeight(sb[3]);
            int travel = sb[3] - handleH; // 滑块可移动距离
            if (travel > 0) {
                double deltaScroll = (mouseY - dragStartMouseY) / travel * maxScroll;
                this.scrollAmount = Math.max(0, Math.min(maxScroll, dragStartScroll + deltaScroll));
                this.targetScroll = this.scrollAmount; // 拖动跟手，目标同步避免松手回弹
                updateWidgetPositions();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // --- 顶部横向标签栏 ---

    private void buildTopTabBar() {
        Category[] cats = Category.values();
        // 左侧给返回箭头让位（标签栏紧贴箭头右缘）
        int startX = BACK_SIZE;
        int avail = this.width - startX;
        int tabWidth = avail / cats.length;

        int x = startX;
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            boolean isSelected = (currentCategory == cat);
            // 最后一个标签补足剩余宽度，避免取整留缝
            int w = (i == cats.length - 1) ? (this.width - x) : tabWidth;
            this.addRenderableWidget(new TabButton(x, tabBarY, w, TAB_H, cat.getDisplayName(), isSelected, () -> {
                if (cat == this.currentCategory) return;
                // 切换前拍下当前页作为滑出快照，按分类顺序决定方向
                this.outgoingPage = snapshotCurrentPage();
                int dir = (cat.ordinal() > this.currentCategory.ordinal()) ? 1 : -1;
                this.slideAnim.start(dir);
                this.currentCategory = cat;
                this.scrollAmount = 0;
                this.targetScroll = 0;
                this.clearWidgets();
                this.init();
            }));
            x += w;
        }
    }

    // =============================================
    // HUD 外观
    // =============================================

    private void buildAppearanceTab(int startY) {
        int y = startY;
        int x = contentX;
        int w = contentWidth;

        // —— 文本 ——
        y += addSectionHeader(y, "text");

        this.showCustomTitleLabel = true;
        this.customTitleLabelY = y;
        y += 11;
        EditBox titleInputBox = new EditBox(this.font, x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"));
        titleInputBox.setMaxLength(32);
        titleInputBox.setValue(viewModel.customOverlayTitle != null ? viewModel.customOverlayTitle : "");
        titleInputBox.setResponder(text -> viewModel.customOverlayTitle = text);
        titleInputBox.setTooltip(Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.custom_title")));
        this.addScrollableWidget(titleInputBox);
        y += ROW_H + ROW_GAP + 4;

        this.showNewLabelLabel = true;
        this.newLabelLabelY = y;
        y += 11;
        EditBox newLabelInputBox = new EditBox(this.font, x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.new_label_text"));
        newLabelInputBox.setMaxLength(16);
        newLabelInputBox.setValue(viewModel.newLabelText != null ? viewModel.newLabelText : "NEW");
        newLabelInputBox.setResponder(text -> viewModel.newLabelText = text);
        newLabelInputBox.setTooltip(Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.new_label_text")));
        this.addScrollableWidget(newLabelInputBox);
        y += ROW_H + ROW_GAP + 4;

        // —— 外观 ——
        y += addSectionHeader(y, "appearance");

        // 皮肤循环
        this.addScrollableWidget(new CycleButton(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.overlay_skin"),
                () -> getSkinName(viewModel.overlaySkin),
                () -> cycleSkin(1), () -> cycleSkin(-1), getSkinTooltip()));
        y += ROW_H + ROW_GAP;

        // 动画速度循环
        this.addScrollableWidget(new CycleButton(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.animation_speed"),
                () -> getAnimationSpeedName(viewModel.animationSpeed),
                () -> cycleEnum(AnimationSpeed.values(), viewModel.animationSpeed, v -> viewModel.animationSpeed = v, 1),
                () -> cycleEnum(AnimationSpeed.values(), viewModel.animationSpeed, v -> viewModel.animationSpeed = v, -1),
                getAnimationSpeedTooltip(viewModel.animationSpeed)));
        y += ROW_H + ROW_GAP + 4;

        // —— 数量显示 ——
        y += addSectionHeader(y, "count_display");

        this.addScrollableWidget(new CycleButton(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_display_mode"),
                () -> getDisplayModeName(viewModel.itemCountDisplayMode),
                () -> cycleEnum(DisplayMode.values(), viewModel.itemCountDisplayMode, v -> viewModel.itemCountDisplayMode = v, 1),
                () -> cycleEnum(DisplayMode.values(), viewModel.itemCountDisplayMode, v -> viewModel.itemCountDisplayMode = v, -1),
                Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.item_count_display_mode"))));
        y += ROW_H + ROW_GAP;

        if (viewModel.itemCountDisplayMode != DisplayMode.OFF) {
            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_scale"),
                    "x", 0.25, 5.0, (double) viewModel.itemCountScale, 2,
                    val -> viewModel.itemCountScale = (float) (Math.round(val * 100.0) / 100.0)));
            y += ROW_H + ROW_GAP;

            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_render_distance"),
                    "m", 4.0, 64.0, (double) viewModel.itemCountRenderDistance, 0,
                    val -> viewModel.itemCountRenderDistance = (int) Math.round(val)));
            y += ROW_H + ROW_GAP;
        }
        y += 4;

        // —— 附加面板 ——
        y += addSectionHeader(y, "extra_panels");

        y = addToggle(x, y, w, "inventory_loot_list",
                () -> viewModel.showInventoryLootList, () -> viewModel.showInventoryLootList = !viewModel.showInventoryLootList);
        y = addToggle(x, y, w, "tooltip_preview",
                () -> viewModel.enableTooltipPreview, () -> viewModel.enableTooltipPreview = !viewModel.enableTooltipPreview);
        addToggle(x, y, w, "hotbar_indicator",
                () -> viewModel.showHotbarIndicator, () -> viewModel.showHotbarIndicator = !viewModel.showHotbarIndicator);
    }
    // =============================================
    // 触发条件
    // =============================================

    private void buildTriggerTab(int startY) {
        int y = startY;
        int x = contentX;
        int w = contentWidth;

        // —— 激活触发器 ——
        y += addSectionHeader(y, "activation");
        y = buildEnumSelectList(x, y, w, ActivationMode.values(), viewModel.activationMode,
                mode -> viewModel.activationMode = mode, this::getModeName, this::getModeTooltip);
        y += 4;

        // —— 滚动行为 ——
        y += addSectionHeader(y, "scroll_behavior");
        y = buildEnumSelectList(x, y, w, ScrollMode.values(), viewModel.scrollMode,
                mode -> viewModel.scrollMode = mode, this::getScrollModeName, this::getScrollModeTooltip);
        y += 4;

        // —— 拾取时机 ——
        y += addSectionHeader(y, "pickup_timing");

        this.addScrollableWidget(new CycleButton(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_intercept_mode_title"),
                () -> getInterceptModeName(viewModel.pickupInterceptMode),
                () -> cycleEnum(PickupInterceptMode.values(), viewModel.pickupInterceptMode, v -> viewModel.pickupInterceptMode = v, 1),
                () -> cycleEnum(PickupInterceptMode.values(), viewModel.pickupInterceptMode, v -> viewModel.pickupInterceptMode = v, -1),
                getInterceptModeTooltip(viewModel.pickupInterceptMode)));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_delay"),
                "s", 0.0, 5.0, (double) viewModel.pickupDelaySeconds, 1,
                val -> viewModel.pickupDelaySeconds = (float) (Math.round(val * 10.0) / 10.0)));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.max_hold_seconds"),
                "s", 0.5, 5.0, (double) viewModel.maxHoldTicks / 20.0, 1,
                val -> {
                    float seconds = (float) (Math.round(val * 10.0) / 10.0);
                    viewModel.maxHoldTicks = (int) (seconds * 20);
                }));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.stability_threshold"),
                "tick", 0.0, 20.0, (double) viewModel.stabilityThresholdTicks, 1,
                val -> viewModel.stabilityThresholdTicks = (int) Math.round(val)));
    }
    // =============================================
    // 高级
    // =============================================

    private void buildAdvancedTab(int startY) {
        int y = startY;
        int x = contentX;
        int w = contentWidth;

        // —— 过滤 ——
        y += addSectionHeader(y, "filter");
        y = addToggle(x, y, w, "rare_item_filter",
                () -> viewModel.enableRareItemFilter, () -> viewModel.enableRareItemFilter = !viewModel.enableRareItemFilter);
        y += 4;

        // —— 合并 ——
        y += addSectionHeader(y, "merge");
        y = addToggle(x, y, w, "super_merge",
                () -> viewModel.enableSuperMerge, () -> viewModel.enableSuperMerge = !viewModel.enableSuperMerge);

        if (viewModel.enableSuperMerge) {
            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_xz"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeXZ, 1,
                    val -> viewModel.mergeRangeXZ = (float) (Math.round(val * 10.0) / 10.0)));
            y += ROW_H + ROW_GAP;

            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_y"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeY, 1,
                    val -> viewModel.mergeRangeY = (float) (Math.round(val * 10.0) / 10.0)));
        }
    }

    // --- 通用构建辅助 ---

    /** 添加一个开关行并返回下一个 y。 */
    private int addToggle(int x, int y, int w, String key, java.util.function.BooleanSupplier getter, Runnable toggle) {
        Component label = Component.translatable("gui." + BetterLooting.MODID + ".config." + key);
        this.addScrollableWidget(new ToggleButton(x, y, w, ROW_H, label, getter, () -> {
            toggle.run();
            this.clearWidgets();
            this.init();
        }, null));
        return y + ROW_H + ROW_GAP;
    }

    /** 构建枚举单选列表（每项一行），返回下一个 y。LOOK_DOWN 选中时插入角度滑块。 */
    private <T extends Enum<T>> int buildEnumSelectList(int x, int startY, int w, T[] values, T current,
                                                        Consumer<T> setter,
                                                        Function<T, Component> nameProvider,
                                                        Function<T, Tooltip> tooltipProvider) {
        int y = startY;
        for (T mode : values) {
            boolean isSelected = (mode == current);
            this.addScrollableWidget(new SelectButton(x, y, w, ROW_H, nameProvider.apply(mode), isSelected, () -> {
                setter.accept(mode);
                this.clearWidgets();
                this.init();
            }, tooltipProvider.apply(mode)));
            y += ROW_H + ROW_GAP;

            if (mode == ActivationMode.LOOK_DOWN && isSelected) {
                this.addScrollableWidget(new ThemedSlider(x + 16, y, w - 16, ROW_H,
                        Component.translatable("gui." + BetterLooting.MODID + ".angle"),
                        "", 0.0, 90.0, (double) viewModel.lookDownAngle, 0,
                        val -> viewModel.lookDownAngle = val.floatValue()));
                y += ROW_H + ROW_GAP;
            }
        }
        return y;
    }

    /** 循环切换枚举值并刷新界面。 */
    private <T extends Enum<T>> void cycleEnum(T[] values, T current, Consumer<T> setter, int dir) {
        int next = (current.ordinal() + dir + values.length) % values.length;
        setter.accept(values[next]);
        this.clearWidgets();
        this.init();
    }

    /** 循环切换皮肤并刷新界面。 */
    private void cycleSkin(int dir) {
        List<String> skins = com.mohuia.better_looting.client.skin.SkinManager.INSTANCE.getAvailableSkins();
        if (skins.isEmpty()) return;
        int idx = skins.indexOf(viewModel.overlaySkin);
        if (idx < 0) idx = 0;
        viewModel.overlaySkin = skins.get((idx + dir + skins.size()) % skins.size());
        this.clearWidgets();
        this.init();
    }
    // =============================================
    // 渲染
    // =============================================

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 先画原版模糊/渐变背景
        super.renderBackground(gui, mouseX, mouseY, partialTick);

        // 2. 在上面画自定义面板内容（不会被 super.render 再次遮盖）
        stepScrollEasing();

        boolean animating = slideAnim.isAnimating() && outgoingPage != null;

        // ==== Scissor：限制在可视区内 ====
        gui.enableScissor(mainX, viewportTop, mainX + mainWidth, viewportBottom);

        if (animating) {
            double p = slideAnim.progress();
            int dir = slideAnim.direction();
            int travel = mainWidth;
            int oldOffset = (int) Math.round(-dir * travel * p);
            int newOffset = (int) Math.round(dir * travel * (1.0 - p));
            renderPage(gui, outgoingPage, oldOffset, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick, true);
            renderPage(gui, snapshotCurrentPage(), newOffset, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick, true);
        } else {
            if (outgoingPage != null) outgoingPage = null;
            renderPage(gui, snapshotCurrentPage(), 0, mouseX, mouseY, partialTick, false);
        }

        gui.disableScissor();

        // 滚动条（边框外侧），动画期间隐藏
        if (!animating && maxScroll > 0) {
            renderScrollBar(gui, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // super.render 内部先调 renderBackground（面板内容），再画标签栏/返回箭头
        super.render(gui, mouseX, mouseY, partialTick);
    }

    /** 渲染单个页面（面板 + 分组标题 + 文本框标签 + 组件 + 键位提示），整体横向偏移 xOffset。 */
    private void renderPage(GuiGraphics gui, Page page, int xOffset, int mouseX, int mouseY, float partialTick, boolean forceVisible) {
        gui.pose().pushPose();
        gui.pose().translate(xOffset, 0, 0);

        int panelY = (int) (mainBaseY - page.scrollAmount); // 边框随滚动整体移动

        // 内容面板（边框，随滚动移动；高度=内容总高）
        renderPanelBackground(gui, mainX, panelY, mainWidth, page.mainHeight);

        // 分组标题（相对边框固定，随滚动移动）
        for (Section s : page.sections) {
            renderSectionHeader(gui, (int) (s.y - page.scrollAmount), s.label);
        }

        // 文本框标签
        if (page.showCustomTitleLabel) {
            gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"),
                    contentX + 1, (int) (page.customTitleLabelY - page.scrollAmount), GuiTheme.TEXT_MUTED, false);
        }
        if (page.showNewLabelLabel) {
            gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.new_label_text"),
                    contentX + 1, (int) (page.newLabelLabelY - page.scrollAmount), GuiTheme.TEXT_MUTED, false);
        }

        // 滚动组件（动画期间强制可见，越界部分由 scissor 裁剪）
        for (AbstractWidget widget : page.widgets) {
            boolean prev = widget.visible;
            if (forceVisible) widget.visible = true;
            widget.render(gui, mouseX, mouseY, partialTick);
            if (forceVisible) widget.visible = prev;
        }

        // 键位提示（面板内最后内容，随滚动移动）
        renderContextKeyInfo(gui, page);

        gui.pose().popPose();
    }

    /** 前置式分组标题：左侧短强调竖条 + 文字 + 向右延伸的细分隔线。 */
    private void renderSectionHeader(GuiGraphics gui, int y, Component label) {
        int textW = font.width(label);
        gui.fill(contentX, y, contentX + 2, y + font.lineHeight, GuiTheme.ACCENT);
        gui.drawString(font, label, contentX + 7, y, GuiTheme.ACCENT, false);
        int lineStart = contentX + 7 + textW + 8;
        int lineY = y + font.lineHeight / 2;
        gui.fill(lineStart, lineY, contentX + contentWidth, lineY + 1, GuiTheme.DIVIDER_LINE);
    }

    /** 滚动条轨道范围：返回 {x, y, width, height}，位于边框右外侧、可视区内。 */
    private int[] scrollbarBounds() {
        int barW = 4;
        int barX = mainX + mainWidth + 4; // 边框外侧
        int barY = viewportTop + 2;
        int barH = (viewportBottom - viewportTop) - 4;
        return new int[]{barX, barY, barW, barH};
    }

    private int scrollbarHandleHeight(int trackH) {
        int total = trackH + maxScroll;
        return Math.max(20, (int) ((float) trackH * trackH / total));
    }

    private int scrollbarHandleY(int trackY, int trackH) {
        int handleH = scrollbarHandleHeight(trackH);
        if (maxScroll <= 0) return trackY;
        return trackY + (int) ((scrollAmount / maxScroll) * (trackH - handleH));
    }

    private void renderScrollBar(GuiGraphics gui, int mouseX, int mouseY) {
        int[] sb = scrollbarBounds();
        int barX = sb[0], barY = sb[1], barW = sb[2], barH = sb[3];
        int handleH = scrollbarHandleHeight(barH);
        int handleY = scrollbarHandleY(barY, barH);

        boolean overThumb = draggingScrollbar || (mouseX >= barX && mouseX <= barX + barW
                && mouseY >= handleY && mouseY <= handleY + handleH);

        gui.fill(barX, barY, barX + barW, barY + barH, GuiTheme.SCROLLBAR_TRACK);
        gui.fill(barX, handleY, barX + barW, handleY + handleH,
                overThumb ? GuiTheme.SCROLLBAR_THUMB_HOVER : GuiTheme.SCROLLBAR_THUMB);
    }

    private void renderContextKeyInfo(GuiGraphics gui, Page page) {
        int infoY = (int) (page.keyInfoY - page.scrollAmount); // 面板内最后内容，随滚动移动

        if (page.category == Category.TRIGGER) {
            boolean activationHadKey = (viewModel.activationMode == ActivationMode.KEY_HOLD || viewModel.activationMode == ActivationMode.KEY_TOGGLE);
            if (activationHadKey) {
                drawKeyString(gui, mainCenterX, infoY, KeyInit.SHOW_OVERLAY, "config.key_info");
            }
            if (viewModel.scrollMode == ScrollMode.KEY_BIND || viewModel.scrollMode == ScrollMode.INVERT_KEY) {
                drawKeyString(gui, mainCenterX, activationHadKey ? infoY + 12 : infoY, KeyInit.SCROLL_MODIFIER, "config.scroll_key_info");
            }
        }
    }

    private void renderPanelBackground(GuiGraphics gui, int x, int y, int w, int h) {
        // 1.21.1 纹理加载可能失败，先兜底填充，再尝试 9-slice 圆角纹理
        boolean textured = false;
        try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(PANEL_TEXTURE);
            tex.setFilter(true, false); // 线性过滤（min/mag = LINEAR）

            final int ts = PANEL_TEX_SIZE;   // 纹理边长
            final int tc = PANEL_SLICE;      // 纹理角块尺寸
            final int gc = PANEL_GUI_SLICE;  // GUI 角块尺寸
            final int tcInner = ts - tc * 2; // 纹理中段
            final int gInnerW = w - gc * 2;  // GUI 横向中段
            final int gInnerH = h - gc * 2;  // GUI 纵向中段

            // 四角
            blitTex(gui, x,           y,           gc, gc, 0,        0,        tc,      tc);
            blitTex(gui, x + w - gc,  y,           gc, gc, ts - tc,  0,        tc,      tc);
            blitTex(gui, x,           y + h - gc,  gc, gc, 0,        ts - tc,  tc,      tc);
            blitTex(gui, x + w - gc,  y + h - gc,  gc, gc, ts - tc,  ts - tc,  tc,      tc);
            // 四边
            blitTex(gui, x + gc,      y,           gInnerW, gc,      tc,      0,        tcInner, tc);
            blitTex(gui, x + gc,      y + h - gc,  gInnerW, gc,      tc,      ts - tc,  tcInner, tc);
            blitTex(gui, x,           y + gc,      gc,      gInnerH, 0,       tc,       tc,      tcInner);
            blitTex(gui, x + w - gc,  y + gc,      gc,      gInnerH, ts - tc, tc,       tc,      tcInner);
            // 中心
            blitTex(gui, x + gc,      y + gc,      gInnerW, gInnerH, tc,      tc,       tcInner, tcInner);
            textured = true;
        } catch (Exception ignored) {
            // 纹理不可用时回退到纯色填充
        }
        if (!textured) {
            gui.fill(x, y, x + w, y + h, GuiTheme.PANEL_BG);
            gui.fill(x, y, x + w, y + 1, GuiTheme.PANEL_BORDER);
            gui.fill(x, y + h - 1, x + w, y + h, GuiTheme.PANEL_BORDER);
        }
    }

    /** 带任意源/目标尺寸缩放的 blit（源区域会被线性过滤缩放到目标区域）。 */
    private void blitTex(GuiGraphics gui, int dx, int dy, int dw, int dh,
                         float u, float v, int sw, int sh) {
        gui.blit(PANEL_TEXTURE, dx, dy, dw, dh, u, v, sw, sh, PANEL_TEX_SIZE, PANEL_TEX_SIZE);
    }

    private void drawKeyString(GuiGraphics gui, int x, int y, KeyMapping key, String langKey) {
        Component keyName = key.getTranslatedKeyMessage();
        int color = key.isUnbound() ? 0xFFFF5555 : 0xFF55FF55;
        gui.drawCenteredString(this.font, Component.translatable("gui." + BetterLooting.MODID + "." + langKey, keyName), x, y, color);
    }

    // =============================================
    // 名称 / Tooltip 获取
    // =============================================

    private Component getModeName(ActivationMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.mode." + mode.name().toLowerCase());
    }

    private Tooltip getModeTooltip(ActivationMode mode) {
        return Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip." + mode.name().toLowerCase()));
    }

    private Component getScrollModeName(ScrollMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.scroll." + mode.name().toLowerCase());
    }

    private Tooltip getScrollModeTooltip(ScrollMode mode) {
        return Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.scroll." + mode.name().toLowerCase()));
    }

    private Component getInterceptModeName(PickupInterceptMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_intercept." + mode.name().toLowerCase());
    }

    private Tooltip getInterceptModeTooltip(PickupInterceptMode mode) {
        return Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.pickup_intercept." + mode.name().toLowerCase()));
    }

    private Component getSkinName(String skin) {
        var ext = com.mohuia.better_looting.client.skin.SkinManager.INSTANCE.getExternalSkin(skin);
        if (ext != null) return Component.literal(ext.displayName);
        if (com.mohuia.better_looting.client.skin.SkinManager.isBuiltin(skin)) {
            return Component.translatable("gui." + BetterLooting.MODID + ".config.overlay_skin." + skin);
        }
        return Component.literal(skin);
    }

    private Tooltip getSkinTooltip() {
        return Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.overlay_skin"));
    }

    private Component getDisplayModeName(DisplayMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_display_mode." + mode.name().toLowerCase());
    }

    private Component getAnimationSpeedName(AnimationSpeed speed) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.animation_speed." + speed.name().toLowerCase());
    }

    private Tooltip getAnimationSpeedTooltip(AnimationSpeed speed) {
        return Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.animation_speed." + speed.name().toLowerCase()));
    }
}
