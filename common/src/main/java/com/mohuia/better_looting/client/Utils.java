package com.mohuia.better_looting.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;

/**
 * 客户端通用的工具类。
 * 提供物品判定、颜色通道位运算和 UI 动画插值（Easing）等静态辅助方法。
 */
public class Utils {

    /**
     * 判定是否应该为该物品在覆盖层中显示详细的工具提示（Tooltip）。
     * 主要是为了过滤掉普通的方块或杂项，只为武器、工具、装备或附魔物品等重要资产显示详细信息。
     *
     * @param stack 要检测的物品栈
     * @return 如果是装备、有耐久度或附魔的物品则返回 true
     */
    public static boolean shouldShowTooltip(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 如果物品有耐久度（工具/武器）或者是附魔的，通常需要显示提示
        if (stack.getMaxDamage() > 0 || stack.isEnchanted()) return true;

        Item item = stack.getItem();
        // 进一步筛选出玩家通常关心的核心装备类物品
        return item instanceof ArmorItem || item instanceof TieredItem ||
                item instanceof ProjectileWeaponItem || item instanceof ShieldItem ||
                item instanceof ElytraItem || item instanceof TridentItem;
    }

    /**
     * 获取物品栈的显示颜色（常用于 UI 中的左侧指示条或文字高亮）。
     * 解析优先级：自定义名称颜色 -> 物品稀有度颜色 -> 默认白色。
     *
     * @param stack 目标物品栈
     * @return 包含完全不透明度（Alpha=255）的 ARGB 颜色值
     */
    public static int getItemStackDisplayColor(ItemStack stack) {
        TextColor textColor = stack.getHoverName().getStyle().getColor();
        // 1. 优先使用物品被铁砧重命名或 NBT 指定的颜色
        // 位或运算 0xFF000000 用于将 Alpha 通道强制设为 255 (完全不透明)
        if (textColor != null) return textColor.getValue() | 0xFF000000;

        ChatFormatting formatting = stack.getRarity().color;
        // 2. 其次使用原版稀有度定义的颜色（如罕见为黄色，史诗为紫色）
        if (formatting.getColor() != null) return formatting.getColor() | 0xFF000000;

        // 3. 如果都没有，默认回退为纯白色
        return 0xFFFFFFFF;
    }

    /**
     * 根据给定的浮点比例（0.0~1.0）动态缩放一个 32位 ARGB 颜色的 Alpha 通道。
     * 常用于处理 UI 元素的整体渐隐渐显动画。
     *
     * @param color 原始 ARGB 颜色值
     * @param alpha 缩放比例 (0.0f 到 1.0f)
     * @return 修改 Alpha 通道后的新 ARGB 颜色值
     */
    public static int applyAlpha(int color, float alpha) {
        int prevAlpha = (color >>> 24); // 无符号右移 24 位提取原 Alpha 值
        int newAlpha = (int) (prevAlpha * alpha);
        // 按位与 0x00FFFFFF 清除原 Alpha 通道，再按位或拼接计算后的新 Alpha 通道
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    /**
     * 直接使用 0~255 的整数覆盖一个 ARGB 颜色的 Alpha 通道。
     *
     * @param baseColor 基础颜色值
     * @param alpha255  新的 Alpha 值 (0 - 255)
     * @return 覆盖 Alpha 后的 ARGB 颜色值
     */
    public static int colorWithAlpha(int baseColor, int alpha255) {
        return (baseColor & 0x00FFFFFF) | (alpha255 << 24);
    }

    /**
     * 缓动函数：Ease-Out-Back (回弹缓出)。
     * 动画会快速向目标值移动，稍微冲过头（Overshoot）然后再平滑回弹到目标位置。
     * 物理感强，非常适合用于 UI 弹窗的“弹出”进场动画，增加 Q 弹感。
     *
     * @param x 线性动画进度 (0.0 到 1.0)
     * @return 经过缓动曲线映射后的进度值
     */
    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        // 数学公式: 1 + c3 * (x - 1)^3 + c1 * (x - 1)^2
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    /**
     * 缓动函数：Ease-Out-Cubic (三次缓出)。
     * 动画开始时速度较快，随后平滑减速直至停止。
     * 相比线性插值，它在结束时更平滑，适合用于列表滚动等需要“平滑刹车”的动画。
     *
     * @param x 线性动画进度 (0.0 到 1.0)
     * @return 经过缓动曲线映射后的进度值
     */
    public static float easeOutCubic(float x) {
        // 数学公式: 1 - (1 - x)^3
        return 1 - (float) Math.pow(1 - x, 3);
    }
}