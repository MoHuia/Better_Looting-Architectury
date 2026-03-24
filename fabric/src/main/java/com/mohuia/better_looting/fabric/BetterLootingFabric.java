package com.mohuia.better_looting.fabric;

import com.mohuia.better_looting.BetterLooting;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric 端的通用（Common）初始化入口。
 * 当 Fabric Loader 启动时，会自动调用这里的 onInitialize()。
 * 主要用于加载双端共用的逻辑，例如网络注册、通用事件、物品/方块注册等。
 */
public class BetterLootingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // 调用你在 Common 包里写的核心初始化逻辑
        BetterLooting.init();
    }
}