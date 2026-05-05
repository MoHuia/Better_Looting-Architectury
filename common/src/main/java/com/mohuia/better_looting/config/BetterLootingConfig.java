package com.mohuia.better_looting.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mohuia.better_looting.client.Core;
import dev.architectury.platform.Platform;
import net.minecraft.util.Mth;

import java.io.File;

/**
 * 模组的核心配置类，负责管理 BetterLooting 的所有用户偏好设置。
 * 使用 NightConfig 库生成带注释的 TOML 文件，以提升用户直接修改配置文件时的体验。
 */
public class BetterLootingConfig {
    // 将后缀名改为 .toml
    private static final File CONFIG_FILE = Platform.getConfigFolder().resolve("better_looting.toml").toFile();

    // 新增：指向旧版本的 JSON 配置文件，用于更新时自动清理
    private static final File OLD_CONFIG_FILE = Platform.getConfigFolder().resolve("better_looting.json").toFile();

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
    public float indicatorX = -1.0f;
    public float indicatorY = -1.0f;
    public int indicatorRotation = 0;

    // ==========================================
    // 交互模式设置 (Interaction Modes)
    // ==========================================
    public ActivationMode activationMode = ActivationMode.ALWAYS;
    public ScrollMode scrollMode = ScrollMode.ALWAYS;

    // ==========================================
    // 判定参数设置 (Scanning Parameters)
    // ==========================================
    public float lookDownAngle = 45.0f;
    public float scanRangeXZ = 1.0f;
    public float scanRangeY = 1.0f;

    // ==========================================
    // 服务端强制覆盖参数 (联机同步专用，不保存到本地文件)
    // ==========================================
    public transient float serverScanRangeXZ = -1.0f;
    public transient float serverScanRangeY = -1.0f;

    // ==========================================
    // 核心功能设置 (Core Feature Settings)
    // ==========================================
    public boolean enableSuperMerge = true;
    public float mergeRangeXZ = 5.0f;
    public float mergeRangeY = 5.0f;
    public float pickupDelaySeconds = 1.0f;
    public int maxHoldTicks = 20;

    // ==========================================
    // 状态持久化设置 (Persistent State Settings)
    // ==========================================
    public Core.FilterMode lastFilterMode = Core.FilterMode.ALL;
    public boolean lastAutoMode = false;

    /**
     * 获取实际生效的水平拾取范围。
     * 如果在联机模式且服务端下发了强制参数，则使用服务端的；否则使用本地配置。
     */
    public float getActualScanRangeXZ() {
        return serverScanRangeXZ > 0 ? serverScanRangeXZ : scanRangeXZ;
    }

