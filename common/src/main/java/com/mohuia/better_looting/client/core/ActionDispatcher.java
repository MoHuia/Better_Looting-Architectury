package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.network.NetworkHandler;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 动作分发器，专门负责将客户端的拾取操作封装为网络包发送给服务端。
 * 将网络通信逻辑与核心业务逻辑解耦。
 */
public class ActionDispatcher {

    /**
     * 发送单体拾取请求，仅拾取当前选中列表项对应的实体。
     * @param selection 选区管理器实例
     */
    public static void sendSinglePickup(SelectionManager selection) {
        List<VisualItemEntry> items = selection.getNearbyItems();
        int index = selection.getSelectedIndex();

        if (index >= 0 && index < items.size()) {
            VisualItemEntry entry = items.get(index);
            List<Integer> ids = new ArrayList<>();
            // 过滤死亡实体，确保服务器只处理存活的 ItemEntity，避免发包冗余
            entry.getSourceEntities().forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });

            if (!ids.isEmpty()) {
                NetworkHandler.sendToServer(new PacketBatchPickup(ids, false, true));
            }
        }
    }

    /**
     * 发送批量拾取请求，拾取传入列表中的所有实体。
     * @param entities  需要拾取的掉落物实体列表
     * @param isAuto    是否是由自动模式触发的拾取
     */
    public static void sendBatchPickup(List<ItemEntity> entities, boolean isAuto) {
        List<Integer> ids = new ArrayList<>();
        entities.forEach(e -> { if(e.isAlive()) ids.add(e.getId()); });
        if (!ids.isEmpty()) {
            NetworkHandler.sendToServer(new PacketBatchPickup(ids, isAuto, false));
        }
    }

    /**
     * 处理自动拾取触发时的发包，并重置自动拾取冷却。
     * @param selection     选区管理器实例
     * @param pickupHandler 拾取处理器实例
     */
    public static void handleAutoPickup(SelectionManager selection, PickupHandler pickupHandler) {
        List<ItemEntity> targets = new ArrayList<>();
        selection.getNearbyItems().forEach(e ->
                e.getSourceEntities().forEach(en -> { if(en.isAlive()) targets.add(en); })
        );

        if (!targets.isEmpty()) {
            sendBatchPickup(targets, true);
            // 通知 PickupHandler 已经触发了自动拾取（通常用于重新计算冷却）
            pickupHandler.onAutoPickupTriggered();
        }
    }
}