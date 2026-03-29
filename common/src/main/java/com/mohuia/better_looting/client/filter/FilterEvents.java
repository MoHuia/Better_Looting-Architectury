package com.mohuia.better_looting.client.filter;

import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.mixin.ACSAccessor;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * 负责注册客户端过滤器相关的输入和渲染事件。
 * 通过 Architectury API 挂载到游戏主循环中，确保跨加载器兼容性。
 */
public class FilterEvents {
    private static long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 150;

    public static void init() {
        // 初始化白名单数据（尝试从本地 JSON 加载）
        FilterWhitelist.INSTANCE.init();

        // 当 GUI 初始化完成后触发。重置状态，防止跨屏幕时的状态残留。
        ClientGuiEvent.INIT_POST.register((screen, access) -> {
            lastClickTime = 0;
            FilterPanel.close();
        });

        // 在 GUI 渲染的最后阶段绘制我们的过滤器面板，确保它覆盖在原版 UI 之上。
        ClientGuiEvent.RENDER_POST.register((screen, gui, mouseX, mouseY, delta) -> {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                FilterPanel.render(gui, mouseX, mouseY, containerScreen);
            }
        });

        // 拦截客户端原始键盘输入，用于快捷键检测。
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (client.screen instanceof AbstractContainerScreen<?> && action == GLFW.GLFW_PRESS) {
                // 意图：如果玩家正在搜索框（如 JEI/REI 或原版创造模式搜索栏）中打字，不要触发快捷键。
                if (client.screen.getFocused() instanceof EditBox) {
                    return EventResult.pass();
                }

                // 默认使用左 Alt 键切换面板开关。
                if (KeyInit.OPEN_FILTER_PANEL.matches(keyCode, scanCode)) {
                    FilterPanel.toggle();
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });

        // 拦截鼠标滚轮事件，用于面板的上下滑动。
        ClientRawInputEvent.MOUSE_SCROLLED.register((client, scrollX, scrollY) -> {
            if (client.screen instanceof AbstractContainerScreen<?>) {
                if (FilterPanel.isOpen() && FilterPanel.scroll(scrollY)) {
                    return EventResult.interruptTrue();
                }
            }
            return EventResult.pass();
        });
    }

    /**
     * 判断鼠标是否悬停在过滤器面板区域内。
     * 依赖 Mixin (ACSAccessor) 获取原版容器的坐标数据。
     */
    public static boolean isMouseOverPanel(double mouseX, double mouseY, AbstractContainerScreen<?> screen) {
        ACSAccessor acc = (ACSAccessor) screen;
        int startX = Math.max(2, acc.getLeftPos() - FilterPanel.PANEL_WIDTH - 2);
        int startY = acc.getTopPos() + (acc.getImageHeight() - FilterPanel.PANEL_HEIGHT) / 2;
        return mouseX >= startX && mouseX <= startX + FilterPanel.PANEL_WIDTH &&
                mouseY >= startY && mouseY <= startY + FilterPanel.PANEL_HEIGHT;
    }

    /**
     * 获取当前鼠标悬停的原版容器槽位。
     * 通过相对坐标计算，替代原版私有/受保护的槽位获取方法。
     */
    public static Slot getHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        ACSAccessor acc = (ACSAccessor) screen;
        double relX = mouseX - acc.getLeftPos();
        double relY = mouseY - acc.getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            // 槽位大小通常为 18x18，这里使用 16x16 加 1 像素容差的逻辑进行碰撞检测。
            if (relX >= slot.x - 1 && relX < slot.x + 17 &&
                    relY >= slot.y - 1 && relY < slot.y + 17) {
                return slot;
            }
        }
        return null;
    }
}