    /**
     * 获取实际生效的垂直拾取范围。
     * 如果在联机模式且服务端下发了强制参数，则使用服务端的；否则使用本地配置。
     */
    public float getActualScanRangeY() {
        return serverScanRangeY > 0 ? serverScanRangeY : scanRangeY;
    }

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
        this.pickupDelaySeconds = Mth.clamp(this.pickupDelaySeconds, 0.0f, 5.0f);
        this.maxHoldTicks = Mth.clamp(this.maxHoldTicks, 10, 100);

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
     * 使用 NightConfig 序列化配置并写入注释
     */
    public static void save() {
        try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_FILE)
                .sync()
                .preserveInsertionOrder()
                .build()) {

            // --- 视觉与 UI 设置 ---
            config.setComment("Visual", "视觉与 UI 设置 (Visual & UI Settings)");
            config.set("Visual.xOffset", INSTANCE.xOffset);
            config.set("Visual.yOffset", INSTANCE.yOffset);
            config.set("Visual.uiScale", INSTANCE.uiScale);
            config.set("Visual.panelWidth", INSTANCE.panelWidth);
            config.set("Visual.visibleRows", INSTANCE.visibleRows);
            config.set("Visual.globalAlpha", INSTANCE.globalAlpha);
            config.setComment("Visual.showHotbarIndicator", "是否显示快捷栏过滤指示器");
            config.set("Visual.showHotbarIndicator", INSTANCE.showHotbarIndicator);
            config.setComment("Visual.customOverlayTitle", "自定义悬浮窗标题 (留空则隐藏)");
            config.set("Visual.customOverlayTitle", INSTANCE.customOverlayTitle);

            // --- 指示器设置 ---
            config.setComment("Indicator", "快捷栏指示器悬浮窗设置 (Indicator Settings)");
            config.setComment("Indicator.indicatorX", "-1 代表使用默认贴合快捷栏的位置(建议游戏内具体调整)");
            config.set("Indicator.indicatorX", INSTANCE.indicatorX);
            config.set("Indicator.indicatorY", INSTANCE.indicatorY);
            config.setComment("Indicator.indicatorRotation", "支持 0, 90, 180, 270");
            config.set("Indicator.indicatorRotation", INSTANCE.indicatorRotation);

            // --- 交互模式 ---
            config.setComment("Interaction", "交互模式设置 (Interaction Modes)");
            config.set("Interaction.activationMode", INSTANCE.activationMode.name());
            config.set("Interaction.scrollMode", INSTANCE.scrollMode.name());

            // --- 判定参数 ---
            config.setComment("Scanning", "判定参数设置 (Scanning Parameters)");
            config.setComment("Scanning.lookDownAngle", "HUD触发角度");
            config.set("Scanning.lookDownAngle", INSTANCE.lookDownAngle);
            config.setComment("Scanning.scanRangeXZ", "水平拾取检测范围(联机时受服主强制同步覆盖)");
            config.set("Scanning.scanRangeXZ", INSTANCE.scanRangeXZ);
            config.setComment("Scanning.scanRangeY", "垂直拾取检测范围(联机时受服主强制同步覆盖)");
            config.set("Scanning.scanRangeY", INSTANCE.scanRangeY);

            // --- 核心功能 ---
            config.setComment("Core", "核心功能设置 (Core Feature Settings)");
            config.setComment("Core.enableSuperMerge", "是否开启掉落物超大堆叠合并");
            config.set("Core.enableSuperMerge", INSTANCE.enableSuperMerge);
            config.setComment("Core.mergeRangeXZ", "水平合并范围 (最大 10)");
            config.set("Core.mergeRangeXZ", INSTANCE.mergeRangeXZ);
            config.setComment("Core.mergeRangeY", "垂直合并范围 (最大 10)");
            config.set("Core.mergeRangeY", INSTANCE.mergeRangeY);
            config.setComment("Core.pickupDelaySeconds", "拾取延迟保护 (秒)");
            config.set("Core.pickupDelaySeconds", INSTANCE.pickupDelaySeconds);
            config.setComment("Core.maxHoldTicks", "长按触发时间 (默认 20 ticks = 1秒)");
            config.set("Core.maxHoldTicks", INSTANCE.maxHoldTicks);

            // --- 状态持久化 (不加注释，以免玩家误改) ---
            config.setComment("State.lastFilterMode", "状态持久化数据，请勿更改此处");
            config.set("State.lastFilterMode", INSTANCE.lastFilterMode.name());
            config.set("State.lastAutoMode", INSTANCE.lastAutoMode);

            config.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取 TOML 配置，若损坏或不存在则重建
     */
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            INSTANCE = new BetterLootingConfig();
            save();
            return;
        }

        try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_FILE).sync().build()) {
            config.load();

            // 使用 <Number> 并调用 floatValue()，防止因用户输入整数导致 Double 强转异常
            INSTANCE.xOffset = config.<Number>getOrElse("Visual.xOffset", 0.0f).floatValue();
            INSTANCE.yOffset = config.<Number>getOrElse("Visual.yOffset", 0.0f).floatValue();
            INSTANCE.uiScale = config.<Number>getOrElse("Visual.uiScale", 0.75f).floatValue();
            INSTANCE.panelWidth = config.getOrElse("Visual.panelWidth", 120);
            INSTANCE.visibleRows = config.<Number>getOrElse("Visual.visibleRows", 4.5f).floatValue();
            INSTANCE.globalAlpha = config.<Number>getOrElse("Visual.globalAlpha", 0.9f).floatValue();
            INSTANCE.showHotbarIndicator = config.getOrElse("Visual.showHotbarIndicator", true);
            INSTANCE.customOverlayTitle = config.getOrElse("Visual.customOverlayTitle", "Loot Detected");

            INSTANCE.indicatorX = config.<Number>getOrElse("Indicator.indicatorX", -1.0f).floatValue();
            INSTANCE.indicatorY = config.<Number>getOrElse("Indicator.indicatorY", -1.0f).floatValue();
            INSTANCE.indicatorRotation = config.getOrElse("Indicator.indicatorRotation", 0);

            try { INSTANCE.activationMode = ActivationMode.valueOf(config.getOrElse("Interaction.activationMode", "ALWAYS")); } catch (Exception ignored) {}
            try { INSTANCE.scrollMode = ScrollMode.valueOf(config.getOrElse("Interaction.scrollMode", "ALWAYS")); } catch (Exception ignored) {}

            INSTANCE.lookDownAngle = config.<Number>getOrElse("Scanning.lookDownAngle", 45.0f).floatValue();
            INSTANCE.scanRangeXZ = config.<Number>getOrElse("Scanning.scanRangeXZ", 1.0f).floatValue();
            INSTANCE.scanRangeY = config.<Number>getOrElse("Scanning.scanRangeY", 1.0f).floatValue();

            INSTANCE.enableSuperMerge = config.getOrElse("Core.enableSuperMerge", true);
            INSTANCE.mergeRangeXZ = config.<Number>getOrElse("Core.mergeRangeXZ", 5.0f).floatValue();
            INSTANCE.mergeRangeY = config.<Number>getOrElse("Core.mergeRangeY", 5.0f).floatValue();
            INSTANCE.pickupDelaySeconds = config.<Number>getOrElse("Core.pickupDelaySeconds", 1.0f).floatValue();
            INSTANCE.maxHoldTicks = config.getOrElse("Core.maxHoldTicks", 20);

            try { INSTANCE.lastFilterMode = Core.FilterMode.valueOf(config.getOrElse("State.lastFilterMode", "ALL")); } catch (Exception ignored) {}
            INSTANCE.lastAutoMode = config.getOrElse("State.lastAutoMode", false);

            INSTANCE.validate();
        } catch (Exception e) {
            e.printStackTrace();
            INSTANCE = new BetterLootingConfig();
            save();
        }
    }

    public static void init() {
        // 清理旧版本的 JSON 配置文件
        if (OLD_CONFIG_FILE.exists()) {
            try {
                if (OLD_CONFIG_FILE.delete()) {
                    System.out.println("[BetterLooting] Successfully deleted old JSON config file.");
                }
            } catch (Exception e) {
                System.err.println("[BetterLooting] Failed to delete old JSON config.");
                e.printStackTrace();
            }
        }

        load();
    }
}