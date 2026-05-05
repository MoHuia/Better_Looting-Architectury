package com.mohuia.better_looting.network.S2C;

import com.mohuia.better_looting.config.BetterLootingConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * 服务端下发给客户端的同步配置数据包 (Server -> Client)
 */
public class PacketSyncConfig {
    private final float scanRangeXZ;
    private final float scanRangeY;

    // 构造器 (发送端使用)
    public PacketSyncConfig(float scanRangeXZ, float scanRangeY) {
        this.scanRangeXZ = scanRangeXZ;
        this.scanRangeY = scanRangeY;
    }

    // 反序列化构造器 (接收端使用)
    public PacketSyncConfig(FriendlyByteBuf buf) {
        this.scanRangeXZ = buf.readFloat();
        this.scanRangeY = buf.readFloat();
    }

    // 序列化
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.scanRangeXZ);
        buf.writeFloat(this.scanRangeY);
    }

    // 数据包处理逻辑 (在客户端执行)
    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        // 将任务推送到主线程执行策安全操作
        context.queue(() -> {
            BetterLootingConfig.get().serverScanRangeXZ = this.scanRangeXZ;
            BetterLootingConfig.get().serverScanRangeY = this.scanRangeY;
        });
    }
}