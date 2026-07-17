package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.gui.Dropdown;
import com.mohuia.better_looting.client.gui.GuiTheme;
import com.mohuia.better_looting.client.gui.SelectButton;
import com.mohuia.better_looting.client.gui.TabButton;
import com.mohuia.better_looting.client.gui.ThemedSlider;
import com.mohuia.better_looting.client.gui.ToggleButton;
import com.mohuia.better_looting.config.BetterLootingConfig.ActivationMode;
import com.mohuia.better_looting.config.BetterLootingConfig.AnimationSpeed;
import com.mohuia.better_looting.config.BetterLootingConfig.DisplayMode;
import com.mohuia.better_looting.config.BetterLootingConfig.LongPressMode;
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
public class ConditionsScreen extends Screen implements Dropdown.Host {

    private final Screen parent;
    private final ConfigViewModel viewModel;

    // 当前展开的下拉框（同一时刻至多一个），由其在顶层渲染浮层并优先接管输入
    private Dropdown openDropdown = null;

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 6;
    private static final int TAB_H = 24;
    private static final int BACK_SIZE = 24;   // 左上角返回箭头尺寸
    private static final int PANEL_PAD = 12;    // 边框内边距（组件与边框间距）

    // 圆角面板 9-slice 纹理
    private static final ResourceLocation PANEL_TEXTURE = new ResourceLocation(BetterLooting.MODID, "textures/gui/panel_bg.png");
    private static final int PANEL_SLICE = 48;   // 纹理中角块大小（48px，含 12px 圆角 + 边框，按超采样降采样到 GUI）
    private static final int PANEL_TEX_SIZE = 256; // 纹理边长
    private static final int PANEL_GUI_SLICE = 12; // 角块在 GUI 上的绘制尺寸（决定圆角视觉半径）

    // --- 布局变量 ---
    // mainX/mainWidth 固定；mainHeight = 内容总高（随组件增减）；
    // 边框顶部实际渲染 Y = mainBaseY - scrollAmount（整个边框带组件一起滚动）。
    private int mainX, mainBaseY, mainWidth, mainHeight;
    private int mainCenterX;
    private int contentStartY;       // 第一个组件相对“边框内”的起始 Y（含内边距），不含滚动偏移
    private int contentX, contentWidth;
    private int tabBarY;
    private int viewportTop, viewportBottom; // 可视区（边框允许出现的纵向范围）

    // --- 滚动相关 ---
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> originalYMap = new HashMap<>(); // 组件“无滚动”时的绝对 Y
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

    // 分组标题（前置式：标题在其分组之上），含分区专属强调色
    private record Section(int y, Component label, int accentColor) {}
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
        final int activationKeyInfoY; // 激活段键位提示 Y，-1 表示不显示
        final int scrollKeyInfoY;     // 滚动段键位提示 Y，-1 表示不显示
        final Category category;
        final double scrollAmount;
        final int mainHeight;

