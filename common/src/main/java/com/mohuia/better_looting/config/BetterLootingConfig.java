package com.mohuia.better_looting.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.util.Mth;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BetterLootingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Platform.getConfigFolder().resolve("better_looting.json").toFile();

    public float xOffset = 0.0f;
    public float yOffset = 0.0f;
    public float uiScale = 0.75f;
    public int panelWidth = 120;
    public float visibleRows = 4.5f;
    public float globalAlpha = 0.9f;

    public ActivationMode activationMode = ActivationMode.ALWAYS;
    public ScrollMode scrollMode = ScrollMode.ALWAYS;

    public float lookDownAngle = 45.0f;
    public float scanRangeXZ = 3.0f;
    public float scanRangeY = 1.5f;

    public enum ActivationMode { ALWAYS, LOOK_DOWN, STAND_STILL, KEY_HOLD, KEY_TOGGLE }
    public enum ScrollMode { ALWAYS, KEY_BIND, STAND_STILL }

    private static BetterLootingConfig INSTANCE = new BetterLootingConfig();
    public static BetterLootingConfig get() { return INSTANCE; }

    public void validate() {
        this.xOffset = Mth.clamp(this.xOffset, -2000.0f, 2000.0f);
        this.yOffset = Mth.clamp(this.yOffset, -2000.0f, 2000.0f);
        this.uiScale = Mth.clamp(this.uiScale, 0.1f, 4.0f);
        this.panelWidth = Mth.clamp(this.panelWidth, 80, 500);
        this.visibleRows = Mth.clamp(this.visibleRows, 1.0f, 20.0f);
        this.globalAlpha = Mth.clamp(this.globalAlpha, 0.1f, 1.0f);
        this.lookDownAngle = Mth.clamp(this.lookDownAngle, 0.0f, 90.0f);
        this.scanRangeXZ = Mth.clamp(this.scanRangeXZ, 0.5f, 8.0f);
        this.scanRangeY = Mth.clamp(this.scanRangeY, 0.5f, 5.0f);

        if (this.activationMode == null) this.activationMode = ActivationMode.ALWAYS;
        if (this.scrollMode == null) this.scrollMode = ScrollMode.ALWAYS;
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            INSTANCE = new BetterLootingConfig();
            save();
        } else {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                BetterLootingConfig loaded = GSON.fromJson(reader, BetterLootingConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    INSTANCE.validate();
                }
            } catch (Exception e) {
                INSTANCE = new BetterLootingConfig();
                save();
            }
        }
    }

    public static void init() {
        load();
    }
}