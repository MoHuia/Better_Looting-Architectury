package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.client.core.VisualItemEntry;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.Optional;

/**
 * 负责渲染“更好拾取”模组客户端界面的核心渲染器。
 * 处理所有 UI 元素的绘制，包括物品列表、过滤器标签、滚动条以及按键提示等。
 */
public class OverlayRenderer {
    private final Minecraft mc;

    public OverlayRenderer(Minecraft mc) {
        this.mc = mc;
    }

    /**
     * 渲染顶部的过滤器标签（例如：全部、仅稀有）。
     */
    public void renderFilterTabs(GuiGraphics gui, int x, int y) {
        RenderSystem.enableBlend();
        var mode = Core.INSTANCE.getFilterMode();
        drawTab(gui, x, y, mode == Core.FilterMode.ALL, 0xFFFFFFFF);
        drawTab(gui, x + 9, y, mode == Core.FilterMode.RARE_ONLY, 0xFFFFD700);
    }

    /**
     * 绘制单个标签的背景与边框。
     * 根据是否处于激活状态 (active) 来应用不同的透明度和颜色。
     */
    private void drawTab(GuiGraphics gui, int x, int y, boolean active, int color) {
        int bg = active ? (color & 0x00FFFFFF) | 0x80000000 : 0x40000000;
        int border = active ? color : Utils.colorWithAlpha(color, 136);
        renderRoundedRect(gui, x, y - 8, 6, 6, bg);
        gui.renderOutline(x, y - 8, 6, 6, border);
    }

