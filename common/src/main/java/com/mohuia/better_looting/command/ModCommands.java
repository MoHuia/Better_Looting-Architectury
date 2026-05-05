package com.mohuia.better_looting.command;

import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mohuia.better_looting.network.NetworkHandler;
import com.mohuia.better_looting.network.S2C.PacketSyncConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 专门用于注册模组自定义指令的类
 */
public class ModCommands {

    public static void register() {
        // 注册：管理员指令 /bl range <xz> <y>
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) -> {
            dispatcher.register(Commands.literal("bl")
                    .requires(source -> source.hasPermission(2)) // 需要OP权限
                    .then(Commands.literal("range")
                            .then(Commands.argument("xz", FloatArgumentType.floatArg(0.5f, 8.0f))
                                    .then(Commands.argument("y", FloatArgumentType.floatArg(0.5f, 5.0f))
                                            .executes(context -> {
                                                float newXZ = FloatArgumentType.getFloat(context, "xz");
                                                float newY = FloatArgumentType.getFloat(context, "y");

                                                // 保存到服务端的配置中
                                                BetterLootingConfig config = BetterLootingConfig.get();
                                                config.scanRangeXZ = newXZ;
                                                config.scanRangeY = newY;
                                                BetterLootingConfig.save();

                                                // 组装数据包并全服广播
                                                PacketSyncConfig packet = new PacketSyncConfig(newXZ, newY);
                                                NetworkHandler.INSTANCE.sendToPlayers(
                                                        context.getSource().getServer().getPlayerList().getPlayers(),
                                                        packet
                                                );

                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("§a[BetterLooting] 已将拾取范围全局设置为 XZ: " + newXZ + ", Y: " + newY),
                                                        true
                                                );
                                                return 1;
                                            }))))
            );
        });
    }
}