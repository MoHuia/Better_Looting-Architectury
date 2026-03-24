package com.mohuia.better_looting.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;

public class Utils {

    public static boolean shouldShowTooltip(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getMaxDamage() > 0 || stack.isEnchanted()) return true;
        Item item = stack.getItem();
        return item instanceof ArmorItem || item instanceof TieredItem ||
                item instanceof ProjectileWeaponItem || item instanceof ShieldItem ||
                item instanceof ElytraItem || item instanceof TridentItem;
    }

    public static int getItemStackDisplayColor(ItemStack stack) {
        TextColor textColor = stack.getHoverName().getStyle().getColor();
        if (textColor != null) return textColor.getValue() | 0xFF000000;
        ChatFormatting formatting = stack.getRarity().color;
        if (formatting.getColor() != null) return formatting.getColor() | 0xFF000000;
        return 0xFFFFFFFF;
    }

    public static int applyAlpha(int color, float alpha) {
        int prevAlpha = (color >>> 24);
        int newAlpha = (int) (prevAlpha * alpha);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    public static int colorWithAlpha(int baseColor, int alpha255) {
        return (baseColor & 0x00FFFFFF) | (alpha255 << 24);
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    public static float easeOutCubic(float x) {
        return 1 - (float) Math.pow(1 - x, 3);
    }
}