    /**
     * 渲染物品列表中的单行条目。
     * 包括背景色块、物品图标、数量、名称以及 "NEW" 标签。
     */
    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew) {
        ItemStack stack = entry.getItem();
        int count = entry.getCount();

        // 渲染条目背景（选中状态会有不同的颜色高亮）
        int bgColor = selected ? Constants.COLOR_BG_SELECTED : Constants.COLOR_BG_NORMAL;
        renderRoundedRect(gui, x, y, width, Constants.ITEM_HEIGHT, Utils.applyAlpha(bgColor, bgAlpha));

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int alpha255 = (int) (textAlpha * 255);

        // 绘制基于物品稀有度或自定义颜色的左侧指示条
        gui.fill(x + 20, y + 3, x + 21, y + Constants.ITEM_HEIGHT - 3,
                Utils.colorWithAlpha(Utils.getItemStackDisplayColor(stack), alpha255));

        // 渲染物品模型及数量
        gui.renderItem(stack, x + 3, y + 3);
        String countText = (count > 1) ? compactCount(count) : null;
        gui.renderItemDecorations(mc.font, stack, x + 3, y + 3, countText);

        // 当透明度过低时跳过文本渲染以优化性能
        if (alpha255 <= 10) return;

        var pose = gui.pose();
        int textColor = Utils.colorWithAlpha(selected ? Constants.COLOR_TEXT_WHITE : Constants.COLOR_TEXT_DIM, alpha255);

        // 使用 PoseStack 进行缩放，使文本适应 UI 比例
        pose.pushPose();
        pose.translate(x + 26, y + 8, 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        // 特殊处理附魔书：如果物品是附魔书，优先显示第一个附魔的名称而不是统一的“附魔书”
        Component displayName = stack.getHoverName();
        if (stack.getItem() instanceof EnchantedBookItem) {
            ItemEnchantments enchants = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchants.isEmpty()) {
                var first = enchants.entrySet().iterator().next();
                displayName = Enchantment.getFullname(first.getKey(), first.getIntValue());
            }
        }

        gui.drawString(mc.font, displayName, 0, 0, textColor, false);
        pose.popPose();

        // 渲染 "NEW" 标签提醒
        if (isNew) {
            pose.pushPose();
            pose.translate(x + width - 22, y + 8, 0);
            pose.scale(0.75f, 0.75f, 1.0f);
            gui.drawString(mc.font, "NEW", 0, 0, Utils.colorWithAlpha(Constants.COLOR_NEW_LABEL, alpha255), true);
            pose.popPose();
        }
    }

    /**
     * 将超过 10000 的数量格式化为 k 单位（例如 12000 -> 12k）。
     */
    private String compactCount(int count) {
        if (count >= 10000) return (count / 1000) + "k";
        return String.valueOf(count);
    }

    /**
     * 渲染滚动条轨道与滑块。
     */
    public void renderScrollBar(GuiGraphics gui, int total, float maxVis, int x, int y, int h, float alpha, float scroll) {
        gui.fill(x, y, x + 2, y + h, Utils.applyAlpha(Constants.COLOR_SCROLL_TRACK, alpha));
        float ratio = maxVis / total;
        int thumbH = Math.max(10, (int) (h * ratio)); // 滑块最小高度限制为 10px
        float progress = (total - maxVis > 0) ? Mth.clamp(scroll / (total - maxVis), 0f, 1f) : 0f;

        renderRoundedRect(gui, x, y + (int) ((h - thumbH) * progress), 2, thumbH,
                Utils.applyAlpha(Constants.COLOR_SCROLL_THUMB, alpha));
    }

    /**
     * 渲染当前选中项左侧的交互按键提示（如拾取按键）。
     * 包含长文本的滚动动画和长按拾取进度条。
     */
    public void renderKeyPrompt(GuiGraphics gui, int x, int startY, int itemHeight, int selIndex, float scroll, float visibleRows, float bgAlpha) {
        float relSel = selIndex - scroll;
        // 如果选中项在可见范围外，则不渲染提示
        if (relSel <= -1.0f || relSel >= visibleRows + 0.5f) return;

        int y = startY + (int) (relSel * itemHeight) + (itemHeight - 14) / 2;

        // 计算边缘淡出透明度，使超出列表边界时的过渡更自然
        float finalAlpha = bgAlpha * (relSel < 0 ? (1f + relSel) : Mth.clamp((visibleRows + 0.5f) - relSel, 0f, 1f));
        if (finalAlpha <= 0.05f) return;

        int boxX = x - 21, boxY = y, boxSize = 14;
        renderRoundedRect(gui, boxX, boxY, boxSize, boxSize, Utils.applyAlpha(Constants.COLOR_KEY_BG, finalAlpha));

        // 渲染长按拾取进度的填充遮罩
        float progress = Core.INSTANCE.getPickupProgress();
        if (progress > 0.0f) {
            int p = 2, s = boxSize - p * 2;
            int fh = (int) (s * progress);
            gui.fill(boxX + p, boxY + boxSize - p - fh, boxX + p + s, boxY + boxSize - p, Utils.colorWithAlpha(0x80808080, (int) (finalAlpha * 255)));
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        String text = KeyInit.PICKUP.getTranslatedKeyMessage().getString().toUpperCase();
        int tw = mc.font.width(text);
        int tc = Utils.colorWithAlpha(Constants.COLOR_TEXT_WHITE, (int) (finalAlpha * 255));
        int margin = 2, mw = boxSize - (margin * 2);

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 10);

        if (tw <= mw) {
            // 文本较短，直接居中绘制
            gui.drawString(mc.font, text, boxX + (boxSize - tw) / 2, boxY + 3, tc, false);
        } else {
            // 文本超长，使用 Scissor (裁剪测试) 并配合时间函数进行左右滚动显示
            int scX = boxX + margin;
            Matrix4f mat = gui.pose().last().pose();
            Vector4f min = mat.transform(new Vector4f(scX, boxY, 0, 1.0f));
            Vector4f max = mat.transform(new Vector4f(scX + mw, boxY + boxSize, 0, 1.0f));

            Window win = mc.getWindow();
            double s = win.getGuiScale();
            int sx = (int) (min.x() * s), sy = (int) ((win.getGuiScaledHeight() - max.y()) * s);
            int sw = (int) ((max.x() - min.x()) * s), sh = (int) ((max.y() - min.y()) * s);

            RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(1, sw), Math.max(1, sh));

            // 使用余弦函数实现平滑的往复滚动效果
            int offset = (int) (((Math.cos(Util.getMillis() / 1000.0) + 1.0) / 2.0) * (tw - mw));
            gui.drawString(mc.font, text, scX - offset, boxY + 3, tc, false);

            RenderSystem.disableScissor();
        }
        gui.pose().popPose();
    }

    /**
     * 渲染物品的原版工具提示 (Tooltip)。
     * 包含了防止 Tooltip 越出屏幕边界的安全判断逻辑。
     */
    public void renderTooltip(GuiGraphics gui, ItemStack stack, int screenW, int screenH, OverlayLayout layout, float scroll, int sel) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var lines = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);        if (lines.isEmpty()) return;

        int maxW = lines.stream().mapToInt(mc.font::width).max().orElse(0);
        int tw = maxW + 20, th = lines.size() * 10 + 12;

        float relY = sel - scroll;
        int listRight = (int) (layout.baseX + layout.slideOffset + (layout.panelWidth + Constants.LIST_X) * layout.finalScale);
        int listLeft = (int) (layout.baseX + layout.slideOffset + Constants.LIST_X * layout.finalScale);
        int itemCenterY = (int) (layout.baseY + (layout.startY + (relY + 0.5f) * layout.itemHeightTotal) * layout.finalScale);

        // 确保 Tooltip 始终在屏幕可视范围内
        int y = Math.max(8, Math.min(screenH - th - 8, itemCenterY - (th / 2)));
        int gap = 12;
        int x = (listRight + gap + tw < screenW - 8) ? listRight + gap : Math.max(8, listLeft - gap - tw);

        gui.renderTooltip(mc.font, lines, Optional.empty(), x, y + 10);
    }

    /**
     * 使用原版矩形渲染拼凑出一个简单的圆角矩形。
     */
    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }
}