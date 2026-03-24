package com.mohuia.better_looting.client.jei;

import dev.architectury.platform.Platform; // 关键：导入 Architectury 的平台工具
import net.minecraft.world.item.ItemStack;

public class JeiCompat {
    public static final boolean IS_JEI_LOADED = Platform.isModLoaded("jei");

    public static ItemStack getHoveredItem() {
        if (IS_JEI_LOADED) {
            return Internal.getHoveredItem();
        }
        return ItemStack.EMPTY;
    }

    private static class Internal {
        static ItemStack getHoveredItem() {
            return JeiPlugin.getIngredientUnderMouse();
        }
    }
}