package com.mohuia.better_looting.client;

import com.mohuia.better_looting.client.core.*;
import com.mohuia.better_looting.client.filter.FilterWhitelist;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.config.ConfigScreen;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端核心类，采用单例模式。
 * 负责串联按键输入、掉落物扫描、UI交互状态以及自动拾取逻辑。
 */
public class Core {
    public static final Core INSTANCE = new Core();

    /** 过滤器模式：扫描所有物品 vs 仅扫描稀有物品 */
    public enum FilterMode { ALL, RARE_ONLY }

    private final PickupHandler pickupHandler = new PickupHandler();
    private final SelectionManager selectionManager = new SelectionManager();
    private final KeyTracker keyTracker = new KeyTracker();

    private FilterMode filterMode = FilterMode.ALL;
    private boolean isAutoMode = false;

    // 按键拦截的缓冲保护时间
    private int interceptGraceTicks = 0;

    // 记录当前是否正处于被拦截的长按状态中（解决长按时物品消失导致的按键穿透）
    private boolean isHoldingInterceptedKey = false;

    private Core() {}

    public void init() {
        FilterWhitelist.INSTANCE.init();

        // 初始化时从配置读取持久化的状态
        BetterLootingConfig cfg = BetterLootingConfig.get();
        this.filterMode = cfg.lastFilterMode;
        this.isAutoMode = cfg.lastAutoMode;

        // 注册客户端 Tick 事件，确保在游戏非暂停状态下处理逻辑
        // 已修改为 CLIENT_PRE，以便在原版按键处理前抢先执行
        ClientTickEvent.CLIENT_PRE.register(this::onClientTick);
    }

