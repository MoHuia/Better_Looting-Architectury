package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.core.VisualItemEntry;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * 负责控制战利品悬浮窗 (Overlay) 的核心渲染逻辑与状态管理。
 * 采用单例模式，通过监听游戏 Tick 和 Render 事件来触发界面的更新与绘制。
 */
public class Overlay {
    public static final Overlay INSTANCE = new Overlay();

    private final OverlayState state = new OverlayState();
    private OverlayRenderer renderer;
    private boolean isOverlayToggled = false;

    private Overlay() {}

    /**
     * 处理客户端 Tick 逻辑，主要用于捕获按键状态以切换 UI 显示模式。
     */
    public void onTick(Minecraft mc) {
        if (mc.level == null) return;

        if (BetterLootingConfig.get().activationMode == BetterLootingConfig.ActivationMode.KEY_TOGGLE) {
            while (KeyInit.SHOW_OVERLAY.consumeClick()) {
                isOverlayToggled = !isOverlayToggled;
            }
        }
    }

    /**
     * UI 渲染的主入口。检查显示条件并协调状态更新与实际渲染流程。
     */
    public void render(GuiGraphics gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // 隐藏 GUI (F1) 或打开了其他菜单时暂停渲染
        if (mc.options.hideGui || mc.screen != null) return;

        Core core = Core.INSTANCE;
        List<VisualItemEntry> nearbyItems = core.getNearbyItems();

        // 当周围没有战利品时，快速收起动画并清空渲染
        if (nearbyItems == null || nearbyItems.isEmpty()) {
            state.tick(false, 0, 0, 5);
            renderFlow(gui, mc, core, List.of());
            return;
        }

        boolean conditionMet = checkActivationCondition(mc);
        boolean shouldShow = !core.isAutoMode() && conditionMet;

        if (this.renderer == null) this.renderer = new OverlayRenderer(mc);
        OverlayLayout layout = new OverlayLayout(mc, state.popupProgress);

        // 更新动画与滚动状态
        state.tick(shouldShow, core.getTargetScrollOffset(), nearbyItems.size(), layout.visibleRows);

        // 只有当 UI 弹出进度大于 0 时才进行渲染，优化性能
        if (state.popupProgress > 0.001f) {
            renderFlow(gui, mc, core, nearbyItems);
        }

        // 定期清理失效的物品动画缓存（每秒 1 次）
        if (mc.level != null && mc.level.getGameTime() % 20 == 0) {
            state.cleanupAnimations(nearbyItems);
        }
    }

    /**
     * 控制渲染的整体层级：设置矩阵位移 -> 渲染标题头 -> 渲染物品列表 -> 渲染 Tooltip。
     */
    private void renderFlow(GuiGraphics gui, Minecraft mc, Core core, List<VisualItemEntry> nearbyItems) {
        OverlayLayout layout = new OverlayLayout(mc, state.popupProgress);
        var pose = gui.pose();

        pose.pushPose();
        // 应用基于动画进度的整体偏移与缩放
        pose.translate(layout.baseX + layout.slideOffset, layout.baseY, 0);
        pose.scale(layout.finalScale, layout.finalScale, 1.0f);

        if (state.popupProgress > 0.1f) {
            drawHeader(gui, layout);
        }

        if (!nearbyItems.isEmpty()) {
            renderItemList(gui, core, nearbyItems, layout);
        }

        pose.popPose();

        // Tooltip 需要在矩阵恢复后独立渲染，以避免跟随列表缩放导致文字模糊或错位
        if (state.popupProgress > 0.9f && !nearbyItems.isEmpty()) {
            renderSelectedTooltip(gui, mc, core, nearbyItems, layout);
        }
    }

    /**
     * 渲染滚动列表中的物品项，包含平滑滚动、边缘透明度衰减和列表裁剪 (Scissor) 逻辑。
     */
    private void renderItemList(GuiGraphics gui, Core core, List<VisualItemEntry> nearbyItems, OverlayLayout layout) {
        int startIdx = Mth.floor(state.currentScroll);
        int endIdx = Mth.ceil(state.currentScroll + layout.visibleRows);

        boolean renderPrompt = false;
        float selectedBgAlpha = 0f;

        // 开启严格裁剪区域，防止列表项超出规定显示范围
        layout.applyStrictScissor();

        for (int i = 0; i < nearbyItems.size(); i++) {
            // 剔除完全不可见的物品，优化渲染性能
            if (i < startIdx - 1 || i > endIdx + 1) continue;

            VisualItemEntry entry = nearbyItems.get(i);
            boolean isSelected = (i == core.getSelectedIndex());

            // 计算单个物品入场时的滑入动画偏移量
            float entryProgress = state.getItemEntryProgress(entry.getPrimaryId());
            float entryOffset = (1.0f - Utils.easeOutCubic(entryProgress)) * 40.0f;

            // 计算列表顶部和底部的透明度衰减效果
            float itemAlpha = calculateListEdgeAlpha(i - state.currentScroll, layout.visibleRows);
            if (itemAlpha * state.popupProgress <= 0.05f) continue;

            gui.pose().pushPose();
            gui.pose().translate(entryOffset, 0, 0);

            float finalBgAlpha = itemAlpha * state.popupProgress * layout.globalAlpha;
            float finalTextAlpha = itemAlpha * state.popupProgress;
            int y = layout.startY + (int) ((i - state.currentScroll) * layout.itemHeightTotal);

            renderer.renderItemRow(gui, Constants.LIST_X, y, layout.panelWidth, entry,
                    isSelected, finalBgAlpha, finalTextAlpha, !core.isItemInInventory(entry.getItem().getItem()));

            if (isSelected) {
                renderPrompt = true;
                selectedBgAlpha = finalBgAlpha;
            }
            gui.pose().popPose();
        }

        if (renderPrompt) {
            // 切换为宽松裁剪区域，防止按键提示(Prompt)等略微越界的元素被切掉
            layout.applyLooseScissor();
            renderer.renderKeyPrompt(gui, Constants.LIST_X, layout.startY, layout.itemHeightTotal,
                    core.getSelectedIndex(), state.currentScroll, layout.visibleRows, selectedBgAlpha);
        }

        RenderSystem.disableScissor();

        // 如果物品总数超过可见行数，则渲染滚动条
        if (nearbyItems.size() > layout.visibleRows) {
            int totalVisualH = (int) (layout.visibleRows * layout.itemHeightTotal);
            renderer.renderScrollBar(gui, nearbyItems.size(), layout.visibleRows,
                    Constants.LIST_X - 6, layout.startY, totalVisualH,
                    state.popupProgress * layout.globalAlpha, state.currentScroll);
        }
    }

