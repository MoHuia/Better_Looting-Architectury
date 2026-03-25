package com.mohuia.better_looting.client;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.overlay.HotbarIndicator;
import com.mohuia.better_looting.client.overlay.Overlay;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 客户端主初始化类。
 * 负责注册客户端专属的事件、按键绑定以及初始化核心的 UI 与渲染逻辑。
 */
@Environment(EnvType.CLIENT)
public class BetterLootingClient {

    public static void init() {
        // 1. 注册所有的按键绑定
        KeyMappingRegistry.register(KeyInit.TOGGLE_FILTER);
        KeyMappingRegistry.register(KeyInit.PICKUP);
        KeyMappingRegistry.register(KeyInit.OPEN_CONFIG);
        KeyMappingRegistry.register(KeyInit.TOGGLE_AUTO);
        KeyMappingRegistry.register(KeyInit.SHOW_OVERLAY);
        KeyMappingRegistry.register(KeyInit.SCROLL_UP);
        KeyMappingRegistry.register(KeyInit.SCROLL_DOWN);
        KeyMappingRegistry.register(KeyInit.SCROLL_MODIFIER);

        // 2. 初始化核心逻辑与过滤器事件监听
        Core.INSTANCE.init();
        FilterEvents.init();

        // 3. 注册客户端 Tick 事件
        // 在每一帧逻辑更新后执行，用于处理状态同步、动画进度计算等非渲染逻辑
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level != null) {
                Core.INSTANCE.onClientTick(mc);
                Overlay.INSTANCE.onTick(mc);
            }
        });

        // 4. 注册 HUD 渲染事件
        // 在原版 HUD 渲染完成后执行，用于将我们的覆盖层（Overlay）绘制在屏幕最上层
        ClientGuiEvent.RENDER_HUD.register((gui, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            Overlay.INSTANCE.render(gui, partialTick);
            HotbarIndicator.INSTANCE.render(gui, partialTick);
        });
    }
}