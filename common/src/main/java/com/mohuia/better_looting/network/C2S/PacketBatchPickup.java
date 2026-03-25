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
// 【修改】改为 record 并实现 CustomPacketPayload 接口
public record PacketBatchPickup(List<Integer> entityIds, boolean isAuto, boolean limitToMaxStack) implements CustomPacketPayload {

    // 【新增】1.21 的 Payload Type 标识
    public static final Type<PacketBatchPickup> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "batch_pickup")
    );

    // 【新增】重构后的 StreamCodec，完美保留了你原来的读写逻辑
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBatchPickup> CODEC = StreamCodec.of(
            (buf, packet) -> {
                // 将数据包序列化为字节流（客户端发送时调用）
                buf.writeBoolean(packet.isAuto());
                buf.writeBoolean(packet.limitToMaxStack());
                buf.writeVarInt(packet.entityIds().size());
                packet.entityIds().forEach(buf::writeInt);
            },
            buf -> {
                // 从字节流中反序列化数据包（服务端接收时调用）
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
     * 服务端处理逻辑的核心方法
     */
    // 【修改】去掉了 Supplier，直接传入 NetworkManager.PacketContext
    public void handle(NetworkManager.PacketContext ctx) {
        // 确保逻辑在服务端主线程上安全执行
        ctx.queue(() -> { // 【修改】去掉了 .get()
            ServerPlayer player = (ServerPlayer) ctx.getPlayer(); // 【修改】去掉了 .get()
            if (player == null || !player.isAlive()) return;

            // 根据是否限制拾取数量来设定初始配额 (record 中需通过方法调用获取)
            int remainingQuota = this.limitToMaxStack() ? 64 : Integer.MAX_VALUE;
            boolean anySuccess = false;     // 标记是否成功捡起了至少一个物品
            boolean inventoryFull = false;  // 标记玩家背包是否已满

            for (int id : this.entityIds()) {
                if (remainingQuota <= 0) break; // 额度耗尽，停止拾取
                Entity target = player.level().getEntity(id);

                // 安全检查：目标必须是掉落物，存活，且距离玩家的平方小于 100（约 10 格内）防作弊
                if (target instanceof ItemEntity item && item.isAlive() && player.distanceToSqr(target) < 100.0) {
                    // 如果物品还在拾取冷却中，则跳过
                    if (this.isAuto() && ((ItemEntityAccessor)item).getPickupDelay() > 0) continue;

                    ItemStack stack = item.getItem();
                    ISuperStack superStack = (ISuperStack) item;

                    // 计算该掉落物实体包含的实际总物品数（原版数量 + 额外堆叠数量）
                    int baseCount = stack.getCount();
                    int extraCount = superStack.betterlooting$getExtraCount();
                    int totalAvailable = baseCount + extraCount;

                    // 确定本次实际要捡起多少个
                    int toTake = Math.min(totalAvailable, remainingQuota);
                    if (toTake <= 0) continue;

                    int actuallyTaken = 0;
                    int amountLeftToInsert = toTake;
                    int maxStackSize = stack.getMaxStackSize();

                    // 【核心分块逻辑】：原版 inventory.add() 不支持处理超过最大堆叠数 (如 64) 的 ItemStack。
                    // 因此必须将我们需要拾取的超大数量，切分成最大堆叠大小的块，逐个尝试塞进背包。
                    while (amountLeftToInsert > 0) {
                        int insertChunk = Math.min(amountLeftToInsert, maxStackSize);
                        ItemStack chunkStack = stack.copy();
                        chunkStack.setCount(insertChunk);

                        // 尝试塞入玩家背包
                        if (player.getInventory().add(chunkStack)) {
                            anySuccess = true;
                            // chunkStack.getCount() 会变成未能塞入的数量（如果背包满了）
                            int chunkTaken = insertChunk - chunkStack.getCount();
                            actuallyTaken += chunkTaken;
                            amountLeftToInsert -= chunkTaken;

                            // 如果塞入后还有剩余，说明背包满了
                            if (chunkTaken < insertChunk) {
                                inventoryFull = true;
                                break;
                            }
                        } else {
                            inventoryFull = true;
                            break;
                        }
                    }

                    // 如果成功捡起了一部分或全部物品
                    if (actuallyTaken > 0) {
                        remainingQuota -= actuallyTaken;
                        player.take(item, actuallyTaken); // 触发原版捡拾动画和统计数据更新

                        int remainingAfterTake = totalAvailable - actuallyTaken;

                        // 根据剩余数量更新地上的掉落物状态
                        if (remainingAfterTake <= 0) {
                            item.discard(); // 全捡完了，清除实体
                        } else {
                            // 没捡完（背包满了或配额耗尽），重新分配掉落物的 原版数量 和 额外数量
                            int newBaseCount = Math.min(remainingAfterTake, maxStackSize);
                            int newExtraCount = remainingAfterTake - newBaseCount;

                            stack.setCount(newBaseCount);
                            item.setItem(stack);
                            superStack.betterlooting$setExtraCount(newExtraCount);
                        }
                    }
                }
            }

            // 如果有捡起物品，播放一次捡拾音效
            if (anySuccess) {
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);
            }
            // 如果背包满了，且不是自动拾取（避免疯狂刷屏），则给玩家发送红字提示
            if (inventoryFull && !this.isAuto()) {
                player.displayClientMessage(Component.translatable("message.better_looting.inventory_full").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}