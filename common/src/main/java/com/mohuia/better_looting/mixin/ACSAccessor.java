package com.mohuia.better_looting.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * AbstractContainerScreen 的 Accessor 接口
 * 用于在模组中获取原版容器 GUI（如物品栏、箱子等）的坐标和尺寸信息，
 * 这对绘制自定义 GUI 面板（如过滤面板）非常有用。
 */
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