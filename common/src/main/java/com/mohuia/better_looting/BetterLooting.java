package com.mohuia.better_looting;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.event.CommonEvents;
import com.mohuia.better_looting.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterLooting {
    public static final String MODID = "better_looting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init() {
        BetterLootingConfig.init();

        NetworkHandler.register();

        CommonEvents.init();

        LOGGER.info("Better Looting Common Initialized.");
    }
}