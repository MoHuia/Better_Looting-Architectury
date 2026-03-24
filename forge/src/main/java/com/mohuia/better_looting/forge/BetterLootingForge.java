package com.mohuia.better_looting.forge;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.BetterLootingClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge 端的模组主入口。
 * 与 Fabric 在 fabric.mod.json 中区分 entrypoints 不同，
 * Forge 通常使用 @Mod 注解并在代码中动态区分物理端 (Dist)。
 */
@Mod(BetterLooting.MODID)
public class BetterLootingForge {

    @SuppressWarnings("removal") // 压制未来 Forge 版本可能移除 EventBuses 相关的警告
    public BetterLootingForge() {
        // 【关键】向 Architectury 注册 Forge 的 Mod 级事件总线
        // 这样你写在 Common 里的 Architectury 事件才能被 Forge 正确触发
        EventBuses.registerModEventBus(BetterLooting.MODID, FMLJavaModLoadingContext.get().getModEventBus());

        // 1. 初始化双端通用的逻辑
        BetterLooting.init();

        // 2. 安全地初始化客户端逻辑
        // DistExecutor.unsafeRunWhenOn 会检查当前运行环境的物理端 (Dist)。
        // 只有在物理客户端 (Dist.CLIENT) 下，才会执行 BetterLootingClient::init，
        // 这完美避免了在独立服务端上因为加载了客户端的 GUI/Render 类而导致的崩溃（NoClassDefFoundError）。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BetterLootingClient::init);
    }
}