package com.mohuia.better_looting.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;


public class KeyInit {
    private static final String CATEGORY = "key.categories.better_looting";

    public static final KeyMapping TOGGLE_FILTER = new KeyMapping(
            "key.better_looting.toggle_filter",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    public static final KeyMapping PICKUP = new KeyMapping(
            "key.better_looting.pickup",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.better_looting.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_AUTO = new KeyMapping(
            "key.better_looting.toggle_auto",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping SHOW_OVERLAY = new KeyMapping(
            "key.better_looting.show_overlay",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping SCROLL_UP = new KeyMapping(
            "key.better_looting.scroll_up",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping SCROLL_DOWN = new KeyMapping(
            "key.better_looting.scroll_down",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping SCROLL_MODIFIER = new KeyMapping(
            "key.better_looting.scroll_modifier",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
}