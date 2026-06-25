package com.mohuia.better_looting.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 下拉框：闭合行显示标签 + 当前值 + ▼ 箭头；点击展开向下（空间不足时向上）浮层列出全部选项。
 * 浮层需绘制在所有滚动组件之上且不受裁剪区限制，故通过 {@link Host} 回调由宿主界面在顶层渲染并优先派发输入。
 */
public class Dropdown extends AbstractButton {

    /** 宿主界面：负责在顶层渲染展开浮层并把点击/滚轮优先转交给当前展开的下拉框。 */
    public interface Host {
        void onDropdownOpen(Dropdown dropdown);
        void onDropdownClose(Dropdown dropdown);
    }

    private final Component label;
    private final Supplier<List<Component>> optionsSupplier;
    private final IntSupplier selectedIndexSupplier;
    private final Consumer<Integer> onSelect;
    private final Host host;

    private boolean expanded = false;
    private int popupScroll = 0; // 浮层内部滚动：首个可见选项的索引偏移
    private final HoverAnim hover = new HoverAnim();

    private static final int OPT_H = 18;       // 浮层单个选项高度
    private static final int MAX_VISIBLE = 8;  // 浮层最多可见选项数，超出内部滚动
    private static final int POPUP_PAD = 10;   // 浮层文字左右内边距
    private static final int VALUE_W = 110;    // 右侧值区/浮层的固定宽度

