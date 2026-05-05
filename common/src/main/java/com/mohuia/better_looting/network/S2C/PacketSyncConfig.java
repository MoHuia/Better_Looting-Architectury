package com.mohuia.better_looting.network.S2C;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.config.BetterLootingConfig;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端下发给客户端的同步配置数据包 (Server -> Client)
 */
public class PacketSyncConfig implements CustomPacketPayload {

    public static final Type<PacketSyncConfig> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, PacketSyncConfig> CODEC = StreamCodec.of(
            (buf, packet) -> packet.toBytes(buf),
            PacketSyncConfig::new
    );

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 数据包处理逻辑 (在客户端执行)
    public void handle(NetworkManager.PacketContext context) {
        // 将任务推送到主线程执行，确保线程安全
        context.queue(() -> {
            BetterLootingConfig.get().serverScanRangeXZ = this.scanRangeXZ;
            BetterLootingConfig.get().serverScanRangeY = this.scanRangeY;
        });
    }
}