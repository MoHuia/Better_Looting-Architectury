package com.mohuia.better_looting.client.gui;

/**
 * 统一的界面主题配色（简约黑白）。
 * 仅使用黑、白、灰阶，无彩色装饰，供条件设置界面及现代组件复用。
 */
public final class GuiTheme {
    private GuiTheme() {}

    // —— 强调（白） ——
    public static final int ACCENT = 0xFFFFFFFF;        // 选中 / 悬停（纯白）
    public static final int ACCENT_SOFT = 0x33FFFFFF;   // 半透明白（悬停 / 选中背景）
    public static final int ACCENT_FAINT = 0x1AFFFFFF;  // 极淡白（轨道 / 微高亮）

    // —— 面板 ——
    public static final int PANEL_BG = 0xC0000000;      // 主面板背景（黑半透明）
    public static final int PANEL_BG_DEEP = 0xD0000000; // 更深背景（标签栏 / 卡片）
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
}
