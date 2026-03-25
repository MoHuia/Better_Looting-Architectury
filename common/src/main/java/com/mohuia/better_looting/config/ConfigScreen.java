package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.core.VisualItemEntry;
import com.mohuia.better_looting.client.gui.CommonSlider;
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
 */
public class ConfigScreen extends Screen {

    private final ConfigViewModel viewModel;
    private final DragController dragController;
    private OverlayRenderer renderer;
    private final List<VisualItemEntry> previewItems = new ArrayList<>();

    // 预览框的边界坐标，用于处理鼠标拖拽和裁剪
    private float boxLeft, boxTop, boxRight, boxBottom;

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

        int cx = this.width / 2;
        int bottomBase = this.height - 30;

        // 右上角：跳转到详细条件设置的齿轮按钮
        this.addRenderableWidget(Button.builder(Component.literal("⚙"), b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ConditionsScreen(this, this.viewModel));
                    }
                })
                .bounds(this.width - 30, 10, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui." + BetterLooting.MODID + ".config.tooltip.settings")))
                .build());

        // 底部：全局透明度调节滑块
        this.addRenderableWidget(new CommonSlider(
                cx - 100, bottomBase - 25, 200, 20,
                Component.translatable("gui." + BetterLooting.MODID + ".config.opacity"),
                0.1, 1.0, (double) viewModel.globalAlpha,
                val -> viewModel.globalAlpha = val.floatValue()
        ));

        // 底部：重置按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui." + BetterLooting.MODID + ".config.reset"), b -> {
            viewModel.resetToDefault();
            this.clearWidgets();
            this.init();
        }).bounds(cx - 105, bottomBase, 100, 20).build());

        // 底部：保存按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui." + BetterLooting.MODID + ".config.save"), b -> {
            viewModel.saveToConfig();
            this.onClose();
        }).bounds(cx + 5, bottomBase, 100, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 绘制原版的虚化背景
        super.renderBackground(gui, mouseX, mouseY, partialTick);

        // 2. 绘制网格和预览面板（让它们处于滑块和按键的下方）
        renderGrid(gui);
        renderPreview(gui, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 这里会自动触发背景绘制和按键绘制
        super.render(gui, mouseX, mouseY, partialTick);

        // 2. 浮窗信息画在最后，确保它悬浮在最顶层，不会被任何东西遮挡
        renderInfoOverlay(gui);
    }

    /**
     * 渲染左上角的调试/信息面板，实时显示当前位置、尺寸、缩放比和触发条件。
     */
    private void renderInfoOverlay(GuiGraphics gui) {
        gui.fill(5, 5, 130, 80, 0x80000000);
        int startX = 10, startY = 10;
        int color = 0xFFAAAAAA;

        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.pos", (int)viewModel.xOffset, (int)viewModel.yOffset), startX, startY, color, false);
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.size", viewModel.panelWidth, String.format("%.1f", viewModel.visibleRows)), startX, startY + 15, color, false);
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.info.scale", String.format("%.2f", viewModel.uiScale)), startX, startY + 30, color, false);

        Component modeName = Component.translatable("gui." + BetterLooting.MODID + ".config.mode." + viewModel.activationMode.name().toLowerCase());
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.header_condition").copy().append(": ").append(modeName), startX, startY + 45, 0xFFDDDDDD, false);
    }

    /**
     * 渲染模拟的掉落物面板。
     * 使用 PoseStack 处理偏移与缩放，并结合 Scissor 处理可视行数截断。
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

        // 渲染列表标题
        float localMinY = -(Constants.ITEM_HEIGHT / 2.0f) - 14;
        pose.pushPose();
        pose.translate(Constants.LIST_X, localMinY, 0);
        pose.scale(0.75f, 0.75f, 1.0f);
        gui.drawString(this.font, Component.translatable("gui." + BetterLooting.MODID + ".config.preview_title"), 0, 0, headerColor, true);
        pose.popPose();

        // 渲染标题下方的分割线
        gui.fill(Constants.LIST_X, (int)localMinY + 10, Constants.LIST_X + viewModel.panelWidth, (int)localMinY + 11, lineColor);

        // 开启剪裁区域以模拟 visibleRows 设定下的溢出隐藏效果
        gui.enableScissor((int)boxLeft, (int)boxTop, (int)boxRight, (int)boxBottom);

        int startY = -(Constants.ITEM_HEIGHT / 2);
        for (int i = 0; i < previewItems.size(); i++) {
            int y = startY + (i * (Constants.ITEM_HEIGHT + 2));
            renderer.renderItemRow(gui, Constants.LIST_X, y, viewModel.panelWidth,
                    previewItems.get(i), i == 0, viewModel.globalAlpha, 1.0f, i == 1);
        }
        gui.disableScissor();
        pose.popPose();

        // 绘制拖拽调整大小的控制柄
        drawControlHandles(gui, mouseX, mouseY);
    }

    /**
     * 绘制调整面板尺寸和位置的控制热区边框。
     * 根据鼠标悬停状态高亮相应的拖拽区域（右侧拉伸、底部拉伸、右下角整体拉伸）。
     */
    private void drawControlHandles(GuiGraphics gui, int mouseX, int mouseY) {
        DragController.DragMode mode = dragController.getCurrentDragMode();
        int borderCol = (mode == DragController.DragMode.MOVE) ? 0xFFFFFFFF : 0x60FFFFFF;
        gui.renderOutline((int)boxLeft, (int)boxTop, (int)(boxRight-boxLeft), (int)(boxBottom-boxTop), borderCol);

        int cR = dragController.isOverRight(mouseX, mouseY) ? 0xFF55FF55 : 0x8055FF55;
        gui.fill((int)boxRight, (int)boxTop, (int)boxRight+4, (int)boxBottom, cR);

        int cB = dragController.isOverBottom(mouseX, mouseY) ? 0xFF5555FF : 0x805555FF;
        gui.fill((int)boxLeft, (int)boxBottom, (int)boxRight, (int)boxBottom+4, cB);

        int cC = dragController.isOverCorner(mouseX, mouseY) ? 0xFFFF5555 : 0x80FF5555;
        gui.fill((int)boxRight-2, (int)boxBottom-2, (int)boxRight+6, (int)boxBottom+6, cC);
    }

    /**
     * 绘制背景参考网格，辅助玩家对齐 UI 元素。
     */
    private void renderGrid(GuiGraphics gui) {
        int color = 0x20FFFFFF;
        for (int x = 0; x < this.width; x += 20) gui.fill(x, 0, x + 1, this.height, color);
        for (int y = 0; y < this.height; y += 20) gui.fill(0, y, this.width, y + 1, color);
        gui.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x40FF0000); // 中心垂直红线
        gui.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x40FF0000); // 中心水平红线
    }

    // --- 鼠标事件转发给 DragController ---

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        return (btn == 0 && dragController.onMouseClicked(x, y, viewModel)) || super.mouseClicked(x, y, btn);
    }

    @Override
    public boolean mouseReleased(double x, double y, int btn) {
        return dragController.onMouseReleased() || super.mouseReleased(x, y, btn);
    }

    @Override
    public boolean mouseDragged(double x, double y, int btn, double dx, double dy) {
        dragController.onMouseDragged(x, y, viewModel);
        return super.mouseDragged(x, y, btn, dx, dy);
    }
}