package com.mohuia.better_looting.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mohuia.better_looting.client.Core;
import dev.architectury.platform.Platform;
import net.minecraft.util.Mth;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 模组的核心配置类，负责管理 BetterLooting 的所有用户偏好设置。
 * 使用 Gson 进行本地化存储，并通过 Architectury API 获取跨平台的配置目录。
 */
public class BetterLootingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Platform.getConfigFolder().resolve("better_looting.json").toFile();

    // ==========================================
    // 视觉与 UI 设置 (Visual & UI Settings)
    // ==========================================
    public float xOffset = 0.0f;
    public float yOffset = 0.0f;
    public float uiScale = 0.75f;
    public int panelWidth = 120;
    public float visibleRows = 4.5f;
    public float globalAlpha = 0.9f;
    public boolean showHotbarIndicator = true;
    public String customOverlayTitle = "Loot Detected";

    // ==========================================
    // 快捷栏指示器悬浮窗设置 (Indicator Settings)
    // ==========================================
    public float indicatorX = -1.0f; // -1 代表使用默认贴合快捷栏的位置
    public float indicatorY = -1.0f;
    public int indicatorRotation = 0; // 支持 0, 90, 180, 270

    // ==========================================
    // 交互模式设置 (Interaction Modes)
    // ==========================================
    public ActivationMode activationMode = ActivationMode.ALWAYS;
    public ScrollMode scrollMode = ScrollMode.ALWAYS;

    // ==========================================
    // 判定参数设置 (Scanning Parameters)
    // ==========================================
    public float lookDownAngle = 45.0f;
    public float scanRangeXZ = 1.0f; // 注意：进阶版这里是 1.0f，旧版是 3.0f，我保留了进阶版的值
    public float scanRangeY = 0.5f;  // 注意：进阶版这里是 0.5f，旧版是 1.5f，同上

    // ==========================================
    // 核心功能设置 (Core Feature Settings)
    // ==========================================
    public boolean enableSuperMerge = true; // 是否开启掉落物超大堆叠合并
    public float mergeRangeXZ = 5.0f;       // 水平合并范围 (最大 10)
    public float mergeRangeY = 5.0f;        // 垂直合并范围 (最大 10)
    public float pickupDelaySeconds = 1.0f; // [从 1.20.1 补回] 拾取延迟秒数

    // ==========================================
    // 状态持久化设置 (Persistent State Settings)
    // ==========================================
    public Core.FilterMode lastFilterMode = Core.FilterMode.ALL; // 记录上次使用的过滤模式
    public boolean lastAutoMode = false;                         // 记录上次是否开启了自动拾取

    /**
     * 触发物品拾取 UI 的条件模式
     */
    public enum ActivationMode { ALWAYS, LOOK_DOWN, STAND_STILL, KEY_HOLD, KEY_TOGGLE }

    /**
     * 允许在物品列表中滚动选择的条件模式
     */
    public enum ScrollMode { ALWAYS, KEY_BIND, INVERT_KEY, STAND_STILL }

    private static BetterLootingConfig INSTANCE = new BetterLootingConfig();
    public static BetterLootingConfig get() { return INSTANCE; }

    /**
     * 校验并限制配置值的范围，防止用户手动修改配置文件导致 UI 越界或崩溃。
     */
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
        this.pickupDelaySeconds = Mth.clamp(this.pickupDelaySeconds, 0.0f, 5.0f); // [从 1.20.1 补回]

        // 防止玩家在 JSON 中误删 title 字段导致渲染时 null.isEmpty() 崩溃
        if (this.customOverlayTitle == null) {
            this.customOverlayTitle = "Loot Detected";
        }

        this.mergeRangeXZ = Mth.clamp(this.mergeRangeXZ, 0.0f, 10.0f);
        this.mergeRangeY = Mth.clamp(this.mergeRangeY, 0.0f, 10.0f);

        this.indicatorRotation = (this.indicatorRotation / 90 * 90) % 360;

        if (this.indicatorRotation < 0) this.indicatorRotation += 360;

        if (this.activationMode == null) this.activationMode = ActivationMode.ALWAYS;
        if (this.scrollMode == null) this.scrollMode = ScrollMode.ALWAYS;

        if (this.lastFilterMode == null) this.lastFilterMode = Core.FilterMode.ALL;
    }

    /**
     * 将当前内存中的配置序列化并保存到本地 JSON 文件。
     */
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从本地 JSON 文件读取配置。若文件不存在或数据损坏，则重置为默认值并重新生成。
     */
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