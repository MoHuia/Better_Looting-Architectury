package com.mohuia.better_looting.config;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.client.inventory.InventoryLootList;
import com.mohuia.better_looting.client.gui.BackButton;
import com.mohuia.better_looting.client.gui.CommonSlider;
import com.mohuia.better_looting.client.overlay.OverlayRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品栏掉落列表独立配置界面（所见即所得）。
 * 屏幕中央绘制一个模拟背包窗口作为参照，玩家通过拖拽实时调整列表相对背包的
 * 位置偏移、宽度与整体缩放，并用右上角滑块调节独立透明度。
 */
public class InventoryConfigScreen extends Screen {

    private final Screen parent;
    private final ConfigViewModel viewModel;
    private OverlayRenderer renderer;
    private final List<VisualItemEntry> previewItems = new ArrayList<>();

    // 模拟背包窗口尺寸（原版玩家背包 GUI 尺寸）
    private static final int INV_W = 176;
    private static final int INV_H = 166;
    // 模拟背包窗口左上角（屏幕居中）
    private int invLeft, invTop;

    // 列表预览框的实时边界（屏幕像素），用于拖拽热区检测
    private float boxLeft, boxTop, boxRight, boxBottom;

    // 拖拽状态
    private enum DragMode { NONE, MOVE, RESIZE_WIDTH, RESIZE_HEIGHT, RESIZE_SCALE }
    private DragMode dragMode = DragMode.NONE;
    private double dragStartX, dragStartY;
    private float initXOffset, initYOffset, initScale;
    private int initWidth, initHeight;

    // 主题色（与 ConfigScreen 保持一致）
    private static final int COLOR_ACCENT = 0xFF00BFFF;
    private static final int COLOR_PANEL_BG = 0x90101010;
    private static final int COLOR_PANEL_BORDER = 0x50FFFFFF;

