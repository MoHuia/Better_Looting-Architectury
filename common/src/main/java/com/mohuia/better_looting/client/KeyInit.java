package com.mohuia.better_looting.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 按键绑定注册表。
 * 定义了模组中所有的自定义快捷键及其默认键位。
 */
public class KeyInit {
    // 设置界面中显示的按键分类名称
    private static final String CATEGORY = "key.categories.better_looting";

    // 切换过滤器状态 (默认: 左 Alt)
    public static final KeyMapping TOGGLE_FILTER = new KeyMapping(
            "key.better_looting.toggle_filter",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    // 拾取选中物品 (默认: F)
    public static final KeyMapping PICKUP = new KeyMapping(
            "key.better_looting.pickup",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    // 打开模组配置界面 (默认: K)
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.better_looting.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    // 切换自动拾取模式 (默认: V)
    public static final KeyMapping TOGGLE_AUTO = new KeyMapping(
            "key.better_looting.toggle_auto",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    // ====================================================================
    // 辅助/动态绑定的按键 (默认未绑定，由代码动态处理鼠标滚轮或特殊组合键)
    // ====================================================================

    // 强制显示覆盖层 UI (无默认键)
    public static final KeyMapping SHOW_OVERLAY = new KeyMapping(
            "key.better_looting.show_overlay",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    // 向上滚动列表 (无默认键)
    public static final KeyMapping SCROLL_UP = new KeyMapping(
            "key.better_looting.scroll_up",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    // 向下滚动列表 (无默认键)
    public static final KeyMapping SCROLL_DOWN = new KeyMapping(
            "key.better_looting.scroll_down",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    // 滚动修饰键，按住时允许通过特定操作滚动列表 (无默认键)
    public static final KeyMapping SCROLL_MODIFIER = new KeyMapping(
            "key.better_looting.scroll_modifier",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
}