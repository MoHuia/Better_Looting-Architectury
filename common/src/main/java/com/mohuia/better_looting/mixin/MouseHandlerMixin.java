package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.filter.FilterBlacklist;
import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.filter.FilterPanel;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.client.inventory.LootListInteraction;
import com.mohuia.better_looting.client.jei.JeiCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
 * 用于接管游戏内的鼠标滚动、点击和移动事件，实现快捷过滤操作及自定义面板的交互。
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
        if (this.minecraft.getWindow() == null) return this.minecraft.mouseHandler.xpos();
        return this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
    }

    /**
     * 获取考虑了 GUI 缩放比例后的实际鼠标 Y 坐标
     */
    @Unique
    private double better_looting$getScaledMouseY() {
        if (this.minecraft.getWindow() == null) return this.minecraft.mouseHandler.ypos();
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
        if (!Core.INSTANCE.shouldIgnoreScroll()) {
            Core.INSTANCE.performScroll(yOffset);
            ci.cancel();
            return;
        }

        // 仅在打开容器界面时处理过滤面板和物品栏列表的滚动
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 如果鼠标悬停在过滤面板上，将滚轮事件传递给面板并取消原版响应
        if (FilterPanel.isOpen() && FilterEvents.isMouseOverPanel(mouseX, mouseY, screen)) {
            if (FilterPanel.scroll(yOffset)) {
                ci.cancel();
            }
        }

        // 物品栏掉落物列表滚动
        if (screen instanceof InventoryScreen) {
            if (LootListInteraction.INSTANCE.isMouseOverList(mouseX, mouseY)) {
                LootListInteraction.INSTANCE.handleScroll(yOffset);
                ci.cancel();
            }
        }
    }

    /**
     * 拦截全局鼠标按键（点击）事件
     */
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void interceptGlobalMousePress(long window, int button, int action, int modifiers, CallbackInfo ci) {
        // 物品拖拽释放（无论当前屏幕，释放拖拽状态）
        if (action == 0 && LootListInteraction.INSTANCE.isDraggingItem()) {
            if (this.minecraft.screen instanceof InventoryScreen invScreen) {
                LootListInteraction.INSTANCE.onItemRelease(invScreen);
            }
            ci.cancel();
            return;
        }

        // 滚动条拖拽释放
        if (action == 0 && LootListInteraction.INSTANCE.isDraggingScrollbar()) {
            LootListInteraction.INSTANCE.onScrollbarRelease();
            ci.cancel();
            return;
        }

        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return;

        double mouseX = better_looting$getScaledMouseX();
        double mouseY = better_looting$getScaledMouseY();

        // 物品栏列表：滚动条按下优先（面板打开时跳过，避免抢夺面板的点击）
        if (action == 1 && containerScreen instanceof InventoryScreen && !FilterPanel.isOpen()) {
            if (LootListInteraction.INSTANCE.isMouseOverScrollbar(mouseX, mouseY)) {
                ci.cancel();
                LootListInteraction.INSTANCE.onScrollbarPress(mouseX, mouseY);
                return;
            }
        }

        // 物品栏列表：物品按下（仅左键，玩家手上没有已拿起的物品时；面板打开时跳过）
        if (action == 1 && button == 0 && containerScreen instanceof InventoryScreen
                && containerScreen.getMenu().getCarried().isEmpty() && !FilterPanel.isOpen()) {
            if (LootListInteraction.INSTANCE.onItemPress(mouseX, mouseY)) {
                ci.cancel();
                return;
            }
        }

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

                    // 左键添加进当前激活的列表，右键移除
                    if (button == 0) {
                        if (FilterPanel.isWhitelistActive()) {
                            FilterWhitelist.INSTANCE.add(target);
                        } else {
                            FilterBlacklist.INSTANCE.add(target);
                        }
                    } else {
                        if (FilterPanel.isWhitelistActive()) {
                            FilterWhitelist.INSTANCE.remove(target);
                        } else {
                            FilterBlacklist.INSTANCE.remove(target);
                        }
                    }

                    // 播放点击音效作为反馈，左键和右键音调不同
                    float pitch = button == 0 ? 1.0f : 0.5f;
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
                }
            }
        }
    }

    /**
     * 拦截鼠标移动事件，驱动物品栏掉落物列表的滚动条拖拽。
     */
    @Inject(method = "onMove", at = @At("HEAD"))
    private void onMouseMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (this.minecraft.getWindow() == null) return;
        double mouseX = xpos * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
        double mouseY = ypos * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();

        if (LootListInteraction.INSTANCE.isDraggingItem()) {
            LootListInteraction.INSTANCE.onItemDrag(mouseX, mouseY);
        }
        if (LootListInteraction.INSTANCE.isDraggingScrollbar()) {
            LootListInteraction.INSTANCE.onScrollbarDrag(mouseX, mouseY);
        }
    }
}
