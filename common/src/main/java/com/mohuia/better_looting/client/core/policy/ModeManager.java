package com.mohuia.better_looting.client.core.policy;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.FilterMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 模式管理器，负责过滤器模式和自动拾取模式的切换与持久化。
 */
public class ModeManager {
    public static final ModeManager INSTANCE = new ModeManager();

    private FilterMode filterMode = FilterMode.ALL;
    private boolean isAutoMode = false;

    private ModeManager() {}

    public void init() {
        BetterLootingConfig cfg = BetterLootingConfig.get();
        this.filterMode = cfg.lastFilterMode;
        this.isAutoMode = cfg.lastAutoMode;
    }

    public void toggleFilterMode() {
        filterMode = (filterMode == FilterMode.ALL) ? FilterMode.RARE_ONLY : FilterMode.ALL;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.lastFilterMode = this.filterMode;
        BetterLootingConfig.save();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Component modeName = Component.translatable("gui.better_looting.config.mode." + filterMode.name().toLowerCase())
                    .withStyle(ChatFormatting.YELLOW);
            Component msg = Component.translatable("message.better_looting.filter_switched", modeName);
            mc.player.displayClientMessage(msg, true);
        }
    }

    public void toggleAutoMode() {
        isAutoMode = !isAutoMode;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.lastAutoMode = this.isAutoMode;
        BetterLootingConfig.save();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Component msg = isAutoMode
                    ? Component.translatable("message.better_looting.auto_on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.better_looting.auto_off").withStyle(ChatFormatting.RED);
            mc.player.displayClientMessage(msg, true);
        }
    }

    public FilterMode getFilterMode() { return filterMode; }
    public boolean isAutoMode() { return isAutoMode; }
}
