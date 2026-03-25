package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.gui.CommonSlider;
import com.mohuia.better_looting.config.BetterLootingConfig.ActivationMode;
import com.mohuia.better_looting.config.BetterLootingConfig.ScrollMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 触发与滚动条件配置界面。
 * 允许玩家自定义面板显示和列表滚动的限制条件（如：一直开启、低头时开启、按键开启等）。
 */
public class ConditionsScreen extends Screen {

    private final Screen parent;
    private final ConfigViewModel viewModel;

    private static final int COLUMN_WIDTH = 160;
    private static final int BTN_HEIGHT = 20;
    private static final int BTN_GAP = 5;
    private static final int PADDING = 15;

    public ConditionsScreen(Screen parent, ConfigViewModel viewModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".conditions_title"));
        this.parent = parent;
        this.viewModel = viewModel;
    }

    @Override
    protected void init() {
        int quarterWidth = this.width / 4;
        int threeQuarterWidth = (this.width / 4) * 3;
        int listStartY = 60;

        // 构建“激活条件”列
        buildEnumColumn(quarterWidth, listStartY,
                ActivationMode.values(), viewModel.activationMode,
                (mode) -> viewModel.activationMode = mode,
                this::getModeName, this::getModeTooltip);

        // 构建“滚动条件”列
        buildEnumColumn(threeQuarterWidth, listStartY,
                ScrollMode.values(), viewModel.scrollMode,
                (mode) -> viewModel.scrollMode = mode,
                this::getScrollModeName, this::getScrollModeTooltip);

        // 返回按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    /**
     * 动态生成枚举选项列。
     * 遍历传入的枚举数组，为每个枚举值生成一个切换按钮。如果是特定模式（如低头模式），还会额外生成相关参数调整的滑块。
     */
    private <T extends Enum<T>> void buildEnumColumn(int centerX, int startY, T[] values, T current,
                                                     Consumer<T> setter,
                                                     Function<T, Component> nameProvider,
                                                     Function<T, Tooltip> tooltipProvider) {
        int currentY = startY;
        int x = centerX - (COLUMN_WIDTH / 2);

        for (T mode : values) {
            boolean isSelected = (mode == current);

            Button btn = Button.builder(formatOptionText(nameProvider.apply(mode), isSelected), b -> {
                        setter.accept(mode);
                        this.clearWidgets();
                        this.init(); // 刷新界面以更新按钮选中状态和可能出现的子选项
                    })
                    .bounds(x, currentY, COLUMN_WIDTH, BTN_HEIGHT)
                    .tooltip(tooltipProvider.apply(mode))
                    .build();
            this.addRenderableWidget(btn);
            currentY += BTN_HEIGHT + BTN_GAP;

            // 特殊处理：如果选中了低头模式，展示角度调节滑块
            if (mode == ActivationMode.LOOK_DOWN && isSelected) {
                this.addRenderableWidget(new CommonSlider(
                        x + 10, currentY, COLUMN_WIDTH - 10, BTN_HEIGHT,
                        Component.translatable("gui." + BetterLooting.MODID + ".angle"),
                        0.0, 90.0, (double) viewModel.lookDownAngle,
                        val -> viewModel.lookDownAngle = val.floatValue()
                ));
                currentY += BTN_HEIGHT + BTN_GAP;
            }
        }
    }

    /**
     * 为选中的选项添加绿色勾选标记，未选中的置灰。
     */
    private Component formatOptionText(Component text, boolean selected) {
        return selected
                ? Component.literal("[✔] ").withStyle(ChatFormatting.GREEN).append(text.copy().withStyle(ChatFormatting.GREEN))
                : text.copy().withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 先让原版渲染它的虚化和暗色底层
        super.renderBackground(gui, mouseX, mouseY, partialTick);

        // 2. 在这里绘制所有需要在按键【底层】的自定义内容（不然会遮挡按键）
        int quarterWidth = this.width / 4;
        int threeQuarterWidth = (this.width / 4) * 3;
        int topY = 50;
        int bottomY = this.height - 40;

        renderColumnBackground(gui, quarterWidth, topY, bottomY, Component.translatable("gui." + BetterLooting.MODID + ".header_condition"));
        renderColumnBackground(gui, threeQuarterWidth, topY, bottomY, Component.translatable("gui." + BetterLooting.MODID + ".scroll_mode"));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. super.render 会自动调用上面的 renderBackground，然后再一层层画出按键！
        super.render(gui, mouseX, mouseY, partialTick);

        // 2. 绘制需要在按键【顶层】的内容（比如文字提示）
        gui.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF); // 记得用之前提到的 0xFFFFFFFF 哦

        int quarterWidth = this.width / 4;
        int threeQuarterWidth = (this.width / 4) * 3;
        int bottomY = this.height - 40;

        renderKeyInfo(gui, quarterWidth, bottomY, viewModel.activationMode);
        renderKeyInfo(gui, threeQuarterWidth, bottomY, viewModel.scrollMode);
    }

    /**
     * 根据当前选择的模式，在列表底部显示对应的按键绑定信息。
     */
    private void renderKeyInfo(GuiGraphics gui, int centerX, int bottomY, Enum<?> mode) {
        if (mode instanceof ActivationMode m && (m == ActivationMode.KEY_HOLD || m == ActivationMode.KEY_TOGGLE)) {
            drawKeyString(gui, centerX, bottomY, KeyInit.SHOW_OVERLAY, "config.key_info");
        } else if (mode instanceof ScrollMode m && m == ScrollMode.KEY_BIND) {
            drawKeyString(gui, centerX, bottomY, KeyInit.SCROLL_MODIFIER, "config.scroll_key_info");
        }
    }

    /**
     * 绘制按键绑定状态文字。若按键未绑定（Unbound），文字将标红警示。
     */
    private void drawKeyString(GuiGraphics gui, int x, int y, net.minecraft.client.KeyMapping key, String langKey) {
        Component keyName = key.getTranslatedKeyMessage();
        int color = key.isUnbound() ? 0xFFFF5555 : 0xFF55FF55;
        gui.drawCenteredString(this.font, Component.translatable("gui." + BetterLooting.MODID + "." + langKey, keyName), x, y - 15, color);
    }

    private void renderColumnBackground(GuiGraphics gui, int centerX, int topY, int bottomY, Component header) {
        int halfW = (COLUMN_WIDTH / 2) + PADDING;
        gui.fill(centerX - halfW, topY, centerX + halfW, bottomY, 0x60000000); // 黑色半透明底
        gui.fill(centerX - halfW, topY, centerX + halfW, topY + 1, 0x80FFFFFF); // 顶部白线
        gui.fill(centerX - halfW, bottomY - 1, centerX + halfW, bottomY, 0x80FFFFFF); // 底部白线
        gui.drawCenteredString(this.font, header, centerX, topY - 12, 0xFFFFAA00); // 橙色标题
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