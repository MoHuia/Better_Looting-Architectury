package com.mohuia.better_looting.client;

import com.mohuia.better_looting.client.filter.FilterEvents;
import com.mohuia.better_looting.client.overlay.HotbarIndicator;
import com.mohuia.better_looting.client.overlay.Overlay;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BetterLootingClient {

    public static void init() {

        KeyMappingRegistry.register(KeyInit.TOGGLE_FILTER);
        KeyMappingRegistry.register(KeyInit.PICKUP);
        KeyMappingRegistry.register(KeyInit.OPEN_CONFIG);
        KeyMappingRegistry.register(KeyInit.TOGGLE_AUTO);
        KeyMappingRegistry.register(KeyInit.SHOW_OVERLAY);
        KeyMappingRegistry.register(KeyInit.SCROLL_UP);
        KeyMappingRegistry.register(KeyInit.SCROLL_DOWN);
        KeyMappingRegistry.register(KeyInit.SCROLL_MODIFIER);

        Core.INSTANCE.init();
        FilterEvents.init();

        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.level != null) {
                Core.INSTANCE.onClientTick(mc);
                Overlay.INSTANCE.onTick(mc);
            }
        });

        ClientGuiEvent.RENDER_HUD.register((gui, partialTick) -> {
            Overlay.INSTANCE.render(gui, partialTick);

            HotbarIndicator.INSTANCE.render(gui, partialTick);
        });
    }
}