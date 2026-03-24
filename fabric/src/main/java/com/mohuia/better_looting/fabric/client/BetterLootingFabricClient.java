package com.mohuia.better_looting.fabric.client;

import com.mohuia.better_looting.client.BetterLootingClient;
import net.fabricmc.api.ClientModInitializer;

public class BetterLootingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BetterLootingClient.init();
    }
}