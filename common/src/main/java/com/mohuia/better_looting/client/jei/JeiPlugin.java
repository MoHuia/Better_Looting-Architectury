package com.mohuia.better_looting.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {

    private static IJeiRuntime jeiRuntime = null;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("better_looting", "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    public static ItemStack getIngredientUnderMouse() {
        if (jeiRuntime == null) return ItemStack.EMPTY;

        Optional<ITypedIngredient<?>> hoveredList = jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse();
        if (hoveredList.isPresent()) {
            return hoveredList.get().getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
        }

        Optional<ITypedIngredient<?>> hoveredBookmark = jeiRuntime.getBookmarkOverlay().getIngredientUnderMouse();
        return hoveredBookmark.map(iTypedIngredient -> iTypedIngredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);

    }
}