    public InventoryConfigScreen(Screen parent, ConfigViewModel viewModel) {
        super(Component.translatable("gui." + BetterLooting.MODID + ".inventory_config.title"));
        this.parent = parent;
        this.viewModel = viewModel;
        previewItems.add(new VisualItemEntry(new ItemStack(Items.DIAMOND, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.GOLDEN_APPLE, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.IRON_SWORD, 1)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.EMERALD, 64)));
        previewItems.add(new VisualItemEntry(new ItemStack(Items.BOOK, 1)));
    }
    // APPEND_MARKER

    @Override
    protected void init() {
        if (this.renderer == null) this.renderer = new OverlayRenderer(this.minecraft);

        // 右上角控制面板
        int panelWidth = 160;
        int startX = this.width - panelWidth - 10;

        this.addRenderableWidget(new ConfigScreen.ModernButton(startX + 5, 15, 60, 20,
                Component.translatable("gui." + BetterLooting.MODID + ".config.reset"), () -> {
            viewModel.inventoryListXOffset = 0f;
            viewModel.inventoryListYOffset = 0f;
            viewModel.inventoryListScale = 1.0f;
            viewModel.inventoryListWidth = 100;
            viewModel.inventoryListHeight = 166;
            viewModel.inventoryListAlpha = 0.9f;
            this.clearWidgets();
            this.init();
        }));

        this.addRenderableWidget(new ConfigScreen.ModernButton(startX + 70, 15, 85, 20,
                Component.translatable("gui." + BetterLooting.MODID + ".config.save"), () -> {
            viewModel.saveToConfig();
            this.onClose();
        }));

        // 独立透明度滑块
        this.addRenderableWidget(new CommonSlider(
                startX + 5, 40, 150, 20,
                Component.translatable("gui." + BetterLooting.MODID + ".config.opacity"),
                0.1, 1.0, (double) viewModel.inventoryListAlpha,
                val -> viewModel.inventoryListAlpha = val.floatValue()
        ));

        // 左上角返回箭头
        this.addRenderableWidget(new BackButton(0, 0, 24, () -> this.minecraft.setScreen(parent)));
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    // =============================================
    // 渲染
    // =============================================

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);

        // 模拟背包窗口居中
        this.invLeft = (this.width - INV_W) / 2;
        this.invTop = (this.height - INV_H) / 2;

        renderMockInventory(gui);
        renderControlPanelBackground(gui);
        renderPreviewList(gui, mouseX, mouseY);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    /** 右上角控制面板背景。 */
    private void renderControlPanelBackground(GuiGraphics gui) {
        int panelWidth = 160;
        int startX = this.width - panelWidth - 10;
        gui.fill(startX, 10, startX + panelWidth + 10, 65, COLOR_PANEL_BG);
        gui.renderOutline(startX, 10, panelWidth + 10, 55, COLOR_PANEL_BORDER);
    }

    /** 绘制模拟背包窗口作为相对定位的参照物。 */
    private void renderMockInventory(GuiGraphics gui) {
        gui.fill(invLeft, invTop, invLeft + INV_W, invTop + INV_H, 0xC0202020);
        gui.renderOutline(invLeft, invTop, INV_W, INV_H, 0x80FFFFFF);
        gui.drawCenteredString(this.font,
                Component.translatable("gui." + BetterLooting.MODID + ".inventory_config.mock_inventory"),
                invLeft + INV_W / 2, invTop + INV_H / 2 - 4, 0xFF777777);
    }

    /** 按当前配置渲染模拟掉落列表，并叠加交互控制手柄（与 ConfigScreen 同构）。 */
    private void renderPreviewList(GuiGraphics gui, int mouseX, int mouseY) {
        int gap = 2;
        float scale = viewModel.inventoryListScale;
        int panelWidth = viewModel.inventoryListWidth;
        int panelHeight = viewModel.inventoryListHeight;
        float listAlpha = viewModel.inventoryListAlpha;
        int itemTotal = Constants.ITEM_HEIGHT + 2;

        // 与游戏内 InventoryLootList 同构：左上角锚点仅由偏移决定，向右下生长。
        // 默认锚点贴合背包左侧（offset=0、宽=默认、scale=1 时右缘恰好贴背包）。
        int panelStartX = invLeft - gap - Constants.LIST_X - InventoryLootList.DEFAULT_LIST_WIDTH
                + Math.round(viewModel.inventoryListXOffset);
        int panelTop = invTop + Math.round(viewModel.inventoryListYOffset);

        // 预览框边界（屏幕坐标）：覆盖物品行区域，宽高随 scale 缩放
        this.boxLeft = panelStartX + Constants.LIST_X * scale;
        this.boxTop = panelTop;
        this.boxRight = boxLeft + panelWidth * scale;
        this.boxBottom = panelTop + panelHeight * scale;

        String effectiveSkin = com.mohuia.better_looting.client.skin.SkinManager.INSTANCE.isAvailable(viewModel.overlaySkin)
                ? viewModel.overlaySkin : "vanilla";
        renderer.setPreviewSkin(effectiveSkin);

        // 按面板高度裁剪可见行，越界部分裁掉，与游戏内表现一致
        int visibleCount = Mth.ceil((float) panelHeight / itemTotal);

        gui.enableScissor((int) boxLeft, (int) boxTop, (int) Math.ceil(boxRight), (int) Math.ceil(boxBottom));
        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(panelStartX, panelTop, 0);
        pose.scale(scale, scale, 1.0f);
        for (int i = 0; i < previewItems.size() && i < visibleCount; i++) {
            int y = i * itemTotal;
            renderer.renderItemRow(gui, Constants.LIST_X, y, panelWidth,
                    previewItems.get(i), false, listAlpha, listAlpha, i == 0, true);
        }
        pose.popPose();
        gui.disableScissor();
        renderer.setPreviewSkin(null);

        drawControlHandles(gui, mouseX, mouseY);
    }

    /** 现代风格交互手柄：边框 + 右侧宽度把手 + 底部高度把手 + 右下角缩放把手（同 ConfigScreen）。 */
    private void drawControlHandles(GuiGraphics gui, int mouseX, int mouseY) {
        int l = (int) boxLeft, t = (int) boxTop, r = (int) boxRight, b = (int) boxBottom;

        boolean overBody = isOverBody(mouseX, mouseY);
        int borderCol = (dragMode == DragMode.MOVE || overBody) ? 0xA0FFFFFF : 0x50FFFFFF;
        gui.renderOutline(l, t, r - l, b - t, borderCol);

        // 右侧宽度把手
        int cR = isOverRight(mouseX, mouseY) ? COLOR_ACCENT : 0xCCFFFFFF;
        gui.fill(r - 1, t + (b - t) / 2 - 10, r + 2, t + (b - t) / 2 + 10, cR);

        // 底部高度把手
        int cB = isOverBottom(mouseX, mouseY) ? COLOR_ACCENT : 0xCCFFFFFF;
        gui.fill(l + (r - l) / 2 - 10, b - 1, l + (r - l) / 2 + 10, b + 2, cB);

        // 右下角缩放把手（L 型）
        int cC = isOverCorner(mouseX, mouseY) ? COLOR_ACCENT : 0xFFFFFFFF;
        gui.fill(r - 6, b - 1, r + 2, b + 2, cC);
        gui.fill(r - 1, b - 6, r + 2, b + 2, cC);
    }

    // =============================================
    // 拖拽热区与交互（向外扩 10px、向内扩 2px，提升边缘抓取容错）
    // =============================================

    private boolean isOverRight(double x, double y) {
        return x >= boxRight && x <= boxRight + 10 && y >= boxTop && y <= boxBottom;
    }

    private boolean isOverBottom(double x, double y) {
        return x >= boxLeft && x <= boxRight && y >= boxBottom && y <= boxBottom + 10;
    }

    private boolean isOverCorner(double x, double y) {
        return x >= boxRight - 2 && x <= boxRight + 10 && y >= boxBottom - 2 && y <= boxBottom + 10;
    }

    private boolean isOverBody(double x, double y) {
        return x >= boxLeft && x <= boxRight && y >= boxTop && y <= boxBottom;
    }

    @Override
    public boolean mouseClicked(double x, double y, int btn) {
        if (btn == 0) {
            // 判定优先级：角落(缩放) > 右缘(宽度) > 底边(高度) > 主体(移动)
            if (isOverCorner(x, y)) dragMode = DragMode.RESIZE_SCALE;
            else if (isOverRight(x, y)) dragMode = DragMode.RESIZE_WIDTH;
            else if (isOverBottom(x, y)) dragMode = DragMode.RESIZE_HEIGHT;
            else if (isOverBody(x, y)) dragMode = DragMode.MOVE;
            else dragMode = DragMode.NONE;

            if (dragMode != DragMode.NONE) {
                dragStartX = x;
                dragStartY = y;
                initXOffset = viewModel.inventoryListXOffset;
                initYOffset = viewModel.inventoryListYOffset;
                initScale = viewModel.inventoryListScale;
                initWidth = viewModel.inventoryListWidth;
                initHeight = viewModel.inventoryListHeight;
                return true;
            }
        }
        return super.mouseClicked(x, y, btn);
    }

    @Override
    public boolean mouseDragged(double x, double y, int btn, double dx, double dy) {
        if (dragMode != DragMode.NONE && btn == 0) {
            double deltaX = x - dragStartX;
            double deltaY = y - dragStartY;
            switch (dragMode) {
                case MOVE -> {
                    viewModel.inventoryListXOffset = initXOffset + (float) deltaX;
                    viewModel.inventoryListYOffset = initYOffset + (float) deltaY;
                }
                case RESIZE_WIDTH -> {
                    // 右缘向右拖增宽，消除缩放影响保证跟手
                    float scaledDelta = (float) deltaX / viewModel.inventoryListScale;
                    viewModel.inventoryListWidth = (int) Mth.clamp(initWidth + scaledDelta, 80, 500);
                }
                case RESIZE_HEIGHT -> {
                    // 底边向下拖增高，消除缩放影响保证跟手
                    float scaledDelta = (float) deltaY / viewModel.inventoryListScale;
                    viewModel.inventoryListHeight = (int) Mth.clamp(initHeight + scaledDelta, 40, 1000);
                }
                case RESIZE_SCALE -> {
                    viewModel.inventoryListScale = Mth.clamp(initScale + (float) (deltaX + deltaY) * 0.005f, 0.1f, 4.0f);
                }
                default -> {}
            }
            return true;
        }
        return super.mouseDragged(x, y, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int btn) {
        if (btn == 0 && dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
            return true;
        }
        return super.mouseReleased(x, y, btn);
    }
}
