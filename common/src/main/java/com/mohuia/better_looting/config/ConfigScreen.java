package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.core.VisualItemEntry;
import com.mohuia.better_looting.client.gui.CommonSlider;
import com.mohuia.better_looting.client.overlay.HotbarIndicator;
import com.mohuia.better_looting.client.overlay.OverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 主配置界面（所见即所得）。
 * 允许玩家通过拖拽、缩放实时预览拾取面板的效果，并提供透明度调节和跳转到条件设置的入口。
 * 包含对快捷栏指示器的自由拖拽与右键旋转功能。
 */
public class ConfigScreen extends Screen {

    private final ConfigViewModel viewModel;
    private final DragController dragController;
    private OverlayRenderer renderer;
    private final List<VisualItemEntry> previewItems = new ArrayList<>();
    public String customOverlayTitle;

    // 预览框的边界坐标，用于处理鼠标拖拽和裁剪
    private float boxLeft, boxTop, boxRight, boxBottom;

    // 悬浮窗指示器拖拽状态
    private boolean isDraggingIndicator = false;
    private double indicatorDragOffsetX = 0;
    private double indicatorDragOffsetY = 0;

    // 统一的美化主题色
    private static final int COLOR_ACCENT = 0xFF00BFFF; // 悬停/选中的强调色 (深空蓝)
    private static final int COLOR_PANEL_BG = 0x90101010; // 面板背景 (深灰半透明)
    private static final int COLOR_PANEL_BORDER = 0x50FFFFFF; // 面板边框
    private static final int COLOR_TEXT_MUTED = 0xFFAAAAAA; // 次要文本颜色

    public ConfigScreen() {
        this(new ConfigViewModel());
    }