        Page(List<AbstractWidget> widgets, List<Section> sections,
             boolean showCustomTitleLabel, int customTitleLabelY,
             boolean showNewLabelLabel, int newLabelLabelY,
             int activationKeyInfoY, int scrollKeyInfoY,
             Category category, double scrollAmount, int mainHeight) {
            this.widgets = widgets;
            this.sections = sections;
            this.showCustomTitleLabel = showCustomTitleLabel;
            this.customTitleLabelY = customTitleLabelY;
            this.showNewLabelLabel = showNewLabelLabel;
            this.newLabelLabelY = newLabelLabelY;
            this.activationKeyInfoY = activationKeyInfoY;
            this.scrollKeyInfoY = scrollKeyInfoY;
            this.category = category;
            this.scrollAmount = scrollAmount;
            this.mainHeight = mainHeight;
        }
    }

    /** 用当前界面状态生成一份页面快照（拷贝列表，避免后续 init 清空时受影响）。 */
    private Page snapshotCurrentPage() {
        return new Page(new ArrayList<>(scrollableWidgets), new ArrayList<>(sections),
                showCustomTitleLabel, customTitleLabelY, showNewLabelLabel, newLabelLabelY,
                activationKeyInfoY, scrollKeyInfoY, currentCategory, scrollAmount, mainHeight);
    }

    // 文本框标题坐标
    private int customTitleLabelY;
    private boolean showCustomTitleLabel = false;
    private int newLabelLabelY;
    private boolean showNewLabelLabel = false;
    // 键位提示 Y（无滚动绝对坐标），-1 表示当前页不显示该段提示
    private int activationKeyInfoY = -1;
    private int scrollKeyInfoY = -1;

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

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
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
        this.openDropdown = null; // 组件将被重建，清空展开引用

        calculateLayout();
        this.showCustomTitleLabel = false;
        this.showNewLabelLabel = false;
        this.activationKeyInfoY = -1;
        this.scrollKeyInfoY = -1;

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

        // 计算内容底部（所有组件 / 标题 / 标签 / 分段键位提示的最大底边）
        int contentBottom = contentStartY;
        for (AbstractWidget w : scrollableWidgets) {
            contentBottom = Math.max(contentBottom, originalYMap.get(w) + w.getHeight());
        }
        if (showCustomTitleLabel) contentBottom = Math.max(contentBottom, customTitleLabelY + 10);
        if (showNewLabelLabel) contentBottom = Math.max(contentBottom, newLabelLabelY + 10);
        for (Section s : sections) contentBottom = Math.max(contentBottom, s.y + font.lineHeight + 6);
        if (activationKeyInfoY >= 0) contentBottom = Math.max(contentBottom, activationKeyInfoY + 12);
        if (scrollKeyInfoY >= 0)     contentBottom = Math.max(contentBottom, scrollKeyInfoY + 12);

        // 边框高度 = 内容总高 + 上下内边距；可视区放不下时按可视区裁顶部并启用滚动
        int desiredHeight = (contentBottom - mainBaseY) + PANEL_PAD;
        int viewportH = viewportBottom - viewportTop;
        this.mainHeight = desiredHeight;
        this.maxScroll = Math.max(0, desiredHeight - viewportH);

        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll));
        this.targetScroll = Math.max(0, Math.min(this.targetScroll, this.maxScroll));
        updateWidgetPositions();
    }

    // --- 下拉框宿主回调 ---

    @Override
    public void onDropdownOpen(Dropdown dropdown) {
        // 同一时刻至多展开一个：先收起此前展开的
        if (openDropdown != null && openDropdown != dropdown) {
            openDropdown.collapse();
        }
        openDropdown = dropdown;
    }

    @Override
    public void onDropdownClose(Dropdown dropdown) {
        if (openDropdown == dropdown) {
            openDropdown = null;
        }
    }

    // --- 滚动管理 ---

    private <T extends AbstractWidget> T addScrollableWidget(T widget) {
        this.addWidget(widget);
        this.scrollableWidgets.add(widget);
        this.originalYMap.put(widget, widget.getY());
        return widget;
    }

    /** 添加前置式分组标题，返回其占用的垂直高度。 */
    private int addSectionHeader(int y, String sectionKey, int accentColor) {
        sections.add(new Section(y, Component.translatable("gui." + BetterLooting.MODID + ".config.section." + sectionKey), accentColor));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isSliding()) return true;
        // 展开的下拉框优先处理浮层内部滚动
        if (openDropdown != null && openDropdown.handleExpandedScroll(mouseX, mouseY, delta)) {
            return true;
        }
        if (mouseY >= viewportTop && mouseY <= viewportBottom) {
            if (maxScroll > 0) {
                // 列表滚动会使浮层与行错位，先收起展开的下拉框
                if (openDropdown != null) openDropdown.collapse();
                // 只改目标值，由 render 每帧缓动逼近，实现丝滑滚动
                this.targetScroll -= delta * 40.0;
                this.targetScroll = Math.max(0, Math.min(this.targetScroll, this.maxScroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 展开的下拉框最优先：其浮层覆盖在其他组件之上
        if (openDropdown != null && button == 0) {
            Dropdown d = openDropdown;
            if (d.handleExpandedClick(mouseX, mouseY)) {
                return true;
            }
            // 点击浮层外：handleExpandedClick 已收起，继续让点击落到下层组件
        }
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
        y += addSectionHeader(y, "text", GuiTheme.SECTION_TEXT);

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
        y += addSectionHeader(y, "appearance", GuiTheme.SECTION_APPEARANCE);

        // 皮肤下拉框
        addSkinDropdown(x, y, w,
                Component.translatable("gui." + BetterLooting.MODID + ".config.overlay_skin"),
                null);
        y += ROW_H + ROW_GAP;

        // 动画速度下拉框
        addEnumDropdown(x, y, w,
                Component.translatable("gui." + BetterLooting.MODID + ".config.animation_speed"),
                AnimationSpeed.values(), viewModel.animationSpeed, v -> viewModel.animationSpeed = v,
                this::getAnimationSpeedName,
                null);
        y += ROW_H + ROW_GAP + 4;

        // —— 数量显示 ——
        y += addSectionHeader(y, "count_display", GuiTheme.SECTION_COUNT);

        addEnumDropdown(x, y, w,
                Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_display_mode"),
                DisplayMode.values(), viewModel.itemCountDisplayMode, v -> viewModel.itemCountDisplayMode = v,
                this::getDisplayModeName,
                null);
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
        y += addSectionHeader(y, "extra_panels", GuiTheme.SECTION_PANELS);

        y = addToggle(x, y, w, "inventory_loot_list",
                () -> viewModel.showInventoryLootList, () -> viewModel.showInventoryLootList = !viewModel.showInventoryLootList);
        y = addToggle(x, y, w, "tooltip_preview",
                () -> viewModel.enableTooltipPreview, () -> viewModel.enableTooltipPreview = !viewModel.enableTooltipPreview);
        y = addToggle(x, y, w, "key_prompt",
                () -> viewModel.showKeyPrompt, () -> viewModel.showKeyPrompt = !viewModel.showKeyPrompt);
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
        y += addSectionHeader(y, "activation", GuiTheme.SECTION_ACTIVATION);
        y = buildEnumSelectList(x, y, w, ActivationMode.values(), viewModel.activationMode,
                mode -> viewModel.activationMode = mode, this::getModeName, this::getModeTooltip);
        // 激活段键位提示紧跟该段组件下方
        boolean activationHadKey = (viewModel.activationMode == ActivationMode.KEY_HOLD || viewModel.activationMode == ActivationMode.KEY_TOGGLE);
        if (activationHadKey) {
            this.activationKeyInfoY = y;
            y += 12;
        }
        y += 4;

        // —— 滚动行为 ——
        y += addSectionHeader(y, "scroll_behavior", GuiTheme.SECTION_SCROLL);
        y = buildEnumSelectList(x, y, w, ScrollMode.values(), viewModel.scrollMode,
                mode -> viewModel.scrollMode = mode, this::getScrollModeName, this::getScrollModeTooltip);
        // 滚动段键位提示紧跟该段组件下方
        boolean scrollHadKey = (viewModel.scrollMode == ScrollMode.KEY_BIND || viewModel.scrollMode == ScrollMode.INVERT_KEY);
        if (scrollHadKey) {
            this.scrollKeyInfoY = y;
            y += 12;
        }
        y += 4;

        // —— 拾取时机 ——
        y += addSectionHeader(y, "pickup_timing", GuiTheme.SECTION_PICKUP);

        addEnumDropdown(x, y, w,
                Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_intercept_mode_title"),
                PickupInterceptMode.values(), viewModel.pickupInterceptMode, v -> viewModel.pickupInterceptMode = v,
                this::getInterceptModeName,
                getInterceptModeTooltip(viewModel.pickupInterceptMode));
        y += ROW_H + ROW_GAP;

        addEnumDropdown(x, y, w,
                Component.translatable("gui." + BetterLooting.MODID + ".config.long_press_mode"),
                LongPressMode.values(), viewModel.longPressMode, v -> viewModel.longPressMode = v,
                this::getLongPressModeName,
                Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.long_press_mode.tooltip")));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_delay"),
                "s", 0.0, 5.0, (double) viewModel.pickupDelaySeconds, 1,
                val -> viewModel.pickupDelaySeconds = (float) (Math.round(val * 10.0) / 10.0),
                Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_delay.tooltip"))));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.max_hold_seconds"),
                "s", 0.0, 5.0, (double) viewModel.maxHoldTicks / 20.0, 1,
                val -> {
                    float seconds = (float) (Math.round(val * 10.0) / 10.0);
                    viewModel.maxHoldTicks = (int) (seconds * 20);
                },
                Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.max_hold_seconds.tooltip"))));
        y += ROW_H + ROW_GAP;

        this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                Component.translatable("gui." + BetterLooting.MODID + ".config.stability_threshold"),
                "tick", 0.0, 20.0, (double) viewModel.stabilityThresholdTicks, 1,
                val -> viewModel.stabilityThresholdTicks = (int) Math.round(val),
                Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.stability_threshold.tooltip"))));
    }
    // =============================================
    // 高级
    // =============================================

    private void buildAdvancedTab(int startY) {
        int y = startY;
        int x = contentX;
        int w = contentWidth;

        // —— 过滤 ——
        y += addSectionHeader(y, "filter", GuiTheme.SECTION_FILTER);
        y = addToggle(x, y, w, "rare_item_filter",
                () -> viewModel.enableRareItemFilter, () -> viewModel.enableRareItemFilter = !viewModel.enableRareItemFilter);
        y += 4;

        // —— 合并 ——
        y += addSectionHeader(y, "merge", GuiTheme.SECTION_MERGE);
        y = addToggle(x, y, w, "super_merge",
                () -> viewModel.enableSuperMerge, () -> viewModel.enableSuperMerge = !viewModel.enableSuperMerge, true);

        if (viewModel.enableSuperMerge) {
            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_xz"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeXZ, 1,
                    val -> viewModel.mergeRangeXZ = (float) (Math.round(val * 10.0) / 10.0),
                    Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_xz.tooltip"))));
            y += ROW_H + ROW_GAP;

            this.addScrollableWidget(new ThemedSlider(x, y, w, ROW_H,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_y"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeY, 1,
                    val -> viewModel.mergeRangeY = (float) (Math.round(val * 10.0) / 10.0),
                    Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_y.tooltip"))));
        }
    }

    // --- 通用构建辅助 ---

    /** 添加一个开关行并返回下一个 y。默认不重建界面以消除闪烁，仅当开关会影响其他组件显隐时才需要重建。 */
    private int addToggle(int x, int y, int w, String key, java.util.function.BooleanSupplier getter, Runnable toggle) {
        return addToggle(x, y, w, key, getter, toggle, false);
    }

    /** 添加一个开关行，needsRebuild=true 时点击会重建整个界面。 */
    private int addToggle(int x, int y, int w, String key, java.util.function.BooleanSupplier getter, Runnable toggle, boolean needsRebuild) {
        Component label = Component.translatable("gui." + BetterLooting.MODID + ".config." + key);
        Tooltip tooltip = Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config." + key + ".tooltip"));
        this.addScrollableWidget(new ToggleButton(x, y, w, ROW_H, label, getter, () -> {
            toggle.run();
            if (needsRebuild) {
                this.clearWidgets();
                this.init();
            }
        }, tooltip));
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
                        val -> viewModel.lookDownAngle = val.floatValue(),
                        Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".angle.tooltip"))));
                y += ROW_H + ROW_GAP;
            }
        }
        return y;
    }

    /** 添加一个枚举下拉框（选项 = 枚举各值），选中即应用并刷新界面。 */
    private <T extends Enum<T>> void addEnumDropdown(int x, int y, int w, Component label,
                                                     T[] values, T current, Consumer<T> setter,
                                                     Function<T, Component> nameProvider, Tooltip tooltip) {
        List<Component> options = new ArrayList<>();
        for (T v : values) options.add(nameProvider.apply(v));
        int selected = current.ordinal();
        this.addScrollableWidget(new Dropdown(x, y, w, ROW_H, label,
                () -> options, () -> selected,
                idx -> {
                    setter.accept(values[idx]);
                    this.clearWidgets();
                    this.init();
                }, this, tooltip));
    }

    /** 添加皮肤下拉框（选项 = 可用皮肤列表），选中即应用并刷新界面。 */
    private void addSkinDropdown(int x, int y, int w, Component label, Tooltip tooltip) {
        List<String> skins = com.mohuia.better_looting.client.skin.SkinManager.INSTANCE.getAvailableSkins();
        List<Component> options = new ArrayList<>();
        for (String s : skins) options.add(getSkinName(s));
        int selected = Math.max(0, skins.indexOf(viewModel.overlaySkin));
        this.addScrollableWidget(new Dropdown(x, y, w, ROW_H, label,
                () -> options, () -> selected,
                idx -> {
                    if (idx >= 0 && idx < skins.size()) {
                        viewModel.overlaySkin = skins.get(idx);
                        this.clearWidgets();
                        this.init();
                    }
                }, this, tooltip));
    }
    // =============================================
    // 渲染
    // =============================================

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);

        stepScrollEasing();

        boolean animating = slideAnim.isAnimating() && outgoingPage != null;

        // ==== Scissor：限制在可视区内（标签栏下方到屏幕底部），滑出/滑入部分被裁掉 ====
        gui.enableScissor(mainX, viewportTop, mainX + mainWidth, viewportBottom);

        // 展开的下拉框浮层盖住下层组件时，用虚假坐标渲染背景，阻止穿透高亮
        boolean popupCovering = !animating && openDropdown != null && openDropdown.isExpanded()
                && !openDropdown.isMouseOver(mouseX, mouseY)
                && openDropdown.isMouseOverExpanded(mouseX, mouseY);

        if (animating) {
            double p = slideAnim.progress();
            int dir = slideAnim.direction();
            int travel = mainWidth;
            int oldOffset = (int) Math.round(-dir * travel * p);
            int newOffset = (int) Math.round(dir * travel * (1.0 - p));
            // 动画期间屏蔽悬停态（鼠标坐标移到可视区外），强制两页组件可见
            renderPage(gui, outgoingPage, oldOffset, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick, true);
            renderPage(gui, snapshotCurrentPage(), newOffset, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick, true);
        } else {
            if (outgoingPage != null) outgoingPage = null; // 动画结束，释放旧页快照
            int rx = popupCovering ? Integer.MIN_VALUE / 2 : mouseX;
            int ry = popupCovering ? Integer.MIN_VALUE / 2 : mouseY;
            renderPage(gui, snapshotCurrentPage(), 0, rx, ry, partialTick, false);
        }

        gui.disableScissor();

        // 滚动条（边框外侧），动画期间隐藏
        if (!animating && maxScroll > 0) {
            renderScrollBar(gui, mouseX, mouseY);
        }

        // 渲染标签栏按钮、返回箭头
        super.render(gui, mouseX, mouseY, partialTick);

        // 展开的下拉框浮层：最顶层渲染，脱离裁剪区，盖住一切
        if (!animating && openDropdown != null && openDropdown.isExpanded()) {
            openDropdown.renderPopup(gui, mouseX, mouseY);
        }
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
            renderSectionHeader(gui, (int) (s.y - page.scrollAmount), s);
        }

        // 文本框标签
        if (page.showCustomTitleLabel) {
            gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"),
                    contentX + 1, (int) (page.customTitleLabelY - page.scrollAmount), GuiTheme.TEXT, false);
        }
        if (page.showNewLabelLabel) {
            gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.new_label_text"),
                    contentX + 1, (int) (page.newLabelLabelY - page.scrollAmount), GuiTheme.TEXT, false);
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

    /** 前置式分组标题：左侧加宽强调竖条 + 半透明背景条 + 文字，使用分区专属强调色。 */
    private void renderSectionHeader(GuiGraphics gui, int y, Section section) {
        int color = section.accentColor;
        // 文字色亮色，背景色用同色系低透明度
        int bgColor = (color & 0x00FFFFFF) | 0x1A000000;
        // 半透明背景条（从左竖条到内容区右边界）
        gui.fill(contentX, y, contentX + contentWidth, y + font.lineHeight, bgColor);
        // 4px 宽强调竖条（左侧）
        gui.fill(contentX, y, contentX + 4, y + font.lineHeight, color);
        // 标题文字（同色）
        gui.drawString(font, section.label, contentX + 9, y, color, false);
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
        if (page.category != Category.TRIGGER) return;
        // 激活段提示：紧跟激活触发器列表下方
        if (page.activationKeyInfoY >= 0) {
            drawKeyString(gui, mainCenterX, (int) (page.activationKeyInfoY - page.scrollAmount),
                    KeyInit.SHOW_OVERLAY, "config.key_info");
        }
        // 滚动段提示：紧跟滚动行为列表下方
        if (page.scrollKeyInfoY >= 0) {
            drawKeyString(gui, mainCenterX, (int) (page.scrollKeyInfoY - page.scrollAmount),
                    KeyInit.SCROLL_MODIFIER, "config.scroll_key_info");
        }
    }

    private void renderPanelBackground(GuiGraphics gui, int x, int y, int w, int h) {
        // 高清圆角面板：256px 大纹理 + 线性过滤，把角块降采样到 GUI 尺寸，避免放大发糊
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

    private Component getDisplayModeName(DisplayMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.item_count_display_mode." + mode.name().toLowerCase());
    }

    private Component getAnimationSpeedName(AnimationSpeed speed) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.animation_speed." + speed.name().toLowerCase());
    }

    private Component getLongPressModeName(LongPressMode mode) {
        return Component.translatable("gui." + BetterLooting.MODID + ".config.long_press_mode." + mode.name().toLowerCase());
    }

}
