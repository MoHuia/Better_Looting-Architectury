package com.mohuia.better_looting.network;

import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import com.mohuia.better_looting.network.S2C.PacketSyncConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 网络数据包注册中心
 * 负责在客户端和服务端之间建立通信频道
 */
public class NetworkHandler {

    /**
     * 注册所有自定义的网络数据包
     * 必须在模组初始化阶段调用
     */
    public static void register() {
        // 注册客户端向服务端发送的：批量拾取数据包 (C2S)
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,     // 方向：Client to Server
                PacketBatchPickup.TYPE,      // 数据包的全局唯一标识
                PacketBatchPickup.CODEC,     // 编解码器
                PacketBatchPickup::handle    // 处理逻辑
        );

        // 注册服务端向客户端同步的：配置更新数据包 (S2C)
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,     // 方向：Server to Client
                PacketSyncConfig.TYPE,       // 数据包的全局唯一标识
                PacketSyncConfig.CODEC,      // 编解码器
                PacketSyncConfig::handle     // 处理逻辑
        );
    }

    /**
     * 便捷方法：从客户端向服务端发送数据包
     */
    public static void sendToServer(CustomPacketPayload msg) {
        // 直接调用 NetworkManager 静态方法发送
        NetworkManager.sendToServer(msg);
    }
}