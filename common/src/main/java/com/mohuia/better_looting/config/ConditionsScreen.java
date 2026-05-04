package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.gui.CommonSlider;
import com.mohuia.better_looting.config.BetterLootingConfig.ActivationMode;
import com.mohuia.better_looting.config.BetterLootingConfig.ScrollMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 触发与滚动条件配置界面。
 * 采用侧边栏标签页布局，支持主内容区域鼠标滚轮滚动。
 */
public class ConditionsScreen extends Screen {

    private final Screen parent;
    private final ConfigViewModel viewModel;

    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 6;

    // --- 侧边栏与主区域布局变量 ---
    private int sidebarX, sidebarY, sidebarWidth, sidebarHeight;
    private int mainX, mainY, mainWidth, mainHeight;
    private int mainCenterX;

    // --- 滚动相关变量 ---
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> originalYMap = new HashMap<>();
    private double scrollAmount = 0;
    private int maxScroll = 0;

    // 渲染文本框标题所需的坐标
    private int customTitleLabelX;
    private int customTitleLabelY;
    private boolean showCustomTitleLabel = false;

    // 定义当前所处的标签页
    private enum Category {
        ACTIVATION("header_condition"),
        SCROLL("scroll_mode"),
        OTHER("other_settings");

        final String langKey;
        Category(String langKey) { this.langKey = langKey; }
        Component getDisplayName() { return Component.translatable("gui." + BetterLooting.MODID + "." + langKey); }
    }

    private Category currentCategory = Category.ACTIVATION;

