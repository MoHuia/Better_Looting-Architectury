package com.mohuia.better_looting;

import com.mohuia.better_looting.command.ModCommands;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.event.CommonEvents;
import com.mohuia.better_looting.network.NetworkHandler;
import com.mohuia.better_looting.network.S2C.PacketSyncConfig;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组公共主类（入口点）
 * 负责初始化双端（客户端+服务端）通用的设置、网络和事件。
 */
public class BetterLooting {
    public static final String MODID = "better_looting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * 公共初始化方法
     * 跨平台加载器（如 Fabric/NeoForge 的入口类）会调用此方法
     */
    public static void init() {
        // 1. 初始化配置文件
        BetterLootingConfig.init();

        // 2. 注册网络通道与数据包
        NetworkHandler.register();

        // 3. 注册通用事件监听器
        CommonEvents.init();

        // 4. 注册模组指令
        ModCommands.register();

        // 5. 注册玩家进出服务器事件 (用于联机同步判定参数)
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide()) {
                BetterLootingConfig config = BetterLootingConfig.get();
                PacketSyncConfig packet = new PacketSyncConfig(config.scanRangeXZ, config.scanRangeY);
                NetworkManager.sendToPlayer((ServerPlayer) player, packet);
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player.level().isClientSide()) {
                BetterLootingConfig.get().serverScanRangeXZ = -1.0f;
                BetterLootingConfig.get().serverScanRangeY = -1.0f;
            }
        });

        LOGGER.info("Better Looting Common Initialized.");
    }
}