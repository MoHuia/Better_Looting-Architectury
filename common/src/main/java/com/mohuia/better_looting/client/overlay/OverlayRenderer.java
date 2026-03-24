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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.Optional;

public class OverlayRenderer {
    private final Minecraft mc;

    public OverlayRenderer(Minecraft mc) {
        this.mc = mc;
    }

    public void renderFilterTabs(GuiGraphics gui, int x, int y) {
        RenderSystem.enableBlend();
        var mode = Core.INSTANCE.getFilterMode();
        drawTab(gui, x, y, mode == Core.FilterMode.ALL, 0xFFFFFFFF);
        drawTab(gui, x + 9, y, mode == Core.FilterMode.RARE_ONLY, 0xFFFFD700);
    }

    private void drawTab(GuiGraphics gui, int x, int y, boolean active, int color) {
        int bg = active ? (color & 0x00FFFFFF) | 0x80000000 : 0x40000000;
        int border = active ? color : Utils.colorWithAlpha(color, 136);
        renderRoundedRect(gui, x, y - 8, 6, 6, bg);
        gui.renderOutline(x, y - 8, 6, 6, border);
    }

    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew) {
        ItemStack stack = entry.getItem();
        int count = entry.getCount();

        int bgColor = selected ? Constants.COLOR_BG_SELECTED : Constants.COLOR_BG_NORMAL;
        renderRoundedRect(gui, x, y, width, Constants.ITEM_HEIGHT, Utils.applyAlpha(bgColor, bgAlpha));

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int alpha255 = (int) (textAlpha * 255);

        gui.fill(x + 20, y + 3, x + 21, y + Constants.ITEM_HEIGHT - 3,
                Utils.colorWithAlpha(Utils.getItemStackDisplayColor(stack), alpha255));

        gui.renderItem(stack, x + 3, y + 3);
        String countText = (count > 1) ? compactCount(count) : null;
        gui.renderItemDecorations(mc.font, stack, x + 3, y + 3, countText);

        if (alpha255 <= 10) return;

        var pose = gui.pose();
        int textColor = Utils.colorWithAlpha(selected ? Constants.COLOR_TEXT_WHITE : Constants.COLOR_TEXT_DIM, alpha255);

        pose.pushPose();
        pose.translate(x + 26, y + 8, 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        Component displayName = stack.getHoverName();
        if (stack.getItem() instanceof EnchantedBookItem) {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (!enchants.isEmpty()) {
                Map.Entry<Enchantment, Integer> first = enchants.entrySet().iterator().next();
                displayName = first.getKey().getFullname(first.getValue());
            }
        }

        gui.drawString(mc.font, displayName, 0, 0, textColor, false);
        pose.popPose();

        if (isNew) {
            pose.pushPose();
            pose.translate(x + width - 22, y + 8, 0);
            pose.scale(0.75f, 0.75f, 1.0f);
            gui.drawString(mc.font, "NEW", 0, 0, Utils.colorWithAlpha(Constants.COLOR_NEW_LABEL, alpha255), true);
            pose.popPose();
        }
    }

    private String compactCount(int count) {
        if (count >= 10000) return (count / 1000) + "k";
        return String.valueOf(count);
    }

    public void renderScrollBar(GuiGraphics gui, int total, float maxVis, int x, int y, int h, float alpha, float scroll) {
        gui.fill(x, y, x + 2, y + h, Utils.applyAlpha(Constants.COLOR_SCROLL_TRACK, alpha));
        float ratio = maxVis / total;
        int thumbH = Math.max(10, (int) (h * ratio));
        float progress = (total - maxVis > 0) ? Mth.clamp(scroll / (total - maxVis), 0f, 1f) : 0f;
        renderRoundedRect(gui, x, y + (int) ((h - thumbH) * progress), 2, thumbH,
                Utils.applyAlpha(Constants.COLOR_SCROLL_THUMB, alpha));
    }

    public void renderKeyPrompt(GuiGraphics gui, int x, int startY, int itemHeight, int selIndex, float scroll, float visibleRows, float bgAlpha) {
        float relSel = selIndex - scroll;
        if (relSel <= -1.0f || relSel >= visibleRows + 0.5f) return;

        int y = startY + (int) (relSel * itemHeight) + (itemHeight - 14) / 2;
        float finalAlpha = bgAlpha * (relSel < 0 ? (1f + relSel) : Mth.clamp((visibleRows + 0.5f) - relSel, 0f, 1f));

        if (finalAlpha <= 0.05f) return;

        int boxX = x - 21, boxY = y, boxSize = 14;
        renderRoundedRect(gui, boxX, boxY, boxSize, boxSize, Utils.applyAlpha(Constants.COLOR_KEY_BG, finalAlpha));

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
            gui.drawString(mc.font, text, boxX + (boxSize - tw) / 2, boxY + 3, tc, false);
        } else {
            int scX = boxX + margin;
            Matrix4f mat = gui.pose().last().pose();
            Vector4f min = mat.transform(new Vector4f(scX, boxY, 0, 1.0f));
            Vector4f max = mat.transform(new Vector4f(scX + mw, boxY + boxSize, 0, 1.0f));

            Window win = mc.getWindow();
            double s = win.getGuiScale();
            int sx = (int) (min.x() * s), sy = (int) ((win.getGuiScaledHeight() - max.y()) * s);
            int sw = (int) ((max.x() - min.x()) * s), sh = (int) ((max.y() - min.y()) * s);

            RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(1, sw), Math.max(1, sh));
            int offset = (int) (((Math.cos(Util.getMillis() / 1000.0) + 1.0) / 2.0) * (tw - mw));
            gui.drawString(mc.font, text, scX - offset, boxY + 3, tc, false);
            RenderSystem.disableScissor();
        }
        gui.pose().popPose();
    }

    public void renderTooltip(GuiGraphics gui, ItemStack stack, int screenW, int screenH, OverlayLayout layout, float scroll, int sel) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var lines = stack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
        if (lines.isEmpty()) return;

        int maxW = lines.stream().mapToInt(mc.font::width).max().orElse(0);
        int tw = maxW + 20, th = lines.size() * 10 + 12;

        float relY = sel - scroll;
        int listRight = (int) (layout.baseX + layout.slideOffset + (layout.panelWidth + Constants.LIST_X) * layout.finalScale);
        int listLeft = (int) (layout.baseX + layout.slideOffset + Constants.LIST_X * layout.finalScale);
        int itemCenterY = (int) (layout.baseY + (layout.startY + (relY + 0.5f) * layout.itemHeightTotal) * layout.finalScale);

        int y = Math.max(8, Math.min(screenH - th - 8, itemCenterY - (th / 2)));
        int gap = 12;
        int x = (listRight + gap + tw < screenW - 8) ? listRight + gap : Math.max(8, listLeft - gap - tw);

        gui.renderTooltip(mc.font, lines, Optional.empty(), x, y + 10);
    }

    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }
}