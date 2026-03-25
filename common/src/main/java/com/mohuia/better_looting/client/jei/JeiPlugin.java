package com.mohuia.better_looting.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 实际的 JEI 插件注册类。
 * 通过 @JeiPlugin 注解告诉 JEI 这是一个需要被加载的插件。
 */
@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {

    // 保存 JEI 运行时的实例，以便在游戏过程中随时查询 JEI 的 UI 状态
    private static IJeiRuntime jeiRuntime = null;

    /**
     * 为此插件提供一个唯一的标识符。
     */
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("better_looting", "jei_plugin");
    }

    /**
     * 当 JEI 初始化完成并准备好运行时数据时触发。
     * 在这里捕获 runtime 实例并将其缓存到静态变量中。
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    /**
     * 核心逻辑：获取当前鼠标在 JEI 界面上悬停的物品。
     * 意图：用于配合自定义的过滤器或快捷键（例如按快捷键将 JEI 里的物品直接加入 Better Looting 的白名单）。
     */
    public static ItemStack getIngredientUnderMouse() {
        // 防御性检查：确保 JEI 已经完全加载且 runtime 可用
        if (jeiRuntime == null) return ItemStack.EMPTY;

        // 1. 检查 JEI 右侧的主物品列表 (Ingredient List Overlay)
        Optional<ITypedIngredient<?>> hoveredList = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse();
        if (hoveredList.isPresent()) {
            // 尝试将悬停的成分(可能是流体、气体等)安全地转换为原版的 ItemStack
            return hoveredList.get().getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        }

        // 2. 检查 JEI 左侧的玩家书签列表 (Bookmark Overlay)
        Optional<ITypedIngredient<?>> hoveredBookmark = jeiRuntime.getBookmarkOverlay().getIngredientUnderMouse();
        return hoveredBookmark.map(iTypedIngredient ->
                iTypedIngredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY)
        ).orElse(ItemStack.EMPTY);
    }
}