package com.mohuia.better_looting.network.C2S;

import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.mixin.ItemEntityAccessor;
import com.mohuia.better_looting.platform.PlatformHooks;
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
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端到服务端 (C2S) 的批量拾取数据包。
 * 作用：由客户端发送，通知服务端玩家尝试拾取了一批掉落物。
 * 核心功能是处理“超大堆叠（SuperStack）”掉落物安全塞入玩家背包的逻辑，
 * 并在背包满或达到拾取上限时停止拾取。
 */
public record PacketBatchPickup(
        /** 客户端请求拾取的掉落物实体 ID 列表 */
        List<Integer> entityIds,
        /** 是否为自动拾取（例如：范围自动吸附）。如果是，则需要严格校验物品的拾取冷却时间 */
        boolean isAuto,
        /** 是否限制单次拾取最大数量（通常限制为一组，即 64 个），防止一次性吸入过多导致卡顿或超模 */
        boolean limitToMaxStack
) implements CustomPacketPayload {

    public static final Type<PacketBatchPickup> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterLooting.MODID, "batch_pickup")
    );

    /**
     * 1.21.1 的编解码器 (替代了旧版的 toBytes 和解码构造函数)
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBatchPickup> CODEC = StreamCodec.of(
            (buf, packet) -> {
                // 编码方法 (客户端使用)
                buf.writeBoolean(packet.isAuto());
                buf.writeBoolean(packet.limitToMaxStack());
                buf.writeVarInt(packet.entityIds().size());
                packet.entityIds().forEach(buf::writeInt);
            },
            buf -> {
                // 解码构造逻辑 (服务端使用)
                boolean isAuto = buf.readBoolean();
                boolean limitToMaxStack = buf.readBoolean();
                int count = buf.readVarInt();
                List<Integer> entityIds = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    entityIds.add(buf.readInt());
                }
                return new PacketBatchPickup(entityIds, isAuto, limitToMaxStack);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 核心处理逻辑 (服务端执行)
     * 处理玩家的拾取请求，验证合法性，并将物品塞入玩家背包。
     */
    public void handle(NetworkManager.PacketContext ctx) {
        // 将逻辑放入主线程队列执行，确保线程安全（操作实体和背包必须在主线程）
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            // 基础校验：玩家必须在线且存活
            if (player == null || !player.isAlive()) return;

            // 如果启用了限制，则本次网络包最多只能拾取 64 个物品，否则无限制
            int remainingQuota = this.limitToMaxStack() ? 64 : Integer.MAX_VALUE;
            boolean anySuccess = false;   // 标记本次请求是否成功拾取了至少一个物品（用于触发音效）
            boolean inventoryFull = false; // 标记玩家背包是否已满

            // 遍历客户端发来的实体 ID 列表
            for (int id : this.entityIds()) {
                // 如果额度用尽或背包已满，直接终止后续处理，节省性能
                if (remainingQuota <= 0 || inventoryFull) break;

                Entity target = player.level().getEntity(id);

                // --- 1. 安全与防作弊校验 ---
                // 必须是物品实体、必须存活、距离玩家不能超过 10 格 (100.0 = 10^2)
                // 如果是自动拾取，原版机制中的“拾取冷却延迟 (PickupDelay)”必须已经归零
                if (!(target instanceof ItemEntity item) || !item.isAlive() || player.distanceToSqr(target) >= 100.0 ||
                        (this.isAuto() && ((ItemEntityAccessor) item).getPickupDelay() > 0)) continue;

                ItemStack stack = item.getItem();
                ISuperStack superStack = (ISuperStack) item; // 强转为我们的混合接口，获取超大堆叠数据

                // --- 2. 数量计算 ---
                // 总可用数量 = 原版物品栈数量 + 我们存储在 NBT/Mixin 里的额外数量
                int totalAvailable = stack.getCount() + superStack.betterlooting$getExtraCount();
                // 决定本次实际尝试拿取的数量（不能超过剩余拾取额度）
                int toTake = Math.min(totalAvailable, remainingQuota);
                if (toTake <= 0) continue;

                // --- 3. 模拟与插入背包 ---
                // 复制一份 ItemStack 用来尝试插入背包，避免直接修改世界中的实体导致数据异常
                ItemStack insertStack = stack.copy();
                insertStack.setCount(toTake);

                // 原版的 add 方法会自动处理背包格子分配。如果装不下，insertStack 的 count 会变成【剩余无法装下的数量】
                player.getInventory().add(insertStack);

                int remainderCount = insertStack.getCount();    // 塞完后剩下的数量（没装进去的）
                int actuallyTaken = toTake - remainderCount;    // 实际成功塞入背包的数量

                // 如果有剩余，说明玩家背包在这一个物品判定时已经满了
                if (remainderCount > 0) inventoryFull = true;

                // --- 4. 成功拾取后的状态更新 ---
                if (actuallyTaken > 0) {
                    anySuccess = true;
                    remainingQuota -= actuallyTaken; // 扣除额度
                    int remainingAfterTake = totalAvailable - actuallyTaken; // 实体在地上应该剩下的总数量

                    // 计算原版的拾取动画数量
                    // 为了让客户端播放动画时不显得奇怪，我们取 实际拾取量 和 (原版堆叠数-1) 之间的合适值
                    int animAmount = remainingAfterTake > 0 ? Math.min(actuallyTaken, Math.max(1, stack.getCount() - 1)) : actuallyTaken;

                    // 触发原版的拾取动画和统计数据更新
                    player.take(item, animAmount);
                    player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), actuallyTaken);

                    /* 构造一个代表“本次实际拾取”的 ItemStack 并通知平台 */
                    try {
                        ItemStack pickedUp = stack.copy();
                        pickedUp.setCount(actuallyTaken);
                        PlatformHooks.fireItemPickupEvent(player, item, pickedUp);
                    } catch (Throwable t) {
                        // 在游戏内聊天框输出简要的报错信息
                        player.sendSystemMessage(Component.literal("§c[BetterLooting 调试] 平台Hook失败: " + t));
                        // 输出堆栈的第一行，精准定位是哪一行代码崩了
                        if (t.getStackTrace().length > 0) {
                            player.sendSystemMessage(Component.literal("§c↳ 崩溃位置: " + t.getStackTrace()[0]));
                        }
                    }

                    // --- 5. 更新地面上的掉落物实体 ---
                    if (remainingAfterTake <= 0) {
                        // 全部捡完了，直接删除地面上的实体
                        item.discard();
                    } else {
                        // 没捡完，重新分配 原版ItemStack数量 和 超大堆叠额外数量
                        // 优先填满原版的 getMaxStackSize（比如 64），多出来的部分再存入 SuperStack 变量
                        int newBaseCount = Math.min(remainingAfterTake, stack.getMaxStackSize());
                        stack.setCount(newBaseCount);
                        item.setItem(stack.copy()); // 更新实体的基础物品信息
                        superStack.betterlooting$setExtraCount(remainingAfterTake - newBaseCount); // 更新额外数量
                    }
                }
            }

            // --- 6. 统一反馈 ---
            // 如果至少成功拾取了一个物品，播放一次拾取音效（避免循环里播放导致噪音轰炸）
            if (anySuccess) player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 2.0F);

            // 如果背包满了，且这是玩家主动操作（非自动拾取），在屏幕上方弹出红色提示文字
            if (inventoryFull && !this.isAuto()) {
                player.displayClientMessage(Component.translatable("message.better_looting.inventory_full").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}