package com.mohuia.better_looting.client;

/**
 * 存储客户端 UI 渲染所需的所有常量。
 * 包括 ARGB 颜色代码、尺寸比例以及动画平滑度。
 */
public class Constants {

    // ==========================================
    // 颜色常量 (ARGB 格式: 0xAARRGGBB)
    // ==========================================
    public static final int COLOR_BG_NORMAL = 0xB0151515;       // 默认背景色，半透明深灰
    public static final int COLOR_BG_SELECTED = 0xE02A2A2A;     // 选中项背景色，不透明度更高
    public static final int COLOR_TEXT_WHITE = 0xFFECECEC;      // 主文本颜色，亮白
    public static final int COLOR_TEXT_DIM = 0xFFCCCCCC;        // 次要文本颜色，浅灰
    public static final int COLOR_SCROLL_TRACK = 0x40FFFFFF;    // 滚动条轨道颜色
    public static final int COLOR_SCROLL_THUMB = 0xFFFFFFFF;    // 滚动条滑块颜色
    public static final int COLOR_ACCENT_PURPLE = 0xFF9B59B6;   // 紫色强调色
    public static final int COLOR_KEY_BG = 0x80000000;          // 按键提示的半透明黑底
    public static final int COLOR_NEW_LABEL = 0xFFFFAA00;       // "NEW" 标签颜色，亮橙色

    // ==========================================
    // 布局与尺寸常量
    // ==========================================
    public static final int ITEM_HEIGHT = 22;                   // 列表单项高度
    public static final int ITEM_WIDTH = 110;                   // 列表单项宽度
    public static final int LIST_X = 30;                        // 列表渲染起始 X 坐标偏移

    // ==========================================
    // 动画常量
    // ==========================================
    public static final float SCROLL_SMOOTHING = 0.2f;          // 滚动平滑系数，越小越平滑
    public static final float POPUP_SMOOTHING = 0.15f;          // 弹出动画平滑系数
}