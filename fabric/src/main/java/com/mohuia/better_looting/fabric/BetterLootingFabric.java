package com.mohuia.better_looting.fabric;

import com.mohuia.better_looting.BetterLooting;
import net.fabricmc.api.ModInitializer;

public class BetterLootingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BetterLooting.init();
    }
}