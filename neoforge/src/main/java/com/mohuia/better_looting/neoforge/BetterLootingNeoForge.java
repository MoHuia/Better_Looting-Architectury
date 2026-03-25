package com.mohuia.better_looting.neoforge;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.BetterLootingClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;

@Mod(BetterLooting.MODID)
public class BetterLootingNeoForge {

    public BetterLootingNeoForge(IEventBus modEventBus) {

        // 1. 初始化双端通用的逻辑
        BetterLooting.init();

        // 2. 安全地初始化客户端逻辑
        EnvExecutor.runInEnv(Env.CLIENT, () -> BetterLootingClient::init);
    }
}