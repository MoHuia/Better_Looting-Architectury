package com.mohuia.better_looting.network.C2S;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.platform.PlatformHooks;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端到服务端 (C2S)：拖拽掉落物到物品栏指定槽位。
 */
public record PacketPlaceIntoSlot(
        List<Integer> entityIds,
        int slotIndex
) implements CustomPacketPayload {

    public static final Type<PacketPlaceIntoSlot> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "place_into_slot")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPlaceIntoSlot> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityIds().size());
                packet.entityIds().forEach(buf::writeInt);
                buf.writeVarInt(packet.slotIndex());
            },
            buf -> {
                int count = buf.readVarInt();
                List<Integer> ids = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    ids.add(buf.readInt());
                }
                return new PacketPlaceIntoSlot(ids, buf.readVarInt());
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player == null || !player.isAlive()) return;

            // 仅处理第一个有效实体
            ItemEntity item = null;
            for (int id : this.entityIds()) {
                Entity e = player.level().getEntity(id);
                if (e instanceof ItemEntity ie && ie.isAlive() && player.distanceToSqr(ie) < 100.0) {
                    item = ie;
                    break;
                }
            }
            if (item == null) return;

            ItemStack groundStack = item.getItem();
            ISuperStack superStack = (ISuperStack) item;
            int totalAvailable = groundStack.getCount() + superStack.betterlooting$getExtraCount();
            int toTake = Math.min(totalAvailable, 64);
            if (toTake <= 0) return;

            // 光标上已有物品时不处理，否则交换出的旧物品会覆盖并销毁它
            if (!player.containerMenu.getCarried().isEmpty()) return;

            // 目标槽位
            Slot slot = player.containerMenu.getSlot(this.slotIndex());
            if (slot == null || !slot.mayPlace(groundStack)) return;

            ItemStack insertStack = groundStack.copy();
            insertStack.setCount(toTake);
            int maxStack = insertStack.getMaxStackSize();
            ItemStack existing = slot.getItem();

            // actuallyTaken 只统计真正离开地面的数量（进入槽位 + 进入光标的地面物品）
            // toCarry 是要放到光标上的物品，可能是地面溢出物，也可能是被顶出的旧物品
            int actuallyTaken;
            ItemStack toCarry = ItemStack.EMPTY;

            if (existing.isEmpty()) {
                // 空槽位：先填满槽位，溢出部分放到光标
                int inSlot = Math.min(toTake, maxStack);
                slot.set(insertStack.copyWithCount(inSlot));
                actuallyTaken = inSlot;
                int overflow = Math.min(toTake - inSlot, maxStack);
                if (overflow > 0) {
                    toCarry = insertStack.copyWithCount(overflow);
                    actuallyTaken += overflow;
                }
            } else if (ItemStack.isSameItemSameComponents(existing, insertStack)) {
                // 同类型：补满堆叠，溢出部分放到光标
                int canAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), toTake);
                if (canAdd > 0) {
                    existing.grow(canAdd);
                    slot.setChanged();
                }
                actuallyTaken = canAdd;
                int overflow = Math.min(toTake - canAdd, maxStack);
                if (overflow > 0) {
                    toCarry = insertStack.copyWithCount(overflow);
                    actuallyTaken += overflow;
                }
            } else {
                // 异类：交换，旧物品占用光标，因此地面溢出物只能留在地上
                if (!slot.mayPickup(player)) return;
                int inSlot = Math.min(toTake, maxStack);
                slot.set(insertStack.copyWithCount(inSlot));
                actuallyTaken = inSlot;
                toCarry = existing.copy();
            }

            if (actuallyTaken <= 0) {
                player.containerMenu.broadcastChanges();
                return;
            }

            player.containerMenu.setCarried(toCarry);

            player.awardStat(Stats.ITEM_PICKED_UP.get(groundStack.getItem()), actuallyTaken);

            player.take(item, actuallyTaken);

            try {
                ItemStack pickedUp = groundStack.copy();
                pickedUp.setCount(actuallyTaken);
                PlatformHooks.fireItemPickupEvent(player, item, pickedUp);
            } catch (Throwable t) {
                player.sendSystemMessage(Component.literal("§c[BetterLooting] Platform hook failed: " + t));
            }

            int remainingAfterTake = totalAvailable - actuallyTaken;
            if (remainingAfterTake <= 0) {
                item.discard();
            } else {
                int newBase = Math.min(remainingAfterTake, groundStack.getMaxStackSize());
                groundStack.setCount(newBase);
                item.setItem(groundStack.copy());
                superStack.betterlooting$setExtraCount(remainingAfterTake - newBase);
            }

            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);

            player.containerMenu.broadcastChanges();
        });
    }
}
