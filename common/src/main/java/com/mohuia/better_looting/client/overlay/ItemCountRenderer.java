package com.mohuia.better_looting.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.mixin.ERDAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Supplier;

public class ItemCountRenderer {

    public static void renderItemCount(
            ItemEntity entity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            ERDAccessor dispatcherAccessor
    ) {
        BetterLootingConfig config = BetterLootingConfig.get();
        BetterLootingConfig.DisplayMode mode = config.itemCountDisplayMode;
        if (mode == BetterLootingConfig.DisplayMode.OFF) return;

        int total = getTotalCount(entity);
        String text = switch (mode) {
            case ITEM_COUNT -> formatItemCount(total, entity);
            case STACK_COUNT -> formatStackCount(entity);
            default -> null;
        };

        if (text == null) return;

        float scale = 0.025f * config.itemCountScale;
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() + 0.75f, 0);
        poseStack.mulPose(dispatcherAccessor.getCameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        Component component = Component.literal(text);
        var matrix4f = poseStack.last().pose();
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int bgColor = (int) (bgOpacity * 255f) << 24;
        Font font = dispatcherAccessor.getFont();
        float x = (float) (-font.width(component) / 2);
        font.drawInBatch(component, x, 0, 0x20FFFFFF, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, bgColor, light);
        font.drawInBatch(component, x, 0, -1, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, light);

        poseStack.popPose();
    }

    private static int getTotalCount(ItemEntity entity) {
        int extra = ((ISuperStack) entity).betterlooting$getExtraCount();
        return entity.getItem().getCount() + extra;
    }

    private static String formatItemCount(int total, ItemEntity entity) {
        if (total >= 1_000_000_000) {
            return String.format("%.3fB", total / 1_000_000_000f);
        }
        if (total >= 1_000_000) {
            return String.format("%.2fM", total / 1_000_000f);
        }
        if (total >= 10_000) {
            return String.format("%.1fK", total / 1_000f);
        }
        if (total > entity.getItem().getMaxStackSize()) {
            return String.valueOf(total);
        }
        return null;
    }

    private static String formatStackCount(ItemEntity entity) {
        int total = getTotalCount(entity);
        int maxStackSize = entity.getItem().getMaxStackSize();
        int stackCount = (int) Math.ceil((double) total / maxStackSize);
        if (stackCount > 1) {
            return stackCount + "x";
        }
        return null;
    }
}
