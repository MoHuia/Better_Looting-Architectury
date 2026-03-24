package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.filter.FilterPanel;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.client.jei.JeiCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鼠标处理器的全局 Mixin（高优先级）
 * 用于接管游戏内的鼠标滚动和点击事件，实现快捷过滤操作及自定义面板的交互。
 */
@Mixin(value = MouseHandler.class, priority = 500)
public class MouseHandlerMixin {

    @Shadow @Final private Minecraft minecraft;

    // 点击防抖处理，防止一次点击触发多次逻辑
    @Unique
    private static long better_looting$lastClickTime = 0;

    /**
     * 获取考虑了 GUI 缩放比例后的实际鼠标 X 坐标
     */
    @Unique
    private double better_looting$getScaledMouseX() {
        return this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
    }

    /**
     * 获取考虑了 GUI 缩放比例后的实际鼠标 Y 坐标
     */
    @Unique
    private double better_looting$getScaledMouseY() {
        return this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();
    }

    /**
     * 拦截全局鼠标滚轮事件
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        // 1. 获取玩家是否按下了潜行键 (Shift)
        boolean isShiftDown = this.minecraft.options.keyShift.isDown();

        // 2. 处理模组 Core 逻辑层面的滚动需求
        // 如果玩家没有按下 Shift 键，且 Core 认为不应该忽略滚动，则模组接管滚轮
        if (!isShiftDown && !com.mohuia.better_looting.client.Core.INSTANCE.shouldIgnoreScroll()) {
            com.mohuia.better_looting.client.Core.INSTANCE.performScroll(yOffset);
            ci.cancel();
            return;
        }

        // 仅在打开容器界面时处理过滤面板的滚动
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 如果鼠标悬停在过滤面板上，将滚轮事件传递给面板并取消原版响应
        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, screen)) {
            // 提示：如果你希望在过滤面板上按 Shift 也能滚动其他东西，也可以把 isShiftDown 加到这里
            if (FilterPanel.scroll(yOffset)) {
                ci.cancel();
            }
        }
    }

    /**
     * 拦截全局鼠标按键（点击）事件
     */
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 1. 优先处理对过滤面板本身的直接点击
        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, containerScreen)) {
            ci.cancel();
            if (action == 1) { // 1 代表按下鼠标
                FilterPanel.click(mouseX, mouseY, containerScreen);
            }
            return;
        }

        // 2. 处理快捷添加/移除过滤器的逻辑（左键或右键）
        if (FilterPanel.isOpen() && (button == 0 || button == 1)) {
            // 如果玩家鼠标上正拿着物品，则不触发快捷过滤逻辑，让玩家正常放下物品
            if (!containerScreen.getMenu().getCarried().isEmpty()) return;

            // 获取鼠标悬停的物品（支持 JEI 面板悬停或原版容器槽位悬停）
            ItemStack jeiStack = JeiCompat.getHoveredItem();
            Slot hoveredSlot = FilterEvents.getHoveredSlot(containerScreen, mouseX, mouseY);

            boolean hasTarget = (hoveredSlot != null && hoveredSlot.hasItem()) || (jeiStack != null && !jeiStack.isEmpty());

            if (hasTarget) {
                ci.cancel(); // 拦截原版的点击拿起物品逻辑

                if (action == 1) {
                    // 防抖限制：200ms 内只允许触发一次，防止连续误触
                    if (System.currentTimeMillis() - better_looting$lastClickTime < 200) return;
                    better_looting$lastClickTime = System.currentTimeMillis();

                    // 优先取原版槽位里的物品，如果没有则取 JEI 里的物品
                    ItemStack target = (hoveredSlot != null && hoveredSlot.hasItem()) ? hoveredSlot.getItem() : jeiStack;

                    // 左键（button == 0）添加进白名单，右键移除
                    if (button == 0) {
                        FilterWhitelist.INSTANCE.add(target);
                    } else {
                        FilterWhitelist.INSTANCE.remove(target);
                    }

                    // 播放点击音效作为反馈，左键和右键音调不同
                    float pitch = button == 0 ? 1.0f : 0.5f;
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
                }
            }
        }
    }
}