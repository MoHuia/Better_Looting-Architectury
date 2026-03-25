package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.gui.CommonSlider;
import com.mohuia.better_looting.config.BetterLootingConfig.ActivationMode;
import com.mohuia.better_looting.config.BetterLootingConfig.ScrollMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 触发与滚动条件配置界面。
 * 允许玩家自定义面板显示和列表滚动的限制条件，并控制其他杂项设置。
 */
public class ConditionsScreen extends Screen {

    private final Screen parent;
    private final ConfigViewModel viewModel;

    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 5;
    private static final int PADDING = 6;

    // --- 动态布局变量 ---
    private int dynamicColWidth;
    private int leftColX, centerColX, rightColX;

    public ConditionsScreen(Screen parent, ConfigViewModel viewModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".conditions_title"));
        this.parent = parent;
        this.viewModel = viewModel;
    }

    private void calculateLayout() {
        int maxBgWidth = 140;
        int gap = 12;

        int bgWidth = Math.min(maxBgWidth, (this.width - (gap * 2) - 20) / 3);

        this.dynamicColWidth = bgWidth - (PADDING * 2);

        this.centerColX = this.width / 2;
        this.leftColX = this.centerColX - bgWidth - gap;
        this.rightColX = this.centerColX + bgWidth + gap;
    }

    @Override
    protected void init() {
        calculateLayout();

        int listStartY = 65;

        // 1. 构建“激活条件”列 (左列)
        buildEnumColumn(leftColX, listStartY, dynamicColWidth,
                ActivationMode.values(), viewModel.activationMode,
                (mode) -> viewModel.activationMode = mode,
                this::getModeName, this::getModeTooltip);

        // 2. 构建“滚动条件”列 (中列)
        buildEnumColumn(centerColX, listStartY, dynamicColWidth,
                ScrollMode.values(), viewModel.scrollMode,
                (mode) -> viewModel.scrollMode = mode,
                this::getScrollModeName, this::getScrollModeTooltip);

        // 3. 构建“其他设置”列 (右列)
        int rightBtnX = rightColX - (dynamicColWidth / 2);
        Component indicatorText = Component.translatable("gui." + BetterLooting.MODID + ".config.hotbar_indicator");

        Button indicatorBtn = Button.builder(formatOptionText(indicatorText, viewModel.showHotbarIndicator), b -> {
            viewModel.showHotbarIndicator = !viewModel.showHotbarIndicator;
            this.clearWidgets();
            this.init();
        }).bounds(rightBtnX, listStartY, dynamicColWidth, BTN_HEIGHT).build();
        this.addRenderableWidget(indicatorBtn);

        // 文本框
        int titleLabelY = listStartY + BTN_HEIGHT + BTN_GAP * 2;
        int titleBoxY = titleLabelY + 12;

        EditBox titleInputBox = new EditBox(this.font, rightBtnX, titleBoxY, dynamicColWidth, BTN_HEIGHT, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"));
        titleInputBox.setMaxLength(32);
        titleInputBox.setValue(viewModel.customOverlayTitle != null ? viewModel.customOverlayTitle : "");
        titleInputBox.setResponder(text -> viewModel.customOverlayTitle = text);
        titleInputBox.setTooltip(Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.custom_title")));
        this.addRenderableWidget(titleInputBox);

        // 4. 返回按钮：居中置底
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private <T extends Enum<T>> void buildEnumColumn(int centerX, int startY, int colWidth, T[] values, T current,
                                                     Consumer<T> setter,
                                                     Function<T, Component> nameProvider,
                                                     Function<T, Tooltip> tooltipProvider) {
        int currentY = startY;
        int x = centerX - (colWidth / 2);

        for (T mode : values) {
            boolean isSelected = (mode == current);

            Button btn = Button.builder(formatOptionText(nameProvider.apply(mode), isSelected), b -> {
                        setter.accept(mode);
                        this.clearWidgets();
                        this.init();
                    })
                    .bounds(x, currentY, colWidth, BTN_HEIGHT)
                    .tooltip(tooltipProvider.apply(mode))
                    .build();
            this.addRenderableWidget(btn);
            currentY += BTN_HEIGHT + BTN_GAP;

            if (mode == ActivationMode.LOOK_DOWN && isSelected) {
                this.addRenderableWidget(new CommonSlider(
                        x + 2, currentY, colWidth - 4, BTN_HEIGHT,
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

    // 分离背景渲染，防止层级错乱遮挡按键
    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 先让原版渲染它的虚化和暗色底层
        super.renderBackground(gui, mouseX, mouseY, partialTick);

        // 2. 绘制三列的半透明底板，确保它们在按键的【底层】
        int topY = 50;
        int bottomY = this.height - 40;

        renderColumnBackground(gui, leftColX, topY, bottomY, dynamicColWidth, Component.translatable("gui." + BetterLooting.MODID + ".header_condition"));
        renderColumnBackground(gui, centerColX, topY, bottomY, dynamicColWidth, Component.translatable("gui." + BetterLooting.MODID + ".scroll_mode"));
        renderColumnBackground(gui, rightColX, topY, bottomY, dynamicColWidth, Component.translatable("gui." + BetterLooting.MODID + ".other_settings"));
    }

    // 组件和文字的顶层渲染
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. super.render 会自动调用上面的 renderBackground，然后再画出所有的 Button 和 EditBox
        super.render(gui, mouseX, mouseY, partialTick);

        // 2. 绘制需要在按键【顶层】的内容（文字提示等）
        gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);

        int bottomY = this.height - 40;
        renderKeyInfo(gui, leftColX, bottomY, viewModel.activationMode);
        renderKeyInfo(gui, centerColX, bottomY, viewModel.scrollMode);

        // 绘制文本框上方的说明文字标签
        int rightBtnX = rightColX - (dynamicColWidth / 2);
        int titleLabelY = 65 + BTN_HEIGHT + BTN_GAP * 2;
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.custom_title_label"), rightBtnX + 2, titleLabelY, 0xDDDDDD);
    }

    private void renderKeyInfo(GuiGraphics gui, int centerX, int bottomY, Enum<?> mode) {
        if (mode instanceof ActivationMode m && (m == ActivationMode.KEY_HOLD || m == ActivationMode.KEY_TOGGLE)) {
            drawKeyString(gui, centerX, bottomY, KeyInit.SHOW_OVERLAY, "config.key_info");
        } else if (mode instanceof ScrollMode m && m == ScrollMode.KEY_BIND) {
            drawKeyString(gui, centerX, bottomY, KeyInit.SCROLL_MODIFIER, "config.scroll_key_info");
        }
    }

    private void drawKeyString(GuiGraphics gui, int x, int y, net.minecraft.client.KeyMapping key, String langKey) {
        Component keyName = key.getTranslatedKeyMessage();
        int color = key.isUnbound() ? 0xFFFF5555 : 0xFF55FF55;
        gui.drawCenteredString(this.font, Component.translatable("gui." + BetterLooting.MODID + "." + langKey, keyName), x, y - 12, color);
    }

    private void renderColumnBackground(GuiGraphics gui, int centerX, int topY, int bottomY, int colWidth, Component header) {
        int halfW = (colWidth / 2) + PADDING;
        gui.fill(centerX - halfW, topY, centerX + halfW, bottomY, 0x60000000);
        gui.fill(centerX - halfW, topY, centerX + halfW, topY + 1, 0x80FFFFFF);
        gui.fill(centerX - halfW, bottomY - 1, centerX + halfW, bottomY, 0x80FFFFFF);
        gui.drawCenteredString(this.font, header, centerX, topY - 12, 0xFFFFAA00);
    }

    // --- 国际化文本获取工具方法 ---
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