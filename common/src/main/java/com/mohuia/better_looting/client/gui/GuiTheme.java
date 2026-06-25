package com.mohuia.better_looting.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 统一的界面主题：以黑白灰为基调，分组标题使用一组柔和的彩色强调色加以区分。
 * 同时提供颜色插值、圆角与投影等绘制辅助，供条件设置界面及各现代组件复用。
 */
public final class GuiTheme {
    private GuiTheme() {}

    // —— 强调（白） ——
    public static final int ACCENT = 0xFFFFFFFF;        // 选中 / 悬停（纯白）
    public static final int ACCENT_SOFT = 0x33FFFFFF;   // 半透明白（悬停 / 选中背景）
    public static final int ACCENT_FAINT = 0x1AFFFFFF;  // 极淡白（轨道 / 微高亮）

    // —— 面板 ——
    public static final int PANEL_BG = 0xC0000000;      // 主面板背景（黑半透明）
    public static final int PANEL_BG_DEEP = 0xE6121212;  // 更深背景（下拉浮层 / 卡片，近不透明避免穿透）
    public static final int PANEL_BORDER = 0x60FFFFFF;  // 面板边框
    public static final int CARD_BG = 0x33000000;       // 分组卡片背景

    // —— 控件 ——
    public static final int WIDGET_BG = 0x33000000;        // 控件默认背景
    public static final int WIDGET_BG_HOVER = 0x33FFFFFF;  // 控件悬停背景
    public static final int WIDGET_BORDER = 0x40FFFFFF;    // 控件默认边框

    // —— 文本 ——
    public static final int TEXT = 0xFFFFFFFF;        // 主文本
    public static final int TEXT_MUTED = 0xFFBBBBBB;  // 次要文本
    public static final int TEXT_DIM = 0xFF777777;    // 暗淡文本
    public static final int TEXT_VALUE = 0xFFFFFFFF;  // 数值 / 高亮值（白）
    public static final int TEXT_ON = 0xFFFFFFFF;     // 开启态（白）
    public static final int TEXT_OFF = 0xFF777777;    // 关闭态（灰）

    // —— 值标签（pill）——
    public static final int VALUE_PILL_BG = 0x40FFFFFF;       // 当前值胶囊背景（淡白）
    public static final int VALUE_PILL_BG_HOVER = 0x59FFFFFF; // 悬停时更亮

    // —— 开关 ——
    public static final int TOGGLE_TRACK_ON = 0xFFE0E0E0;   // 开关轨道（开，浅白）
    public static final int TOGGLE_TRACK_OFF = 0x40FFFFFF;  // 开关轨道（关，淡白）
    public static final int TOGGLE_KNOB_ON = 0xFF1A1A1A;    // 开关圆点（开，深黑，与白轨对比）
    public static final int TOGGLE_KNOB_OFF = 0xFFBBBBBB;   // 开关圆点（关，灰）

    // —— 滚动条 ——
    public static final int SCROLLBAR_TRACK = 0x40000000;
    public static final int SCROLLBAR_THUMB = 0xFF666666;
    public static final int SCROLLBAR_THUMB_HOVER = 0xFFDDDDDD;

    // —— 分隔线 ——
    public static final int DIVIDER_LINE = 0x33FFFFFF;

    // —— 投影 ——
    public static final int SHADOW_OUTER = 0x30000000; // 外层柔和投影
    public static final int SHADOW_INNER = 0x50000000; // 内层较实投影

    // —— 分组标题强调色（柔和彩色，按分区区分）——
    public static final int SECTION_TEXT = 0xFFFFB74D;
    public static final int SECTION_APPEARANCE = 0xFFFF8A65;
    public static final int SECTION_COUNT = 0xFFFFD54F;
    public static final int SECTION_PANELS = 0xFFF06292;
    public static final int SECTION_ACTIVATION = 0xFF4FC3F7;
    public static final int SECTION_SCROLL = 0xFF4DD0E1;
    public static final int SECTION_PICKUP = 0xFF81C784;
    public static final int SECTION_FILTER = 0xFFBA68C8;
    public static final int SECTION_MERGE = 0xFF4DB6AC;

    // —— Tooltip 语义色 ——
    public static final int TOOLTIP_GOOD = 0xFF81C784;      // 推荐/安全
    public static final int TOOLTIP_WARN = 0xFFFFD54F;      // 注意
    public static final int TOOLTIP_CAUTION = 0xFFFF8A65;   // 谨慎/冲突风险
    public static final int TOOLTIP_NEUTRAL = 0xFFAAAAAA;    // 中性/关闭
    public static final int TOOLTIP_ACTIVE = 0xFF4FC3F7;     // 活跃/动态
    public static final int TOOLTIP_TOGGLE = 0xFFBA68C8;     // 切换/持久

    // =============================================
    // 绘制辅助
    // =============================================

    /** 按 t∈[0,1] 在两个 ARGB 颜色间线性插值。 */
    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    /** 在矩形外侧绘制一圈柔和投影（向右、向下偏移，营造悬浮层次）。 */
    public static void drawShadow(GuiGraphics gui, int x, int y, int w, int h) {
        // 外层淡影（偏移 2px）
        gui.fill(x + 2, y + h, x + w + 2, y + h + 2, SHADOW_OUTER);
        gui.fill(x + w, y + 2, x + w + 2, y + h, SHADOW_OUTER);
        // 内层较实（偏移 1px，紧贴边）
        gui.fill(x + 1, y + h, x + w + 1, y + h + 1, SHADOW_INNER);
        gui.fill(x + w, y + 1, x + w + 1, y + h, SHADOW_INNER);
    }
}
