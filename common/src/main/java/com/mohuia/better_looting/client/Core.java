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

public class Core {
    public static final Core INSTANCE = new Core();
    public enum FilterMode { ALL, RARE_ONLY }

    private final PickupHandler pickupHandler = new PickupHandler();
    private final SelectionManager selectionManager = new SelectionManager();
    private final KeyTracker keyTracker = new KeyTracker();

    private FilterMode filterMode = FilterMode.ALL;
    private boolean isAutoMode = false;

    private Core() {}

    public void init() {
        FilterWhitelist.INSTANCE.init();
        ClientTickEvent.CLIENT_POST.register(this::onClientTick);
    }

    public boolean isHudActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || selectionManager.getNearbyItems().isEmpty()) return false;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        return switch (cfg.activationMode) {
            case ALWAYS -> true;
            case LOOK_DOWN -> mc.player.getXRot() >= cfg.lookDownAngle;
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.001;
            case KEY_HOLD -> KeyInit.SHOW_OVERLAY.isDown();
            case KEY_TOGGLE -> keyTracker.isOverlayToggleActive();
        };
    }

    public boolean shouldIgnoreScroll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && !(mc.screen instanceof ConfigScreen)) return true;
        if (!isHudActive()) return true;
        if (selectionManager.getNearbyItems().size() <= 1) return true;

        BetterLootingConfig cfg = BetterLootingConfig.get();
        return switch (cfg.scrollMode) {
            case ALWAYS -> false;
            case KEY_BIND -> !KeyInit.SCROLL_MODIFIER.isDown();
            case STAND_STILL -> mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        };
    }

    public void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        keyTracker.tickOverlayToggle();
        selectionManager.updateItems(LootScanner.scan(mc, this.filterMode));

        if (isAutoMode && isHudActive()) {
            if (pickupHandler.canAutoPickup()) {
                ActionDispatcher.handleAutoPickup(selectionManager, pickupHandler);
            }
        } else {
            pickupHandler.resetAutoCooldown();
        }

        handleInputLogic();
    }

    private void handleInputLogic() {
        keyTracker.tickActionToggles(this::toggleFilterMode, this::toggleAutoMode);

        boolean isKeyDown = keyTracker.isPhysicalKeyDown(KeyInit.PICKUP);
        boolean hasTargets = isHudActive();

        var action = pickupHandler.tickInput(isKeyDown, hasTargets);

        switch (action) {
            case SINGLE -> ActionDispatcher.sendSinglePickup(selectionManager);
            case BATCH -> {
                List<ItemEntity> all = new ArrayList<>();
                selectionManager.getNearbyItems().forEach(e -> all.addAll(e.getSourceEntities()));
                ActionDispatcher.sendBatchPickup(all, false);
            }
        }
    }

    public void performScroll(double delta) {
        selectionManager.performScroll(delta);
    }

    public void toggleFilterMode() {
        filterMode = (filterMode == FilterMode.ALL) ? FilterMode.RARE_ONLY : FilterMode.ALL;
    }

    public void toggleAutoMode() {
        isAutoMode = !isAutoMode;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Component msg = isAutoMode
                    ? Component.translatable("message.better_looting.auto_on").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.better_looting.auto_off").withStyle(ChatFormatting.RED);
            mc.player.displayClientMessage(msg, true);
        }
    }

    public FilterMode getFilterMode() { return filterMode; }
    public boolean isAutoMode() { return isAutoMode; }
    public List<VisualItemEntry> getNearbyItems() { return selectionManager.getNearbyItems(); }
    public int getSelectedIndex() { return selectionManager.getSelectedIndex(); }
    public int getTargetScrollOffset() { return selectionManager.getTargetScrollOffset(); }
    public float getPickupProgress() { return pickupHandler.getProgress(); }

    public boolean isItemInInventory(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (var stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) return true;
        }
        return false;
    }

    public static boolean shouldIntercept() {
        return INSTANCE.isHudActive() || INSTANCE.pickupHandler.isInteracting();
    }
}