    /**
     * 判断当前是否应该在屏幕上渲染拾取 HUD。
     * 结合了玩家状态、周围物品数量以及玩家在配置中选择的激活模式。
     */
    public boolean isHudActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || selectionManager.getNearbyItems().isEmpty()) return false;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        // 根据配置的激活模式判断是否显示 HUD
        return switch (cfg.activationMode) {
            case ALWAYS -> true;
            case LOOK_DOWN -> mc.player.getXRot() >= cfg.lookDownAngle; // 视角向下看
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001; // 几乎静止状态
            case KEY_HOLD -> KeyInit.SHOW_OVERLAY.isDown(); // 按住特定键
            case KEY_TOGGLE -> keyTracker.isOverlayToggleActive(); // 切换键模式
        };
    }

    /**
     * 判断是否应该忽略鼠标滚轮事件（防止与原版快捷栏切换冲突）。
     * @return true 表示忽略模组滚动（将控制权交还给原版快捷栏），false 表示模组拦截滚动。
     */
    public boolean shouldIgnoreScroll() {
        Minecraft mc = Minecraft.getInstance();
        // 1. 基础条件拦截：打开了其他界面、HUD未激活、或物品不足以滚动时，直接交还给原版
        if (mc.screen != null && !(mc.screen instanceof ConfigScreen)) return true;
        if (!isHudActive()) return true;
        if (selectionManager.getNearbyItems().size() <= 1) return true;

        // 2. 根据玩家配置的模式，决定是否将滚轮控制权交还给原版快捷栏
        BetterLootingConfig cfg = BetterLootingConfig.get();
        return switch (cfg.scrollMode) {
            case ALWAYS -> false; // 绝对占用：模组始终接管滚轮
            case INVERT_KEY -> KeyInit.SCROLL_MODIFIER.isDown(); // 新增(按键反转)：按下快捷键时交还给原版，松开时模组接管
            case KEY_BIND -> !KeyInit.SCROLL_MODIFIER.isDown(); // 原有(按键绑定)：没按快捷键时交还给原版，按下时模组接管
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.001; // 移动时交还给原版
        };
    }

    public void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        // 必须先扫描掉落物！确保后续 isHudActive() 判定时，拿到的是当前帧最准确的环境状态
        selectionManager.updateItems(LootScanner.scan(mc, this.filterMode));

        // 获取当前物理按键的状态
        boolean isPhysicalDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP);

        // 一旦玩家松开按键，立刻解除长按锁定
        if (!isPhysicalDown) {
            isHoldingInterceptedKey = false;
        }

        // 只要当前满足拦截条件，或者正处于已被锁定的长按状态中
        if (shouldIntercept() || isHoldingInterceptedKey) {
            // 无条件持续排空原版冲突按键！不再受 isPhysicalDown 限制。
            // （防止玩家疯狂连按时，物理按键已松开，但底层 clickCount 仍有残留引发穿透）
            suppressVanillaOverlappingKeys(mc);

            // 仅在此处做长按锁定
            if (isPhysicalDown) {
                isHoldingInterceptedKey = true; // 直到松手才解除
            }
        }

        // 递减拦截缓冲计时器
        if (interceptGraceTicks > 0) {
            interceptGraceTicks--;
        }

        // 更新按键切换状态
        keyTracker.tickOverlayToggle();

        // 处理自动拾取逻辑
        if (isAutoMode && isHudActive()) {
            if (pickupHandler.canAutoPickup()) {
                ActionDispatcher.handleAutoPickup(selectionManager, pickupHandler);
            }
        } else {
            pickupHandler.resetAutoCooldown();
        }

        handleInputLogic();
    }

    /**
     * 处理玩家的主动按键输入（单次拾取或批量拾取）。
     */
    private void handleInputLogic() {
        keyTracker.tickActionToggles(this::toggleFilterMode, this::toggleAutoMode);

        boolean isKeyDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP);
        boolean hasTargets = isHudActive();

        // 获取当前输入对应的动作类型
        var action = pickupHandler.tickInput(isKeyDown, hasTargets);
        int delayTicks = (int) (BetterLootingConfig.get().pickupDelaySeconds * 20);

        switch (action) {
            case SINGLE -> {
                ActionDispatcher.sendSinglePickup(selectionManager);
                interceptGraceTicks = delayTicks; // 给予 10 Tick (1秒) 的延迟保护
            }
            case BATCH -> {
                List<ItemEntity> all = new ArrayList<>();
                // 收集附近所有可拾取实体的引用
                selectionManager.getNearbyItems().forEach(e -> all.addAll(e.getSourceEntities()));
                ActionDispatcher.sendBatchPickup(all, false);
                interceptGraceTicks = delayTicks; // 给予 10 Tick (1秒) 的延迟保护
            }
        }
    }

    public void performScroll(double delta) {
        selectionManager.performScroll(delta);
    }

    public void toggleFilterMode() {
        // 1. 切换模式
        filterMode = (filterMode == FilterMode.ALL) ? FilterMode.RARE_ONLY : FilterMode.ALL;

        // 同步状态到配置并持久化保存
        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.lastFilterMode = this.filterMode;
        BetterLootingConfig.save();

        // 2. 发送快捷栏上方提示
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 获取当前模式的本地化名称，并设为黄色高亮
            Component modeName = Component.translatable("gui.better_looting.config.mode." + filterMode.name().toLowerCase())
                    .withStyle(ChatFormatting.YELLOW);

            // 组合完整的提示信息
            Component msg = Component.translatable("message.better_looting.filter_switched", modeName);

            // 发送到 Action Bar (第二个参数为 true)
            mc.player.displayClientMessage(msg, true);
        }
    }

    public void toggleAutoMode() {
        isAutoMode = !isAutoMode;

        // 同步状态到配置并持久化保存
        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.lastAutoMode = this.isAutoMode;
        BetterLootingConfig.save();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 在玩家屏幕上显示自动拾取模式的开关提示（带颜色格式化）
            Component msg = isAutoMode
                    ? Component.translatable("message.better_looting.auto_on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.better_looting.auto_off").withStyle(ChatFormatting.RED);
            mc.player.displayClientMessage(msg, true);
        }
    }

    // --- Getter 方法，供渲染层调用 ---
    public FilterMode getFilterMode() { return filterMode; }
    public boolean isAutoMode() { return isAutoMode; }
    public List<VisualItemEntry> getNearbyItems() { return selectionManager.getNearbyItems(); }
    public int getSelectedIndex() { return selectionManager.getSelectedIndex(); }
    public int getTargetScrollOffset() { return selectionManager.getTargetScrollOffset(); }
    public float getPickupProgress() { return pickupHandler.getProgress(); }

    /**
     * 检查某个物品是否已经存在于玩家背包中，可用于高亮显示或过滤。
     * @param item 需要检查的物品类型
     * @return 存在任意数量返回 true，否则返回 false。
     */
    public boolean isItemInInventory(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (var stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) return true;
        }
        return false;
    }

    /**
     * 静态辅助方法：判断模组是否应该拦截特定的游戏交互（例如防止在拾取时错误攻击/右键）
     */
    public static boolean shouldIntercept() {
        // interceptGraceTicks > 0 判定
        return INSTANCE.isHudActive() || INSTANCE.pickupHandler.isInteracting() || INSTANCE.interceptGraceTicks > 0;
    }

    /**
     * 动态检测并消耗掉与拾取键重合的原版按键事件，防止“按键穿透”
     */
    private void suppressVanillaOverlappingKeys(Minecraft mc) {
        // 如果拾取键和副手交换键（默认 F）绑定了同一个物理按键
        if (KeyInit.PICKUP.same(mc.options.keySwapOffhand)) {
            // 循环消耗掉原版的点击次数，这样原版的 handleKeybinds 就不会触发副手交换了
            while (mc.options.keySwapOffhand.consumeClick()) {}
        }

        // 防患于未然：如果玩家把拾取键绑定成了丢弃键（默认 Q）
        if (KeyInit.PICKUP.same(mc.options.keyDrop)) {
            while (mc.options.keyDrop.consumeClick()) {}
        }
    }
}