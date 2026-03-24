package com.mohuia.better_looting.network.C2S;

import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.mixin.ItemEntityAccessor;
import dev.architectury.networking.NetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端到服务端 (C2S) 的批量拾取数据包
 * 用于通知服务端玩家尝试拾取了一批掉落物，并处理“超大堆叠”掉落物的背包塞入逻辑。
 */
public class PacketBatchPickup {
    private final List<Integer> entityIds; // 要拾取的掉落物实体 ID 列表
    private final boolean isAuto;          // 是否为自动拾取触发（用于控制提示信息的显示）
    private final boolean limitToMaxStack; // 是否限制一次最多只捡一组（64个）

    public PacketBatchPickup(List<Integer> entityIds, boolean isAuto, boolean limitToMaxStack) {
        this.entityIds = entityIds;
        this.isAuto = isAuto;
        this.limitToMaxStack = limitToMaxStack;
    }

    /**
     * 从字节流中反序列化数据包（服务端接收时调用）
     */
    public PacketBatchPickup(FriendlyByteBuf buf) {
        this.isAuto = buf.readBoolean();
        this.limitToMaxStack = buf.readBoolean();
        int count = buf.readVarInt();
        this.entityIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) this.entityIds.add(buf.readInt());
    }

    /**
     * 将数据包序列化为字节流（客户端发送时调用）
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isAuto);
        buf.writeBoolean(limitToMaxStack);
        buf.writeVarInt(entityIds.size());
        entityIds.forEach(buf::writeInt);
    }

    /**
     * 服务端处理逻辑的核心方法
     */
    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        // 确保逻辑在服务端主线程上安全执行
        ctx.get().queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.get().getPlayer();
            if (player == null || !player.isAlive()) return;

            // 根据是否限制拾取数量来设定初始配额
            int remainingQuota = limitToMaxStack ? 64 : Integer.MAX_VALUE;
            boolean anySuccess = false;     // 标记是否成功捡起了至少一个物品
            boolean inventoryFull = false;  // 标记玩家背包是否已满

            for (int id : entityIds) {
                if (remainingQuota <= 0) break; // 额度耗尽，停止拾取
                Entity target = player.level().getEntity(id);

                // 安全检查：目标必须是掉落物，存活，且距离玩家的平方小于 100（约 10 格内）防作弊
                if (target instanceof ItemEntity item && item.isAlive() && player.distanceToSqr(target) < 100.0) {
                    // 如果物品还在拾取冷却中，则跳过
                    if (this.isAuto && ((ItemEntityAccessor)item).getPickupDelay() > 0) continue;

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
            if (inventoryFull && !isAuto) {
                player.displayClientMessage(Component.translatable("message.better_looting.inventory_full").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}