package com.mohuia.better_looting.network;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

/**
 * 基于 Architectury API 的网络数据包注册中心
 * 负责在客户端和服务端之间建立通信频道
 */
public class NetworkHandler {
    // 创建模组专属的主网络通道
    public static final NetworkChannel INSTANCE = NetworkChannel.create(
            new ResourceLocation(BetterLooting.MODID, "main")
    );

    /**
     * 注册所有自定义的网络数据包
     * 必须在模组初始化阶段调用
     */
    public static void register() {
        // 注册批量拾取数据包
        INSTANCE.register(PacketBatchPickup.class,
                PacketBatchPickup::toBytes,      // 序列化
                PacketBatchPickup::new,         // 反序列化
                PacketBatchPickup::handle);     // 处理器
    }

    /**
     * 便捷方法：从客户端向服务端发送数据包
     */
    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }
}