package com.mohuia.better_looting.network;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.network.C2S.PacketBatchPickup;
import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

public class NetworkHandler {
    public static final NetworkChannel INSTANCE = NetworkChannel.create(
            new ResourceLocation(BetterLooting.MODID, "main")
    );

    public static void register() {
        INSTANCE.register(PacketBatchPickup.class,
                PacketBatchPickup::toBytes,
                PacketBatchPickup::new,
                PacketBatchPickup::handle);
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }
}