    public ConfigScreen(ConfigViewModel existingModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".config.title"));
        this.viewModel = existingModel;
        this.dragController = new DragController();

        // 填充虚拟的预览物品数据，供渲染使用
        previewItems.add(new VisualItemEntry(new ItemStack(Items.DIAMOND, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.GOLDEN_APPLE, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.IRON_SWORD, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.EMERALD, 64)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.BOOK, 1)));
    }

    @Override
    protected void init() {
        if (this.renderer == null) this.renderer = new OverlayRenderer(this.minecraft);

        // 初始化指示器默认坐标
        if (viewModel.indicatorX < 0 || viewModel.indicatorY < 0) {
            viewModel.indicatorX = this.width / 2f + 91f + 6f;
            viewModel.indicatorY = this.height - 22f + 4f;
        }

        // ==========================================
        // 右上角紧凑控制区
        // ==========================================
        int panelWidth = 160;
        int startX = this.width - panelWidth - 10;

        this.addRenderableWidget(new ModernButton(startX + 5, 15, 60, 20, Component.translatable("gui." + BetterLooting.MODID + ".config.reset"), () -> {
            viewModel.resetToDefault();
            this.clearWidgets();
            this.init();
        }));

        this.addRenderableWidget(new ModernButton(startX + 70, 15, 60, 20, Component.translatable("gui." + BetterLooting.MODID + ".config.save"), () -> {
            viewModel.saveToConfig();
            this.onClose();
        }));

        this.addRenderableWidget(new ModernButton(startX + 135, 15, 20, 20, Component.empty(), () -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ConditionsScreen(this, this.viewModel));
            }
        }, Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.settings"))) {
            @Override
            protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
                // 1. 先调用父类方法，画出现代风的半透明底色和悬停边框
                super.renderWidget(gui, mouseX, mouseY, partialTick);

                // 2. 获取当前悬停状态来决定图标颜色
                boolean hovered = this.isHoveredOrFocused();
                int iconColor = hovered ? 0xFFFFFFFF : COLOR_TEXT_MUTED; // 悬停亮白，平时暗灰

                // 计算按钮的中心点
                int cx = this.getX() + this.width / 2;
                int cy = this.getY() + this.height / 2;

                // 3. 绘制极简风格的“调节推子/均衡器”图标 (纯几何方块，边缘绝对锐利)
                // 上线条 + 左侧滑块
                gui.fill(cx - 5, cy - 4, cx + 5, cy - 3, iconColor); // 底线
                gui.fill(cx - 4, cy - 5, cx - 2, cy - 2, iconColor); // 节点

                // 中线条 + 右侧滑块
                gui.fill(cx - 5, cy, cx + 5, cy + 1, iconColor);
                gui.fill(cx + 2, cy - 1, cx + 4, cy + 2, iconColor);

                // 下线条 + 中间偏左滑块
                gui.fill(cx - 5, cy + 4, cx + 5, cy + 5, iconColor);
                gui.fill(cx - 1, cy + 3, cx + 1, cy + 6, iconColor);
            }
        });

        // 透明度滑块
        this.addRenderableWidget(new CommonSlider(
                startX + 5, 40, 150, 20,
                Component.translatable("gui." + BetterLooting.MODID + ".config.opacity"),
                0.1, 1.0, (double) viewModel.globalAlpha,
                val -> viewModel.globalAlpha = val.floatValue()
        ));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        renderGrid(gui);
        renderControlPanelBackground(gui); // 新增：为右上角按钮绘制一个底层面板
        renderInfoOverlay(gui);
        renderPreview(gui, mouseX, mouseY);
        renderIndicator(gui, mouseX, mouseY);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    /**
     * 为右上角的按钮区域绘制一个半透明背景面板，使其看起来像一个整体的控制台。
     */
    private void renderControlPanelBackground(GuiGraphics gui) {
        int panelWidth = 160;
        int startX = this.width - panelWidth - 10;
        gui.fill(startX, 10, startX + panelWidth + 10, 65, COLOR_PANEL_BG);
        gui.renderOutline(startX, 10, panelWidth + 10, 55, COLOR_PANEL_BORDER);
    }

    /**
     * 渲染快捷栏指示器及优雅的交互热区边框。
     */
    private void renderIndicator(GuiGraphics gui, int mouseX, int mouseY) {
        if (!viewModel.showHotbarIndicator) return;

        gui.pose().pushPose();
        float centerX = viewModel.indicatorX + HotbarIndicator.WIDTH / 2f;
        float centerY = viewModel.indicatorY + HotbarIndicator.HEIGHT / 2f;
        gui.pose().translate(centerX, centerY, 0);
        gui.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(viewModel.indicatorRotation));
        gui.pose().translate(-centerX, -centerY, 0);

        // 使用更柔和的提示框代替原本的绿色硬边框
        boolean isHovered = isMouseOverIndicator(mouseX, mouseY);
        if (isHovered || isDraggingIndicator) {
            int outlineColor = isDraggingIndicator ? COLOR_ACCENT : 0x8000BFFF;
            gui.fill((int)viewModel.indicatorX - 3, (int)viewModel.indicatorY - 3, (int)viewModel.indicatorX + HotbarIndicator.WIDTH + 3, (int)viewModel.indicatorY + HotbarIndicator.HEIGHT + 3, 0x3000BFFF);
            gui.renderOutline((int)viewModel.indicatorX - 3, (int)viewModel.indicatorY - 3, HotbarIndicator.WIDTH + 6, HotbarIndicator.HEIGHT + 6, outlineColor);
        }

        gui.pose().popPose();
        HotbarIndicator.INSTANCE.renderInternal(gui, viewModel.indicatorX, viewModel.indicatorY, viewModel.indicatorRotation, Core.INSTANCE.getFilterMode());
    }

    /**
     * 重新设计的左上角信息面板：增加内边距、边框、并优化字体颜色层级。
     */
    private void renderInfoOverlay(GuiGraphics gui) {
        int startX = 10;
        int startY = 10;
        int width = 165;
        int height = 100;

        // 绘制面板背景与边框
        gui.fill(startX, startY, startX + width, startY + height, COLOR_PANEL_BG);
        gui.renderOutline(startX, startY, width, height, COLOR_PANEL_BORDER);

        int textX = startX + 8;
        int textY = startY + 8;

        // 标题
        gui.drawString(this.font, Component.literal("📊 面板状态 / Status"), textX, textY, 0xFFFFFFFF, true);
        gui.fill(textX, textY + 11, startX + width - 8, textY + 12, 0x40FFFFFF); // 分割线

        textY += 16;
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.pos", (int)viewModel.xOffset, (int)viewModel.yOffset), textX, textY, COLOR_TEXT_MUTED, false);

        textY += 14;
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.size", viewModel.panelWidth, String.format("%.1f", viewModel.visibleRows)), textX, textY, COLOR_TEXT_MUTED, false);

        textY += 14;
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.scale", String.format("%.2f", viewModel.uiScale)), textX, textY, COLOR_TEXT_MUTED, false);

        textY += 14;
        Component modeName = Component.translatable("gui." + BetterLooting.MODID + ".config.mode." + viewModel.activationMode.name().toLowerCase());
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.header_condition").copy().append(": ").append(modeName), textX, textY, 0xFFE0E0E0, false);

        // 底部操作提示 (用高亮色区分)
        textY += 16;
        gui.drawString(this.font, Component.literal("💡 左键拖拽 | 右键旋转指示器"), textX, textY, 0xFFFFDD66, false);
    }

    /**
     * 渲染模拟的掉落物面板。
     */
    private void renderPreview(GuiGraphics gui, int mouseX, int mouseY) {
        var bounds = viewModel.calculatePreviewBounds(this.width, this.height);
        this.boxLeft = bounds.left();
        this.boxTop = bounds.top();
        this.boxRight = bounds.right();
        this.boxBottom = bounds.bottom();
        dragController.updateBounds(boxLeft, boxTop, boxRight, boxBottom);

        float baseX = (this.width / 2.0f + viewModel.xOffset);
        float baseY = (this.height / 2.0f + viewModel.yOffset);
        float scale = viewModel.uiScale;

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(baseX, baseY, 0);
        pose.scale(scale, scale, 1.0f);

        int renderAlpha = Math.max((int) (viewModel.globalAlpha * 255), 20);
        int headerColor = (0x00FFAA00 & 0x00FFFFFF) | (renderAlpha << 24);
        int lineColor = (0x00AAAAAA & 0x00FFFFFF) | ((int)(renderAlpha * 0.5f) << 24);

        float localMinY = -(Constants.ITEM_HEIGHT / 2.0f) - 14;
        pose.pushPose();
        pose.translate(Constants.LIST_X, localMinY, 0);
        pose.scale(0.75f, 0.75f, 1.0f);
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.preview_title"), 0, 0, headerColor, true);
        pose.popPose();

        gui.fill(Constants.LIST_X, (int)localMinY + 10, Constants.LIST_X + viewModel.panelWidth, (int)localMinY + 11, lineColor);

        gui.enableScissor((int)boxLeft, (int)boxTop, (int)boxRight, (int)boxBottom);

        int startY = -(Constants.ITEM_HEIGHT / 2);
        for (int i = 0; i < previewItems.size(); i++) {
            int y = startY + (i * (Constants.ITEM_HEIGHT + 2));
            renderer.renderItemRow(gui, Constants.LIST_X, y, viewModel.panelWidth,
                    previewItems.get(i), i == 0, viewModel.globalAlpha, 1.0f, i == 1);
        }
        gui.disableScissor();
        pose.popPose();

        drawControlHandles(gui, mouseX, mouseY);
    }

    /**
     * 重新设计的交互控制边框。
     * 摒弃了原版大块的红蓝绿颜色，改用细致的线条、半透明遮罩和“L”型边角手柄，提升现代感。
     */
    private void drawControlHandles(GuiGraphics gui, int mouseX, int mouseY) {
        DragController.DragMode mode = dragController.getCurrentDragMode();

        // 绘制主边框 (当正在拖动整个面板时加粗并高亮)
        int borderCol = (mode == DragController.DragMode.MOVE || dragController.isOverBody(mouseX, mouseY)) ? 0xA0FFFFFF : 0x50FFFFFF;
        gui.renderOutline((int)boxLeft, (int)boxTop, (int)(boxRight-boxLeft), (int)(boxBottom-boxTop), borderCol);

        // 如果处于调整大小的悬停状态，绘制半透明背景作为反馈
        if (mode != DragController.DragMode.NONE && mode != DragController.DragMode.MOVE) {
            gui.fill((int)boxLeft, (int)boxTop, (int)boxRight, (int)boxBottom, 0x15FFFFFF);
        }

        int l = (int)boxLeft, t = (int)boxTop, r = (int)boxRight, b = (int)boxBottom;

        // 右侧宽度调整手柄 (悬停时变为强调色，绘制一个小把手而不是整条线)
        int cR = dragController.isOverRight(mouseX, mouseY) ? COLOR_ACCENT : 0xCCFFFFFF;
        gui.fill(r - 1, t + (b - t)/2 - 10, r + 2, t + (b - t)/2 + 10, cR);

        // 底部高度调整手柄
        int cB = dragController.isOverBottom(mouseX, mouseY) ? COLOR_ACCENT : 0xCCFFFFFF;
        gui.fill(l + (r - l)/2 - 10, b - 1, l + (r - l)/2 + 10, b + 2, cB);

        // 右下角缩放调整手柄 (绘制一个精致的 L 型括弧)
        int cC = dragController.isOverCorner(mouseX, mouseY) ? COLOR_ACCENT : 0xFFFFFFFF;
        gui.fill(r - 6, b - 1, r + 2, b + 2, cC); // 底部横线
        gui.fill(r - 1, b - 6, r + 2, b + 2, cC); // 右侧竖线
    }

    /**
     * 柔化背景参考网格。
     * 将刺眼的红色和高亮的白色替换为极其微弱的透明白/淡蓝色，让主体内容更突出。
     */
    private void renderGrid(GuiGraphics gui) {
        int softGridColor = 0x0AFFFFFF; // 极低透明度的白色
        for (int x = 0; x < this.width; x += 20) gui.fill(x, 0, x + 1, this.height, softGridColor);
        for (int y = 0; y < this.height; y += 20) gui.fill(0, y, this.width, y + 1, softGridColor);

        // 中心十字线改用柔和的淡蓝色并加点刻度感
        int centerColor = 0x2000BFFF;
        gui.fill(this.width / 2, 0, this.width / 2 + 1, this.height, centerColor);
        gui.fill(0, this.height / 2, this.width, this.height / 2 + 1, centerColor);
    }

    /**
     * 极简现代风按钮，完美适配半透明 HUD 主题。
     */
    public static class ModernButton extends net.minecraft.client.gui.components.AbstractButton {
        private final Runnable onPress;

        public ModernButton(int x, int y, int width, int height, Component message, Runnable onPress) {
            this(x, y, width, height, message, onPress, null);
        }

        public ModernButton(int x, int y, int width, int height, Component message, Runnable onPress, Tooltip tooltip) {
            super(x, y, width, height, message);
            this.onPress = onPress;
            if (tooltip != null) {
                this.setTooltip(tooltip);
            }
        }

        @Override
        public void onPress() {
            this.onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();

            // 悬停时：背景变为带有主题色的半透明，边框高亮深空蓝
            // 正常时：背景为深灰色半透明，边框为微弱的白边
            int bgColor = hovered ? 0x6000BFFF : 0x40000000;
            int borderColor = hovered ? COLOR_ACCENT : COLOR_PANEL_BORDER; // 复用我们之前定义的主题色常量
            int textColor = hovered ? 0xFFFFFFFF : COLOR_TEXT_MUTED;

            // 绘制扁平背景
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            // 绘制精致的 1 像素边框
            gui.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);

            // 居中绘制文本 (关闭阴影，使其更现代化、更清爽)
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int textWidth = font.width(this.getMessage());
            int textX = this.getX() + (this.width - textWidth) / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            gui.drawString(font, this.getMessage(), textX, textY, textColor, false);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    // --- 鼠标交互及逻辑检测 (保持原样) ---

    private boolean isMouseOverIndicator(double mouseX, double mouseY) {
        if (!viewModel.showHotbarIndicator) return false;
        float maxDim = Math.max(HotbarIndicator.WIDTH, HotbarIndicator.HEIGHT);
        float centerX = viewModel.indicatorX + HotbarIndicator.WIDTH / 2f;
        float centerY = viewModel.indicatorY + HotbarIndicator.HEIGHT / 2f;
        return mouseX >= centerX - maxDim && mouseX <= centerX + maxDim
                && mouseY >= centerY - maxDim && mouseY <= centerY + maxDim;
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        if (isMouseOverIndicator(x, y)) {
            if (btn == 0) {
                isDraggingIndicator = true;
                indicatorDragOffsetX = x - viewModel.indicatorX;
                indicatorDragOffsetY = y - viewModel.indicatorY;
                return true;
            } else if (btn == 1) { // 右键旋转
                viewModel.indicatorRotation = (viewModel.indicatorRotation + 90) % 360;
                return true;
            }
        }
        return (btn == 0 && dragController.onMouseClicked(x, y, viewModel)) || super.mouseClicked(x, y, btn);
    }

    @Override
    public boolean mouseReleased(double x, double y, int btn) {
        if (btn == 0 && isDraggingIndicator) {
            isDraggingIndicator = false;
            return true;
        }
        return dragController.onMouseReleased() || super.mouseReleased(x, y, btn);
    }

    @Override
    public boolean mouseDragged(double x, double y, int btn, double dx, double dy) {
        if (isDraggingIndicator && btn == 0) {
            viewModel.indicatorX = (float) (x - indicatorDragOffsetX);
            viewModel.indicatorY = (float) (y - indicatorDragOffsetY);
            return true;
        }

        dragController.onMouseDragged(x, y, viewModel);
        return super.mouseDragged(x, y, btn, dx, dy);
    }
}