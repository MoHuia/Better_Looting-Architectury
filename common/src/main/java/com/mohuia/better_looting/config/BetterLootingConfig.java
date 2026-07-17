package com.mohuia.better_looting.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mohuia.better_looting.config.FilterMode;
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
    public String newLabelText = "NEW";
    public int inventoryListWidth = 100;
    public boolean showInventoryLootList = true;
    /** 物品栏掉落列表相对默认贴合位置的额外 X 偏移 */
    public float inventoryListXOffset = 0.0f;
    /** 物品栏掉落列表相对背包顶部的额外 Y 偏移 */
    public float inventoryListYOffset = 0.0f;
    /** 物品栏掉落列表整体缩放倍率 (0.1 ~ 4.0) */
    public float inventoryListScale = 1.0f;
    /** 物品栏掉落列表独立透明度 (0.1 ~ 1.0) */
    public float inventoryListAlpha = 0.9f;
    /** 物品栏掉落列表面板像素高度 (默认 166, 约等于背包高度) */
    public int inventoryListHeight = 166;
    public boolean enableTooltipPreview = true;
    public boolean showKeyPrompt = true;
    public String overlaySkin = "vanilla";

    /** 掉落物上方数量文字显示模式: OFF(关闭) / ITEM_COUNT(物品数量) / STACK_COUNT(堆叠组数) */
    public DisplayMode itemCountDisplayMode = DisplayMode.ITEM_COUNT;

    /** 数量文字缩放倍率 (0.25 ~ 5.0, 默认 1.0) */
    public float itemCountScale = 1.0f;

    /** 数量文字最大渲染距离 (4 ~ 64, 默认 16) */
    public int itemCountRenderDistance = 16;

    /** 所有可用的悬浮窗物品行背景皮肤（对应 textures/overlay/&lt;skin&gt;/ 目录），供游戏内循环切换使用。 */
    public static final String[] AVAILABLE_OVERLAY_SKINS = { "vanilla", "stardew", "terraria" };

    // ==========================================
    // 快捷栏指示器悬浮窗设置 (Indicator Settings)
    // ==========================================
    public float indicatorX = -1.0f;
    public float indicatorY = -1.0f;
    public int indicatorRotation = 0;

    // ==========================================
    // 交互模式设置 (Interaction Modes)
    // ==========================================
    public AnimationSpeed animationSpeed = AnimationSpeed.MEDIUM;
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
    public PickupInterceptMode pickupInterceptMode = PickupInterceptMode.AUTO;
    public LongPressMode longPressMode = LongPressMode.PICKUP_ALL;
    public int stabilityThresholdTicks = 4;
    public boolean enableRareItemFilter = true;
    public boolean enableSuperMerge = true;
    public float mergeRangeXZ = 5.0f;
    public float mergeRangeY = 5.0f;
    /** 运输方块黑名单，逗号分隔的方块ID关键词。物品站在含有关键词的方块上时将跳过超大堆叠合并，避免干扰机械动力等模组的传送带/漏斗/溜槽运输逻辑。 */
    public String mergeTransportBlacklist = "belt,conveyor,chute,funnel,depot";
    public float pickupDelaySeconds = 1.0f;
    public int maxHoldTicks = 20;

    // ==========================================
    // 状态持久化设置 (Persistent State Settings)
    // ==========================================
    public FilterMode lastFilterMode = FilterMode.ALL;
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

    /**
     * 动画速度模式，控制悬浮窗动画的快慢
     */
    public enum AnimationSpeed {
        /** 慢速动画（0.5x） */
        SLOW,
        /** 中速动画（1.0x，默认） */
        MEDIUM,
        /** 快速动画（2.0x） */
        FAST,
        /** 关闭所有动画（瞬时切换） */
        OFF
    }

    /**
     * 掉落物上方数量文字显示模式
     */
    public enum DisplayMode {
        /** 关闭显示 */
        OFF,
        /** 显示物品总数量（K/M/B 缩写大数字） */
        ITEM_COUNT,
        /** 显示堆叠组数（如 3x） */
        STACK_COUNT
    }

    /**
     * 原版拾取事件拦截策略
     */
    public enum PickupInterceptMode {
        /** 智能模式：仅在其他模组未处理拾取事件时才拦截（推荐，兼容性最好） */
        AUTO,
        /** 始终拦截：无条件阻止原版拾取（可能与背包类模组冲突） */
        ALWAYS
    }

    /**
     * 长按拾取行为模式
     */
    public enum LongPressMode {
        /** 全部拾取：长按一键拾取范围内所有掉落物 */
        PICKUP_ALL,
        /** 单行全部拾取：长按一键拾取当前选中行同类物品的全部 */
        PICKUP_ROW
    }

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
        this.inventoryListWidth = Mth.clamp(this.inventoryListWidth, 80, 500);
        this.inventoryListXOffset = Mth.clamp(this.inventoryListXOffset, -2000.0f, 2000.0f);
        this.inventoryListYOffset = Mth.clamp(this.inventoryListYOffset, -2000.0f, 2000.0f);
        this.inventoryListScale = Mth.clamp(this.inventoryListScale, 0.1f, 4.0f);
        this.inventoryListAlpha = Mth.clamp(this.inventoryListAlpha, 0.1f, 1.0f);
        this.inventoryListHeight = Mth.clamp(this.inventoryListHeight, 40, 1000);
        this.visibleRows = Mth.clamp(this.visibleRows, 1.0f, 20.0f);
        this.globalAlpha = Mth.clamp(this.globalAlpha, 0.1f, 1.0f);
        this.lookDownAngle = Mth.clamp(this.lookDownAngle, 0.0f, 90.0f);
        this.scanRangeXZ = Mth.clamp(this.scanRangeXZ, 0.5f, 8.0f);
        this.scanRangeY = Mth.clamp(this.scanRangeY, 0.5f, 5.0f);
        this.pickupDelaySeconds = Mth.clamp(this.pickupDelaySeconds, 0.0f, 5.0f);
        this.maxHoldTicks = Mth.clamp(this.maxHoldTicks, 0, 100);

        if (this.customOverlayTitle == null) {
            this.customOverlayTitle = "Loot Detected";
        }

        if (this.newLabelText == null) {
            this.newLabelText = "NEW";
        }

        if (this.overlaySkin == null || this.overlaySkin.trim().isEmpty()) {
            this.overlaySkin = "vanilla";
        }

        this.itemCountScale = Mth.clamp(this.itemCountScale, 0.25f, 5.0f);
        this.itemCountRenderDistance = Mth.clamp(this.itemCountRenderDistance, 4, 64);
        if (this.itemCountDisplayMode == null) this.itemCountDisplayMode = DisplayMode.ITEM_COUNT;

        this.stabilityThresholdTicks = Mth.clamp(this.stabilityThresholdTicks, 0, 20);
        this.mergeRangeXZ = Mth.clamp(this.mergeRangeXZ, 0.0f, 10.0f);
        this.mergeRangeY = Mth.clamp(this.mergeRangeY, 0.0f, 10.0f);

        this.indicatorRotation = (this.indicatorRotation / 90 * 90) % 360;
        if (this.indicatorRotation < 0) this.indicatorRotation += 360;

        if (this.activationMode == null) this.activationMode = ActivationMode.ALWAYS;
        if (this.animationSpeed == null) this.animationSpeed = AnimationSpeed.MEDIUM;
        if (this.scrollMode == null) this.scrollMode = ScrollMode.ALWAYS;
        if (this.pickupInterceptMode == null) this.pickupInterceptMode = PickupInterceptMode.AUTO;
        if (this.longPressMode == null) this.longPressMode = LongPressMode.PICKUP_ALL;
        if (this.lastFilterMode == null) this.lastFilterMode = FilterMode.ALL;
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
            config.setComment("Visual.newLabelText", "新物品标签文本 (留空则隐藏, 默认 NEW)");
            config.set("Visual.newLabelText", INSTANCE.newLabelText);
            config.setComment("Visual.inventoryListWidth", "物品栏左侧掉落物列表面板宽度 (默认 100)");
            config.set("Visual.inventoryListWidth", INSTANCE.inventoryListWidth);
            config.setComment("Visual.inventoryListXOffset", "物品栏掉落列表相对默认贴合位置的额外 X 偏移");
            config.set("Visual.inventoryListXOffset", INSTANCE.inventoryListXOffset);
            config.setComment("Visual.inventoryListYOffset", "物品栏掉落列表相对背包顶部的额外 Y 偏移");
            config.set("Visual.inventoryListYOffset", INSTANCE.inventoryListYOffset);
            config.setComment("Visual.inventoryListScale", "物品栏掉落列表整体缩放倍率 (0.1 ~ 4.0, 默认 1.0)");
            config.set("Visual.inventoryListScale", INSTANCE.inventoryListScale);
            config.setComment("Visual.inventoryListAlpha", "物品栏掉落列表独立透明度 (0.1 ~ 1.0, 默认 0.9)");
            config.set("Visual.inventoryListAlpha", INSTANCE.inventoryListAlpha);
            config.setComment("Visual.inventoryListHeight", "物品栏掉落列表面板像素高度 (默认 166)");
            config.set("Visual.inventoryListHeight", INSTANCE.inventoryListHeight);
            config.setComment("Visual.showInventoryLootList", "是否在物品栏左侧显示掉落物列表");
            config.set("Visual.showInventoryLootList", INSTANCE.showInventoryLootList);
            config.setComment("Visual.enableTooltipPreview", "是否在悬浮窗选中物品时显示物品信息预览");
            config.set("Visual.enableTooltipPreview", INSTANCE.enableTooltipPreview);
            config.setComment("Visual.showKeyPrompt", "是否在悬浮窗选中物品时显示左侧按键提示框");
            config.set("Visual.showKeyPrompt", INSTANCE.showKeyPrompt);
            config.setComment("Visual.overlaySkin", "悬浮窗物品行背景皮肤: vanilla(原版) / stardew(星露谷风格) / terraria(泰拉瑞亚风格)。对应 textures/overlay/<skin>/ 目录, 默认 vanilla");
            config.set("Visual.overlaySkin", INSTANCE.overlaySkin);
            config.setComment("Visual.itemCountDisplayMode", "掉落物上方数量文字显示模式: OFF(关闭) / ITEM_COUNT(物品数量) / STACK_COUNT(堆叠组数), 默认 ITEM_COUNT");
            config.set("Visual.itemCountDisplayMode", INSTANCE.itemCountDisplayMode.name());
            config.setComment("Visual.itemCountScale", "数量文字缩放倍率 (0.25 ~ 5.0, 默认 1.0)");
            config.set("Visual.itemCountScale", INSTANCE.itemCountScale);
            config.setComment("Visual.itemCountRenderDistance", "数量文字最大渲染距离 (4 ~ 64, 默认 16)");
            config.set("Visual.itemCountRenderDistance", INSTANCE.itemCountRenderDistance);

            // --- 指示器设置 ---
            config.setComment("Indicator", "快捷栏指示器悬浮窗设置 (Indicator Settings)");
            config.setComment("Indicator.indicatorX", "-1 代表使用默认贴合快捷栏的位置(建议游戏内具体调整)");
            config.set("Indicator.indicatorX", INSTANCE.indicatorX);
            config.set("Indicator.indicatorY", INSTANCE.indicatorY);
            config.setComment("Indicator.indicatorRotation", "支持 0, 90, 180, 270");
            config.set("Indicator.indicatorRotation", INSTANCE.indicatorRotation);

            // --- 交互模式 ---
            config.setComment("Interaction", "交互模式设置 (Interaction Modes)");
            config.setComment("Interaction.animationSpeed", "动画速度: SLOW(慢) / MEDIUM(中,默认) / FAST(快) / OFF(关闭)");
            config.set("Interaction.animationSpeed", INSTANCE.animationSpeed.name());
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
            config.setComment("Core.pickupInterceptMode", "拾取拦截策略: AUTO(智能,推荐) / ALWAYS(始终拦截)");
            config.set("Core.pickupInterceptMode", INSTANCE.pickupInterceptMode.name());
            config.setComment("Core.longPressMode", "长按拾取模式: PICKUP_ALL(全部拾取,默认) / PICKUP_ROW(单行全部拾取)");
            config.set("Core.longPressMode", INSTANCE.longPressMode.name());
            config.setComment("Core.stabilityThresholdTicks", "物品必须连续存在多少 tick 才在悬浮窗显示（默认4=0.2秒，0=关闭）");
            config.set("Core.stabilityThresholdTicks", INSTANCE.stabilityThresholdTicks);
            config.setComment("Core.enableRareItemFilter", "白名单是否启用默认稀有物品过滤。关闭后仅显示白名单内的物品");
            config.set("Core.enableRareItemFilter", INSTANCE.enableRareItemFilter);
            config.setComment("Core.enableSuperMerge", "是否开启掉落物超大堆叠合并");
            config.set("Core.enableSuperMerge", INSTANCE.enableSuperMerge);
            config.setComment("Core.mergeRangeXZ", "水平合并范围 (最大 10)");
            config.set("Core.mergeRangeXZ", INSTANCE.mergeRangeXZ);
            config.setComment("Core.mergeRangeY", "垂直合并范围 (最大 10)");
            config.set("Core.mergeRangeY", INSTANCE.mergeRangeY);
            config.setComment("Core.mergeTransportBlacklist", "运输方块黑名单，用逗号分隔方块ID关键词。物品站在匹配的方块上将跳过超大堆叠合并，避免干扰传送带等运输模组。默认: belt,conveyor,chute,funnel,depot");
            config.set("Core.mergeTransportBlacklist", INSTANCE.mergeTransportBlacklist);
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
            INSTANCE.newLabelText = config.getOrElse("Visual.newLabelText", "NEW");
            INSTANCE.inventoryListWidth = config.getOrElse("Visual.inventoryListWidth", 100);
            INSTANCE.inventoryListXOffset = config.<Number>getOrElse("Visual.inventoryListXOffset", 0.0f).floatValue();
            INSTANCE.inventoryListYOffset = config.<Number>getOrElse("Visual.inventoryListYOffset", 0.0f).floatValue();
            INSTANCE.inventoryListScale = config.<Number>getOrElse("Visual.inventoryListScale", 1.0f).floatValue();
            INSTANCE.inventoryListAlpha = config.<Number>getOrElse("Visual.inventoryListAlpha", 0.9f).floatValue();
            INSTANCE.inventoryListHeight = config.getOrElse("Visual.inventoryListHeight", 166);
            INSTANCE.showInventoryLootList = config.getOrElse("Visual.showInventoryLootList", true);
            INSTANCE.enableTooltipPreview = config.getOrElse("Visual.enableTooltipPreview", true);
            INSTANCE.showKeyPrompt = config.getOrElse("Visual.showKeyPrompt", true);
            INSTANCE.overlaySkin = config.getOrElse("Visual.overlaySkin", "vanilla");
            try { INSTANCE.itemCountDisplayMode = DisplayMode.valueOf(config.getOrElse("Visual.itemCountDisplayMode", "ITEM_COUNT")); } catch (Exception ignored) {}
            INSTANCE.itemCountScale = config.<Number>getOrElse("Visual.itemCountScale", 1.0f).floatValue();
            INSTANCE.itemCountRenderDistance = config.getOrElse("Visual.itemCountRenderDistance", 16);

            INSTANCE.indicatorX = config.<Number>getOrElse("Indicator.indicatorX", -1.0f).floatValue();
            INSTANCE.indicatorY = config.<Number>getOrElse("Indicator.indicatorY", -1.0f).floatValue();
            INSTANCE.indicatorRotation = config.getOrElse("Indicator.indicatorRotation", 0);

            try { INSTANCE.animationSpeed = AnimationSpeed.valueOf(config.getOrElse("Interaction.animationSpeed", "MEDIUM")); } catch (Exception ignored) {}
            try { INSTANCE.activationMode = ActivationMode.valueOf(config.getOrElse("Interaction.activationMode", "ALWAYS")); } catch (Exception ignored) {}
            try { INSTANCE.scrollMode = ScrollMode.valueOf(config.getOrElse("Interaction.scrollMode", "ALWAYS")); } catch (Exception ignored) {}

            INSTANCE.lookDownAngle = config.<Number>getOrElse("Scanning.lookDownAngle", 45.0f).floatValue();
            INSTANCE.scanRangeXZ = config.<Number>getOrElse("Scanning.scanRangeXZ", 1.0f).floatValue();
            INSTANCE.scanRangeY = config.<Number>getOrElse("Scanning.scanRangeY", 1.0f).floatValue();

            try { INSTANCE.pickupInterceptMode = PickupInterceptMode.valueOf(config.getOrElse("Core.pickupInterceptMode", "AUTO")); } catch (Exception ignored) {}
            try { INSTANCE.longPressMode = LongPressMode.valueOf(config.getOrElse("Core.longPressMode", "PICKUP_ALL")); } catch (Exception ignored) {}
            INSTANCE.stabilityThresholdTicks = config.getOrElse("Core.stabilityThresholdTicks", 4);
            INSTANCE.enableRareItemFilter = config.getOrElse("Core.enableRareItemFilter", true);
            INSTANCE.enableSuperMerge = config.getOrElse("Core.enableSuperMerge", true);
            INSTANCE.mergeRangeXZ = config.<Number>getOrElse("Core.mergeRangeXZ", 5.0f).floatValue();
            INSTANCE.mergeRangeY = config.<Number>getOrElse("Core.mergeRangeY", 5.0f).floatValue();
            INSTANCE.mergeTransportBlacklist = config.getOrElse("Core.mergeTransportBlacklist", "belt,conveyor,chute,funnel,depot");
            INSTANCE.pickupDelaySeconds = config.<Number>getOrElse("Core.pickupDelaySeconds", 1.0f).floatValue();
            INSTANCE.maxHoldTicks = config.getOrElse("Core.maxHoldTicks", 20);

            try { INSTANCE.lastFilterMode = FilterMode.valueOf(config.getOrElse("State.lastFilterMode", "ALL")); } catch (Exception ignored) {}
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