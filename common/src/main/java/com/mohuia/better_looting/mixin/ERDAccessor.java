package com.mohuia.better_looting.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface ERDAccessor {
    @Accessor("font")
    Font getFont();

    @Accessor("cameraOrientation")
    Quaternionf getCameraOrientation();
}