    /**
     * 为当前选中的物品渲染原版物品信息提示框 (Tooltip)。
     */
    private void renderSelectedTooltip(GuiGraphics gui, Minecraft mc, Core core, List<VisualItemEntry> nearbyItems, OverlayLayout layout) {
        int sel = core.getSelectedIndex();
        if (sel >= 0 && sel < nearbyItems.size()) {
            var stack = nearbyItems.get(sel).getItem();
            if (Utils.shouldShowTooltip(stack)) {
                renderer.renderTooltip(gui, stack, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                        layout, state.currentScroll, sel);
            }
        }
    }

    /**
     * 判断当前玩家的状态是否满足触发悬浮窗的条件（基于配置文件）。
     */
    private boolean checkActivationCondition(Minecraft mc) {
        var cfg = BetterLootingConfig.get();
        if (mc.player == null) return false;

        return switch (cfg.activationMode) {
            case LOOK_DOWN -> mc.player.getXRot() > cfg.lookDownAngle;
            case STAND_STILL -> {
                double dx = mc.player.getX() - mc.player.xo;
                double dz = mc.player.getZ() - mc.player.zo;
                // 利用位移平方差判断玩家是否几乎处于静止状态
                yield (dx * dx + dz * dz) < 0.0001;
            }
            case KEY_HOLD -> !KeyInit.SHOW_OVERLAY.isUnbound() && KeyInit.SHOW_OVERLAY.isDown();
            case KEY_TOGGLE -> isOverlayToggled;
            case ALWAYS -> true;
        };
    }

    /**
     * 绘制悬浮窗的头部，包括标题文字和下划线分隔符。
     */
    private void drawHeader(GuiGraphics gui, OverlayLayout layout) {
        int headerY = layout.startY - 14;
        int titleAlpha = (int)(state.popupProgress * layout.globalAlpha * 255);

        // 1. 获取用户自定义配置的字符串
        String customTitle = BetterLootingConfig.get().customOverlayTitle;

        // 2. 判空：只有当字符串存在且不为空时，才渲染文字
        if (customTitle != null && !customTitle.isEmpty()) {
            gui.pose().pushPose();
            gui.pose().translate(Constants.LIST_X, headerY, 0);
            gui.pose().scale(0.75f, 0.75f, 1.0f);

            // 使用 Component.literal 直接渲染配置中的纯文本
            Component title = Component.literal(customTitle);
            gui.drawString(Minecraft.getInstance().font, title, 0, 0, Utils.colorWithAlpha(0xFFFFD700, titleAlpha), true);
            gui.pose().popPose();
        }

        // 3. 渲染下划线和右侧 Tabs（不受标题是否留空影响）
        int lineColor = Utils.colorWithAlpha(0xFFAAAAAA, (int)(titleAlpha * 0.5));
        gui.fill(Constants.LIST_X, headerY + 10, Constants.LIST_X + layout.panelWidth, headerY + 11, lineColor);
        renderer.renderFilterTabs(gui, Constants.LIST_X + layout.panelWidth - 20, headerY + 10);
    }

    /**
     * 计算列表顶部和底部元素的透明度，使其实现平滑淡入淡出的视觉效果。
     *
     * @param relativeIndex 元素相对于当前滚动位置的索引 (例如 < 0 表示在视口上方)
     * @param visibleRows   配置中定义的最大可见行数
     * @return 0.0f 到 1.0f 之间的透明度乘数
     */
    private float calculateListEdgeAlpha(float relativeIndex, float visibleRows) {
        if (relativeIndex < 0) return Mth.clamp(1.0f + (relativeIndex * 1.5f), 0f, 1f);
        if (relativeIndex > visibleRows - 1.0f) {
            return Mth.clamp(1.0f - (relativeIndex - (visibleRows - 1.0f)), 0f, 1f);
        }
        return 1.0f;
    }
}