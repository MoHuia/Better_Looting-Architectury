package com.mohuia.better_looting;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.event.CommonEvents;
import com.mohuia.better_looting.network.NetworkHandler;
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
        // 初始化配置文件
        BetterLootingConfig.init();

        // 注册网络通道与数据包
        NetworkHandler.register();

        // 注册通用事件监听器
        CommonEvents.init();

        LOGGER.info("Better Looting Common Initialized.");
    }
}