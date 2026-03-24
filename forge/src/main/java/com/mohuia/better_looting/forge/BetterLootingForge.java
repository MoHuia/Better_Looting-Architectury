package com.mohuia.better_looting.forge;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.BetterLootingClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BetterLooting.MODID)
public class BetterLootingForge {

    @SuppressWarnings("removal")
    public BetterLootingForge() {
        EventBuses.registerModEventBus(BetterLooting.MODID, FMLJavaModLoadingContext.get().getModEventBus());

        BetterLooting.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BetterLootingClient::init);
    }
}