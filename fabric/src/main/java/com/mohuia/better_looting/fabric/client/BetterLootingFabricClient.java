package com.mohuia.better_looting.fabric.client;

import com.mohuia.better_looting.client.BetterLootingClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric 端的客户端（Client）初始化入口。
 * 仅当游戏以客户端模式（带图形界面）运行时才会加载此单例。
 * 主要用于处理仅限客户端的逻辑，例如按键绑定、屏幕渲染、GUI 交互等。
 */
public class BetterLootingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 调用仅限客户端执行的核心初始化逻辑
        BetterLootingClient.init();
    }
}