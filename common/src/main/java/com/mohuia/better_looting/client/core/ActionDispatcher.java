package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.network.NetworkHandler;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

public class ActionDispatcher {

    public static void sendSinglePickup(SelectionManager selection) {
        List<VisualItemEntry> items = selection.getNearbyItems();
        int index = selection.getSelectedIndex();

        if (index >= 0 && index < items.size()) {
            VisualItemEntry entry = items.get(index);
            List<Integer> ids = new ArrayList<>();
            entry.getSourceEntities().forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });

            if (!ids.isEmpty()) {
                NetworkHandler.sendToServer(new PacketBatchPickup(ids, false, true));
            }
        }
    }

    public static void sendBatchPickup(List<ItemEntity> entities, boolean isAuto) {
        List<Integer> ids = new ArrayList<>();
        entities.forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });
        if (!ids.isEmpty()) {
            NetworkHandler.sendToServer(new PacketBatchPickup(ids, isAuto, false));
        }
    }

    public static void handleAutoPickup(SelectionManager selection, PickupHandler pickupHandler) {
        List<ItemEntity> targets = new ArrayList<>();
        selection.getNearbyItems().forEach(e ->
                e.getSourceEntities().forEach(en -> { if(en.isAlive()) targets.add(en); })
        );

        if (!targets.isEmpty()) {
            sendBatchPickup(targets, true);
            pickupHandler.onAutoPickupTriggered();
        }
    }
}