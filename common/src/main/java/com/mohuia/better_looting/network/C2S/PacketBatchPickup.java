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

public class PacketBatchPickup {
    private final List<Integer> entityIds;
    private final boolean isAuto;
    private final boolean limitToMaxStack;

    public PacketBatchPickup(List<Integer> entityIds, boolean isAuto, boolean limitToMaxStack) {
        this.entityIds = entityIds;
        this.isAuto = isAuto;
        this.limitToMaxStack = limitToMaxStack;
    }

    public PacketBatchPickup(FriendlyByteBuf buf) {
        this.isAuto = buf.readBoolean();
        this.limitToMaxStack = buf.readBoolean();
        int count = buf.readVarInt();
        this.entityIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) this.entityIds.add(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isAuto);
        buf.writeBoolean(limitToMaxStack);
        buf.writeVarInt(entityIds.size());
        entityIds.forEach(buf::writeInt);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.get().getPlayer();
            if (player == null || !player.isAlive()) return;

            int remainingQuota = limitToMaxStack ? 64 : Integer.MAX_VALUE;
            boolean anySuccess = false;
            boolean inventoryFull = false;

            for (int id : entityIds) {
                if (remainingQuota <= 0) break;
                Entity target = player.level().getEntity(id);

                if (target instanceof ItemEntity item && item.isAlive() && player.distanceToSqr(target) < 100.0) {
                    if (((ItemEntityAccessor)item).getPickupDelay() > 0) continue;

                    ItemStack stack = item.getItem();
                    ISuperStack superStack = (ISuperStack) item;

                    int baseCount = stack.getCount();
                    int extraCount = superStack.betterlooting$getExtraCount();
                    int totalAvailable = baseCount + extraCount;

                    int toTake = Math.min(totalAvailable, remainingQuota);
                    if (toTake <= 0) continue;

                    int actuallyTaken = 0;
                    int amountLeftToInsert = toTake;
                    int maxStackSize = stack.getMaxStackSize();

                    while (amountLeftToInsert > 0) {
                        int insertChunk = Math.min(amountLeftToInsert, maxStackSize);
                        ItemStack chunkStack = stack.copy();
                        chunkStack.setCount(insertChunk);

                        if (player.getInventory().add(chunkStack)) {
                            anySuccess = true;
                            int chunkTaken = insertChunk - chunkStack.getCount();
                            actuallyTaken += chunkTaken;
                            amountLeftToInsert -= chunkTaken;

                            if (chunkTaken < insertChunk) {
                                inventoryFull = true;
                                break;
                            }
                        } else {
                            inventoryFull = true;
                            break;
                        }
                    }

                    if (actuallyTaken > 0) {
                        remainingQuota -= actuallyTaken;
                        player.take(item, actuallyTaken);

                        int remainingAfterTake = totalAvailable - actuallyTaken;

                        if (remainingAfterTake <= 0) {
                            item.discard();
                        } else {
                            int newBaseCount = Math.min(remainingAfterTake, maxStackSize);
                            int newExtraCount = remainingAfterTake - newBaseCount;

                            stack.setCount(newBaseCount);
                            item.setItem(stack);
                            superStack.betterlooting$setExtraCount(newExtraCount);
                        }
                    }
                }
            }

            if (anySuccess) {
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);
            }
            if (inventoryFull && !isAuto) {
                player.displayClientMessage(Component.translatable("message.better_looting.inventory_full").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}