package com.mohuia.better_looting.network.C2S;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.mixin.ItemEntityAccessor;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端到服务端 (C2S) 的批量拾取数据包
 * 用于通知服务端玩家尝试拾取了一批掉落物，并处理“超大堆叠”掉落物的背包塞入逻辑。
 */
public record PacketBatchPickup(List<Integer> entityIds, boolean isAuto, boolean limitToMaxStack) implements CustomPacketPayload {

    public static final Type<PacketBatchPickup> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "batch_pickup")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBatchPickup> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.isAuto());
                buf.writeBoolean(packet.limitToMaxStack());
                buf.writeVarInt(packet.entityIds().size());
                packet.entityIds().forEach(buf::writeInt);
            },
            buf -> {
                boolean isAuto = buf.readBoolean();
                boolean limitToMaxStack = buf.readBoolean();
                int count = buf.readVarInt();
                List<Integer> entityIds = new ArrayList<>(count);
                for (int i = 0; i < count; i++) entityIds.add(buf.readInt());
                return new PacketBatchPickup(entityIds, isAuto, limitToMaxStack);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 核心处理逻辑 (服务端执行)
     * 完全仿造 1.20.1 的逻辑，保留单次直接插入的写法。
     */
    public void handle(NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            // 基础校验：玩家必须在线且存活
            if (player == null || !player.isAlive()) return;

            // 如果启用了限制，则本次网络包最多只能拾取 64 个物品，否则无限制
            int remainingQuota = this.limitToMaxStack() ? 64 : Integer.MAX_VALUE;
            boolean anySuccess = false;   // 标记本次请求是否成功拾取了至少一个物品
            boolean inventoryFull = false; // 标记玩家背包是否已满

            // 遍历客户端发来的实体 ID 列表
            for (int id : this.entityIds()) {
                // 如果额度用尽或背包已满，直接终止后续处理，节省性能
                if (remainingQuota <= 0 || inventoryFull) break;

                Entity target = player.level().getEntity(id);

                // --- 1. 安全与防作弊校验 ---
                if (!(target instanceof ItemEntity item) || !item.isAlive() || player.distanceToSqr(target) >= 100.0) continue;
                if (this.isAuto() && ((ItemEntityAccessor) item).getPickupDelay() > 0) continue;

                ItemStack stack = item.getItem();
                ISuperStack superStack = (ISuperStack) item;

                // --- 2. 数量计算 ---
                int totalAvailable = stack.getCount() + superStack.betterlooting$getExtraCount();
                int toTake = Math.min(totalAvailable, remainingQuota);
                if (toTake <= 0) continue;

                // --- 3. 模拟与插入背包 (1.20.1 原版逻辑) ---
                ItemStack insertStack = stack.copy();
                insertStack.setCount(toTake);

                // 直接塞入
                player.getInventory().add(insertStack);

                int remainderCount = insertStack.getCount();    // 剩下的数量
                int actuallyTaken = toTake - remainderCount;    // 实际成功塞入的数量

                if (remainderCount > 0) {
                    inventoryFull = true;
                }

                // --- 4. 成功拾取后的状态更新 ---
                if (actuallyTaken > 0) {
                    anySuccess = true;
                    remainingQuota -= actuallyTaken;
                    int remainingAfterTake = totalAvailable - actuallyTaken;

                    int animAmount = remainingAfterTake > 0
                            ? Math.min(actuallyTaken, Math.max(1, stack.getCount() - 1))
                            : actuallyTaken;

                    player.take(item, animAmount);
                    player.awardStat(net.minecraft.stats.Stats.ITEM_PICKED_UP.get(stack.getItem()), actuallyTaken);

                    // --- 5. 更新地面上的掉落物实体 ---
                    if (remainingAfterTake <= 0) {
                        item.discard();
                    } else {
                        int newBaseCount = Math.min(remainingAfterTake, stack.getMaxStackSize());
                        stack.setCount(newBaseCount);
                        item.setItem(stack.copy());
                        superStack.betterlooting$setExtraCount(remainingAfterTake - newBaseCount);
                    }
                }
            }

            // --- 6. 统一反馈 ---
            if (anySuccess) {
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);
            }
            if (inventoryFull && !this.isAuto()) {
                player.displayClientMessage(Component.translatable("message.better_looting.inventory_full").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}