    public ConditionsScreen(Screen parent, ConfigViewModel viewModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".conditions_title"));
        this.parent = parent;
        this.viewModel = viewModel;
    }

    private void calculateLayout() {
        this.sidebarWidth = Math.max(100, this.width / 4);
        this.sidebarX = 15;
        this.sidebarY = 40;
        this.sidebarHeight = this.height - 80;

        this.mainX = sidebarX + sidebarWidth + 10;
        this.mainY = 40;
        this.mainWidth = this.width - sidebarX - sidebarWidth - 40;
        // 稍微减小主区域高度，为底部的快捷键提示文本和返回按钮留出空间
        this.mainHeight = this.height - 100;
        this.mainCenterX = mainX + (mainWidth / 2);
    }

    @Override
    protected void init() {
        this.scrollableWidgets.clear();
        this.originalYMap.clear();

        calculateLayout();
        this.showCustomTitleLabel = false;

        int widgetWidth = Math.min(220, mainWidth - 20);

        // 1. 构建左侧侧边栏标签按钮
        buildSidebar();

        // 2. 根据当前选中的标签页构建右侧内容
        int startY = mainY + 15;

        switch (currentCategory) {
            case ACTIVATION -> buildActivationTab(startY, widgetWidth);
            case SCROLL -> buildScrollTab(startY, widgetWidth);
            case OTHER -> buildOtherTab(startY, widgetWidth);
        }

        // 3. 计算最大滚动距离
        int maxWidgetY = mainY;
        for (AbstractWidget w : scrollableWidgets) {
            maxWidgetY = Math.max(maxWidgetY, originalYMap.get(w) + w.getHeight());
        }
        // 如果自定义标题标签在最底部，也纳入计算
        if (showCustomTitleLabel) {
            maxWidgetY = Math.max(maxWidgetY, customTitleLabelY + 10);
        }
        this.maxScroll = Math.max(0, maxWidgetY - mainY - mainHeight + 20); // 20是底部的留白

        // 应用滚动偏移
        updateWidgetPositions();

        // 4. 返回按钮：全局居中置底 (不加入滚动列表)
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    /**
     * 将组件添加到滚动区域的专用方法。
     * 只添加到事件接收器(addWidget)，不添加到默认渲染器，以便我们手动使用 Scissor 裁剪渲染。
     */
    private <T extends AbstractWidget> T addScrollableWidget(T widget) {
        this.addWidget(widget); // 允许接收鼠标和键盘事件
        this.scrollableWidgets.add(widget);
        this.originalYMap.put(widget, widget.getY());
        return widget;
    }

    // 更新组件的实际 Y 坐标
    private void updateWidgetPositions() {
        int paddingY = 6; // 设置上下边距为 6 像素

        for (AbstractWidget widget : scrollableWidgets) {
            int newY = (int) (originalYMap.get(widget) - scrollAmount);
            widget.setY(newY);
            // 只要组件还在裁剪区(可见区)内，就保持渲染和交互状态，防止在边距内被误触
            widget.visible = (newY + widget.getHeight() > mainY + paddingY) && (newY < mainY + mainHeight - paddingY);
        }
    }

    // 监听鼠标滚轮事件
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 判断鼠标是否在主区域内
        if (mouseX >= mainX && mouseX <= mainX + mainWidth && mouseY >= mainY && mouseY <= mainY + mainHeight) {
            if (maxScroll > 0) {
                this.scrollAmount -= delta * 20.0; // 控制每次滚动的像素速度
                this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll));
                updateWidgetPositions();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void buildSidebar() {
        int currentY = sidebarY + 15;
        int btnWidth = sidebarWidth - 20;
        int btnX = sidebarX + 10;

        for (Category category : Category.values()) {
            boolean isSelected = (currentCategory == category);

            Button tabBtn = Button.builder(category.getDisplayName(), b -> {
                this.currentCategory = category;
                this.scrollAmount = 0; // 切换标签时重置滚动条
                this.clearWidgets();
                this.init();
            }).bounds(btnX, currentY, btnWidth, BTN_HEIGHT).build();

            tabBtn.active = !isSelected;
            this.addRenderableWidget(tabBtn);
            currentY += BTN_HEIGHT + BTN_GAP;
        }
    }

    private void buildActivationTab(int startY, int widgetWidth) {
        buildEnumList(mainCenterX, startY, widgetWidth, ActivationMode.values(), viewModel.activationMode,
                mode -> viewModel.activationMode = mode,
                this::getModeName, this::getModeTooltip);
    }

    private void buildScrollTab(int startY, int widgetWidth) {
        buildEnumList(mainCenterX, startY, widgetWidth, ScrollMode.values(), viewModel.scrollMode,
                mode -> viewModel.scrollMode = mode,
                this::getScrollModeName, this::getScrollModeTooltip);
    }

    private void buildOtherTab(int startY, int widgetWidth) {
        int currentY = startY;
        int x = mainCenterX - (widgetWidth / 2);

        // 1. 悬浮窗标题输入框 (移动到首位)
        this.showCustomTitleLabel = true;
        this.customTitleLabelX = x;
        this.customTitleLabelY = currentY;
        currentY += 12; // 为文字标签留出空间

        EditBox titleInputBox = new EditBox(this.font, x, currentY, widgetWidth, BTN_HEIGHT, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"));
        titleInputBox.setMaxLength(32);
        titleInputBox.setValue(viewModel.customOverlayTitle != null ? viewModel.customOverlayTitle : "");
        titleInputBox.setResponder(text -> viewModel.customOverlayTitle = text);
        titleInputBox.setTooltip(Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.custom_title")));
        this.addScrollableWidget(titleInputBox);
        currentY += BTN_HEIGHT + BTN_GAP + 6;

        // 2. 快捷栏指示器开关
        Component indicatorText = Component.translatable("gui." + BetterLooting.MODID + ".config.hotbar_indicator");
        this.addScrollableWidget(Button.builder(formatOptionText(indicatorText, viewModel.showHotbarIndicator), b -> {
            viewModel.showHotbarIndicator = !viewModel.showHotbarIndicator;
            this.clearWidgets();
            this.init();
        }).bounds(x, currentY, widgetWidth, BTN_HEIGHT).build());
        currentY += BTN_HEIGHT + BTN_GAP + 6;

        // 3. 超级合并开关
        Component mergeText = Component.translatable("gui." + BetterLooting.MODID + ".config.super_merge");
        this.addScrollableWidget(Button.builder(formatOptionText(mergeText, viewModel.enableSuperMerge), b -> {
            viewModel.enableSuperMerge = !viewModel.enableSuperMerge;
            this.clearWidgets();
            this.init();
        }).bounds(x, currentY, widgetWidth, BTN_HEIGHT).build());
        currentY += BTN_HEIGHT + BTN_GAP;

        if (viewModel.enableSuperMerge) {
            // 水平合并范围
            this.addScrollableWidget(new CommonSlider(
                    x, currentY, widgetWidth, BTN_HEIGHT,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_xz"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeXZ, 1,
                    val -> viewModel.mergeRangeXZ = (float) (Math.round(val * 10.0) / 10.0)
            ));
            currentY += BTN_HEIGHT + BTN_GAP;

            // 垂直合并范围
            this.addScrollableWidget(new CommonSlider(
                    x, currentY, widgetWidth, BTN_HEIGHT,
                    Component.translatable("gui." + BetterLooting.MODID + ".config.merge_range_y"),
                    "m", 0.0, 10.0, (double) viewModel.mergeRangeY, 1,
                    val -> viewModel.mergeRangeY = (float) (Math.round(val * 10.0) / 10.0)
            ));
            currentY += BTN_HEIGHT + BTN_GAP;
        }

        // 拾取保护延迟
        this.addScrollableWidget(new CommonSlider(
                x, currentY, widgetWidth, BTN_HEIGHT,
                Component.translatable("gui." + BetterLooting.MODID + ".config.pickup_delay"),
                "s", 0.0, 5.0, (double) viewModel.pickupDelaySeconds, 1,
                val -> viewModel.pickupDelaySeconds = (float) (Math.round(val * 10.0) / 10.0)
        ));
        currentY += BTN_HEIGHT + BTN_GAP;

        // 长按触发时间
        this.addScrollableWidget(new CommonSlider(
                x, currentY, widgetWidth, BTN_HEIGHT,
                Component.translatable("gui." + BetterLooting.MODID + ".config.max_hold_seconds"),
                "s", 0.5, 5.0, (double) viewModel.maxHoldTicks / 20.0, 1,
                val -> {
                    float seconds = (float) (Math.round(val * 10.0) / 10.0);
                    viewModel.maxHoldTicks = (int) (seconds * 20);
                }
        ));
    }

    private <T extends Enum<T>> void buildEnumList(int centerX, int startY, int widgetWidth, T[] values, T current,
                                                   Consumer<T> setter,
                                                   Function<T, Component> nameProvider,
                                                   Function<T, Tooltip> tooltipProvider) {
        int currentY = startY;
        int x = centerX - (widgetWidth / 2);

        for (T mode : values) {
            boolean isSelected = (mode == current);

            Button btn = Button.builder(formatOptionText(nameProvider.apply(mode), isSelected), b -> {
                        setter.accept(mode);
                        this.clearWidgets();
                        this.init();
                    })
                    .bounds(x, currentY, widgetWidth, BTN_HEIGHT)
                    .tooltip(tooltipProvider.apply(mode))
                    .build();
            // 修改点：使用 addScrollableWidget
            this.addScrollableWidget(btn);
            currentY += BTN_HEIGHT + BTN_GAP;

            if (mode == ActivationMode.LOOK_DOWN && isSelected) {
                this.addScrollableWidget(new CommonSlider(
                        x + 10, currentY, widgetWidth - 10, BTN_HEIGHT,
                        Component.translatable("gui." + BetterLooting.MODID + ".angle"),
                        0.0, 90.0, (double) viewModel.lookDownAngle,
                        val -> viewModel.lookDownAngle = val.floatValue()
                ));
                currentY += BTN_HEIGHT + BTN_GAP;
            }
        }
    }

    private Component formatOptionText(Component text, boolean selected) {
        return selected
                ? Component.literal("[✔] ").withStyle(ChatFormatting.GREEN).append(text.copy().withStyle(ChatFormatting.GREEN))
                : text.copy().withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);

        gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        renderPanelBackground(gui, sidebarX, sidebarY, sidebarWidth, sidebarHeight);
        renderPanelBackground(gui, mainX, mainY, mainWidth, mainHeight);

        gui.drawCenteredString(this.font, currentCategory.getDisplayName().copy().withStyle(ChatFormatting.GOLD), mainCenterX, mainY - 12, 0xFFFFFFFF);

        // ==== 开启 Scissor 裁剪测试 ====
        int paddingY = 6; // 同样保持 6 像素的上下边距
        // 稍微缩小裁剪区域，使组件在碰到真正的边框前就被隐藏
        gui.enableScissor(mainX, mainY + paddingY, mainX + mainWidth, mainY + mainHeight - paddingY);

        if (showCustomTitleLabel) {
            int labelY = (int) (customTitleLabelY - scrollAmount);
            // 同样为自定义标题的文本渲染增加边距判断
            if (labelY > mainY + paddingY - 10 && labelY < mainY + mainHeight - paddingY) {
                gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"), customTitleLabelX + 2, labelY, 0xDDDDDD);
            }
        }

        // 手动渲染滚动组件
        for (AbstractWidget widget : scrollableWidgets) {
            widget.render(gui, mouseX, mouseY, partialTick);
        }

        gui.disableScissor();
        // ==== 裁剪结束 ====

        // 渲染滚动条
        if (maxScroll > 0) {
            renderScrollBar(gui);
        }

        renderContextKeyInfo(gui);

        // 调用 super 会渲染侧边栏和底部的"完成"按钮
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderScrollBar(GuiGraphics gui) {
        int scrollbarX = mainX + mainWidth - 6;
        int scrollbarY = mainY + 2;
        int scrollbarHeight = mainHeight - 4;

        int handleHeight = Math.max(10, (int) ((float) scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll)));
        int handleY = scrollbarY + (int) ((scrollAmount / maxScroll) * (scrollbarHeight - handleHeight));

        gui.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x80000000); // 滚动条背景槽
        gui.fill(scrollbarX, handleY, scrollbarX + 4, handleY + handleHeight, 0xFF888888); // 滚动条滑块
    }

    private void renderContextKeyInfo(GuiGraphics gui) {
        // 修改点：将键位提示固定在主框的下方，防止滚动时与之重叠
        int infoY = mainY + mainHeight + 10;

        if (currentCategory == Category.ACTIVATION) {
            if (viewModel.activationMode == ActivationMode.KEY_HOLD || viewModel.activationMode == ActivationMode.KEY_TOGGLE) {
                drawKeyString(gui, mainCenterX, infoY, KeyInit.SHOW_OVERLAY, "config.key_info");
            }
        } else if (currentCategory == Category.SCROLL) {
            if (viewModel.scrollMode == ScrollMode.KEY_BIND || viewModel.scrollMode == ScrollMode.INVERT_KEY) {
                drawKeyString(gui, mainCenterX, infoY, KeyInit.SCROLL_MODIFIER, "config.scroll_key_info");
            }
        }
    }

    private void renderPanelBackground(GuiGraphics gui, int x, int y, int w, int h) {
        gui.fill(x, y, x + w, y + h, 0x60000000);
        gui.fill(x, y, x + w, y + 1, 0x80FFFFFF);
        gui.fill(x, y + h - 1, x + w, y + h, 0x80FFFFFF);
    }

    private void drawKeyString(GuiGraphics gui, int x, int y, net.minecraft.client.KeyMapping key, String langKey) {
        Component keyName = key.getTranslatedKeyMessage();
        int color = key.isUnbound() ? 0xFFFF5555 : 0xFF55FF55;
        gui.drawCenteredString(this.font, Component.translatable("gui." + BetterLooting.MODID + "." + langKey, keyName), x, y, color);
    }

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
}