package com.mohuia.better_looting.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mohuia.better_looting.client.overlay.ItemCountRenderer;
import com.mohuia.better_looting.config.BetterLootingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(
            method = "renderEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void betterlooting$renderItemCount(
            Entity entity,
            double camX, double camY, double camZ,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo ci
    ) {
        if (BetterLootingConfig.get().itemCountDisplayMode == BetterLootingConfig.DisplayMode.OFF) return;

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (dispatcher.camera == null) return;

        int maxDistance = BetterLootingConfig.get().itemCountRenderDistance;
        if (dispatcher.distanceToSqr(entity) > maxDistance * maxDistance) return;

        if (entity instanceof ItemEntity itemEntity) {
            var offset = dispatcher.getRenderer(entity).getRenderOffset(entity, partialTicks);
            int light = dispatcher.getPackedLightCoords(entity, partialTicks);
            double nx = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - camX + offset.x();
            double ny = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - camY + offset.y();
            double nz = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - camZ + offset.z();

            poseStack.pushPose();
            poseStack.translate(nx, ny, nz);
            ItemCountRenderer.renderItemCount(itemEntity, poseStack, bufferSource, light, (ERDAccessor) dispatcher);
            poseStack.popPose();
        }
    }
}
