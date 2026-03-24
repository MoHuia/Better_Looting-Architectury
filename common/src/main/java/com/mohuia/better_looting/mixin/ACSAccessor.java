package com.mohuia.better_looting.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ACSAccessor {
    @Accessor(value = "leftPos", remap = true)
    int getLeftPos();

    @Accessor(value = "topPos", remap = true)
    int getTopPos();

    @Accessor(value = "imageWidth", remap = true)
    int getImageWidth();

    @Accessor(value = "imageHeight", remap = true)
    int getImageHeight();
}