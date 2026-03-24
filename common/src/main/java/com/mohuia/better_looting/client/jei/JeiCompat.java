package com.mohuia.better_looting.client.jei;

import dev.architectury.platform.Platform;
import net.minecraft.world.item.ItemStack;

/**
 * JEI (Just Enough Items) 兼容性入口类。
 * 作为一个“软依赖”包装器，确保在玩家没有安装 JEI 时，模组依然可以正常运行而不会崩溃。
 */
public class JeiCompat {
    // 缓存 JEI 是否已加载的状态，避免在每帧渲染或高频事件中重复调用 Platform.isModLoaded 产生性能开销。
    public static final boolean IS_JEI_LOADED = Platform.isModLoaded("jei");

    /**
     * 获取玩家当前在 JEI 面板中鼠标悬停的物品。
     * 供外部调用的安全方法。
     *
     * @return 如果鼠标悬停在 JEI 物品上则返回该物品，否则返回 ItemStack.EMPTY
     */
    public static ItemStack getHoveredItem() {
        if (IS_JEI_LOADED) {
            return Internal.getHoveredItem();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 内部类：物理隔离 JEI 的代码。
     * 意图：JVM 在加载外部类 (JeiCompat) 时，不会立即验证和加载内部类 (Internal)。
     * 只有当 IS_JEI_LOADED 为 true 且实际执行到此处时，才会加载包含 JEI 引用的代码。
     * 这彻底避免了未安装 JEI 时的 ClassNotFoundException。
     */
    private static class Internal {
        static ItemStack getHoveredItem() {
            return JeiPlugin.getIngredientUnderMouse();
        }
    }
}