    public Dropdown(int x, int y, int width, int height, Component label,
                    Supplier<List<Component>> optionsSupplier, IntSupplier selectedIndexSupplier,
                    Consumer<Integer> onSelect, Host host, Tooltip tooltip) {
        super(x, y, width, height, label);
        this.label = label;
        this.optionsSupplier = optionsSupplier;
        this.selectedIndexSupplier = selectedIndexSupplier;
        this.onSelect = onSelect;
        this.host = host;
        if (tooltip != null) this.setTooltip(tooltip);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void collapse() {
        if (!expanded) return;
        expanded = false;
        if (host != null) host.onDropdownClose(this);
    }

    @Override
    public void onPress() {
        if (expanded) {
            collapse();
        } else {
            expanded = true;
            popupScroll = clampScroll(selectedIndexSupplier.getAsInt() - MAX_VISIBLE / 2);
            if (host != null) host.onDropdownOpen(this);
        }
    }

    // --- 浮层几何 ---

    private int optionCount() {
        return optionsSupplier.get().size();
    }

    private int visibleCount() {
        return Math.min(optionCount(), MAX_VISIBLE);
    }

    private int popupHeight() {
        return visibleCount() * OPT_H;
    }

    /** 浮层宽度：固定的右侧值区宽度（不随行宽变化），但不超过行宽。 */
    private int popupWidth() {
        return Math.min(VALUE_W, width);
    }

    /** 浮层左边 X = 值区左边 X：右对齐到行右边缘（绝对屏幕坐标）。 */
    private int popupX() {
        return getX() + width - popupWidth();
    }

    private int maxScroll() {
        return Math.max(0, optionCount() - MAX_VISIBLE);
    }

    private int clampScroll(int s) {
        return Math.max(0, Math.min(s, maxScroll()));
    }

    /** 下方空间不足时向上展开。 */
    private boolean opensDown() {
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int below = screenH - (getY() + height);
        return below >= popupHeight() || below >= getY();
    }

    /** 浮层顶部 Y（绝对屏幕坐标）。 */
    private int popupTop() {
        return opensDown() ? (getY() + height) : (getY() - popupHeight());
    }

    /** 命中浮层内某个选项则返回其索引，否则返回 -1。 */
    private int optionAt(double mouseX, double mouseY) {
        int px = popupX();
        if (mouseX < px || mouseX > px + popupWidth()) return -1;
        int top = popupTop();
        if (mouseY < top || mouseY >= top + popupHeight()) return -1;
        int row = (int) ((mouseY - top) / OPT_H);
        int idx = popupScroll + row;
        return (idx >= 0 && idx < optionCount()) ? idx : -1;
    }

    /** 鼠标是否落在闭合行或展开浮层范围内。 */
    public boolean isMouseOverExpanded(double mouseX, double mouseY) {
        if (this.isMouseOver(mouseX, mouseY)) return true;
        if (!expanded) return false;
        int top = popupTop();
        int px = popupX();
        return mouseX >= px && mouseX <= px + popupWidth()
                && mouseY >= top && mouseY < top + popupHeight();
    }

    // --- 展开态输入（由宿主优先转交）---

    /** 处理展开态下的点击。返回 true 表示已消费。 */
    public boolean handleExpandedClick(double mouseX, double mouseY) {
        if (!expanded) return false;
        if (this.isMouseOver(mouseX, mouseY)) { // 再次点击闭合行 → 收起
            collapse();
            return true;
        }
        int idx = optionAt(mouseX, mouseY);
        if (idx >= 0) {
            collapse();
            if (idx != selectedIndexSupplier.getAsInt()) onSelect.accept(idx);
            return true;
        }
        // 点击浮层外 → 收起，不消费（让点击落到下层）
        collapse();
        return false;
    }

    /** 处理展开态下的滚轮。返回 true 表示已消费。 */
    public boolean handleExpandedScroll(double mouseX, double mouseY, double delta) {
        if (!expanded || maxScroll() == 0) return false;
        if (!isMouseOverExpanded(mouseX, mouseY)) return false;
        popupScroll = clampScroll(popupScroll - (int) Math.signum(delta));
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        boolean active = this.isHoveredOrFocused() || expanded;
        float t = hover.update(active);
        int x = getX(), y = getY(), w = width, h = height;

        // 行背景：在卡片色与淡白间插值
        gui.fill(x, y, x + w, y + h, GuiTheme.lerpColor(GuiTheme.CARD_BG, GuiTheme.ACCENT_FAINT, t));
        // 左侧高亮条随 hover 渐显
        if (t > 0.01f) {
            gui.fill(x, y, x + 2, y + h, GuiTheme.lerpColor(0x00FFFFFF, GuiTheme.ACCENT, t));
        }

        var font = Minecraft.getInstance().font;
        int ty = y + (h - 8) / 2;

        // 左侧标签
        gui.drawString(font, label, x + 8, ty, GuiTheme.lerpColor(GuiTheme.TEXT_MUTED, GuiTheme.TEXT, t), false);

        // 右侧固定宽度值区（pill 框）
        int boxW = popupWidth();
        int boxX = x + w - boxW;
        int boxColor = GuiTheme.lerpColor(GuiTheme.VALUE_PILL_BG, GuiTheme.VALUE_PILL_BG_HOVER, t);
        gui.fill(boxX, y + 2, x + w, y + h - 2, boxColor);
        if (t > 0.01f) {
            gui.renderOutline(boxX, y + 2, boxW, h - 4, GuiTheme.lerpColor(0x00FFFFFF, GuiTheme.WIDGET_BORDER, t));
        }

        // 值区内：箭头靠右，当前值在箭头左侧
        String arrow = expanded ? "\u25B2" : "\u25BC";
        int arrowX = x + w - 6 - font.width(arrow);
        gui.drawString(font, arrow, arrowX, ty, GuiTheme.lerpColor(GuiTheme.TEXT_DIM, GuiTheme.ACCENT, t), false);

        int idx = selectedIndexSupplier.getAsInt();
        List<Component> opts = optionsSupplier.get();
        if (idx >= 0 && idx < opts.size()) {
            // 值文本在值区内左对齐，超出箭头前的部分由 scissor 裁掉
            int valLeft = boxX + 6;
            gui.enableScissor(boxX, y, arrowX - 2, y + h);
            gui.drawString(font, opts.get(idx), valLeft, ty, GuiTheme.TEXT_VALUE, false);
            gui.disableScissor();
        }
    }

    /**
     * 在顶层渲染展开浮层。由宿主在所有滚动组件之后、脱离裁剪区调用。
     */
    public void renderPopup(GuiGraphics gui, int mouseX, int mouseY) {
        if (!expanded) return;
        var font = Minecraft.getInstance().font;
        int x = popupX(), w = popupWidth();
        int top = popupTop();
        int h = popupHeight();
        int selected = selectedIndexSupplier.getAsInt();
        List<Component> opts = optionsSupplier.get();

        // 浮层投影 + 背景 + 边框
        GuiTheme.drawShadow(gui, x, top, w, h);
        gui.fill(x, top, x + w, top + h, GuiTheme.PANEL_BG_DEEP);
        gui.renderOutline(x, top, w, h, GuiTheme.PANEL_BORDER);

        int vis = visibleCount();
        for (int row = 0; row < vis; row++) {
            int idx = popupScroll + row;
            if (idx < 0 || idx >= opts.size()) continue;
            int oy = top + row * OPT_H;
            boolean isSel = (idx == selected);
            boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= oy && mouseY < oy + OPT_H;

            int bg = isSel ? GuiTheme.ACCENT_SOFT : (hover ? GuiTheme.ACCENT_FAINT : 0);
            if (bg != 0) gui.fill(x, oy, x + w, oy + OPT_H, bg);
            if (isSel) gui.fill(x, oy, x + 2, oy + OPT_H, GuiTheme.ACCENT);

            int textColor = isSel ? GuiTheme.TEXT : (hover ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED);
            int maxS0 = maxScroll();
            int textRight = x + w - (maxS0 > 0 ? 5 : 0) - 2;
            gui.enableScissor(x, oy, textRight, oy + OPT_H);
            gui.drawString(font, opts.get(idx), x + POPUP_PAD, oy + (OPT_H - 8) / 2, textColor, false);
            gui.disableScissor();
        }

        // 内部滚动指示：超出可见数时绘制细滚动条
        int maxS = maxScroll();
        if (maxS > 0) {
            int trackX = x + w - 3;
            int thumbH = Math.max(8, h * vis / opts.size());
            int thumbY = top + (h - thumbH) * popupScroll / maxS;
            gui.fill(trackX, top + 1, trackX + 2, top + h - 1, GuiTheme.SCROLLBAR_TRACK);
            gui.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, GuiTheme.SCROLLBAR_THUMB);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
