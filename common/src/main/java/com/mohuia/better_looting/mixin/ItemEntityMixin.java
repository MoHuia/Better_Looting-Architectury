package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.config.BetterLootingConfig;
import dev.architectury.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/**
 * ItemEntity 的 Mixin，用于实现"掉落物超大堆叠"功能。
 * 通过实现 ISuperStack 接口（鸭子类型），为原版掉落物附加一个"额外数量"属性。
 * 这可以有效减少地上掉落物实体的数量，从而大幅优化游戏性能。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ISuperStack {

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);

    // 注册实体同步数据，用于在服务端和客户端之间同步"额外物品数量"
    @Unique
    private static final EntityDataAccessor<Integer> EXTRA_ITEM_COUNT = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);

    // NBT 标签键名，用于将额外数量保存到硬盘
    @Unique
    private static final String EXTRA_COUNT_NBT_KEY = "BetterLootingExtraCount";

    @Unique
    private boolean betterlooting$savingItem;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int betterlooting$getExtraCount() {
        return this.entityData.get(EXTRA_ITEM_COUNT);
    }

    @Override
    public void betterlooting$setExtraCount(int count) {
        this.entityData.set(EXTRA_ITEM_COUNT, Math.max(0, count));
    }

    @Override
    public void betterlooting$addExtraCount(int count) {
        this.betterlooting$setExtraCount(this.betterlooting$getExtraCount() + count);
    }

    /**
     * Create 的运输和加工读取普通 ItemStack，然后可能直接 discard 实体。
     * 它无法读取 EXTRA_ITEM_COUNT；脚下方块黑名单也覆盖不到风道和机器吸入范围。
     * 配置在首次检测 Create 时默认关闭，用户仍可自行选择开启。
     */
    @Unique
    private boolean betterlooting$canSuperMerge() {
        return BetterLootingConfig.get().enableSuperMerge;
    }

    /**
     * 在消费者读取物品之前拆开历史超级堆叠。
     * 不能只在 tick 尾部补货：Create 可能在方块实体 tick 或 entityInside 中
     * 读取物品后立即删除实体，根本不会到达补货逻辑。
     */
    @Inject(method = "getItem", at = @At("RETURN"))
    private void betterlooting$unpackForCreate(CallbackInfoReturnable<ItemStack> cir) {
        if (this.level().isClientSide || betterlooting$savingItem || this.betterlooting$getExtraCount() <= 0) return;
        if (BetterLootingConfig.get().enableSuperMerge) return;
        if (!Platform.isModLoaded("create")) return;
        betterlooting$spillExtra(cir.getReturnValue());
    }

    /**
     * 拦截原版 playerTouch 入口。
     * <p>
     * ALWAYS 模式：无条件取消原版拾取，完全由 BL 接管（用户主动选择，可能与背包类模组冲突）。
     * <p>
     * AUTO 模式（默认）：
     * - Forge：放行 playerTouch，让 Forge 的 EntityItemPickupEvent 正常抛出。精妙背包等"拾取响应"
     *   模组依赖该事件实现拾取升级，BL 的 ForgePickupInterceptor（LOWEST 优先级）作为兜底。
     * - Fabric：保持拦截（Fabric 无优先级事件总线，由 PICKUP_ITEM_PRE 兜底；官方精妙背包无 Fabric 版）。
     * <p>
     * 实际拾取操作由 PacketBatchPickup 完全接管，不走此路径。
     */
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void betterlooting$interceptPlayerTouch(Player player, CallbackInfo ci) {
        if (this.level().isClientSide) return;
        BetterLootingConfig.PickupInterceptMode mode = BetterLootingConfig.get().pickupInterceptMode;
        if (mode == BetterLootingConfig.PickupInterceptMode.ALWAYS) {
            ci.cancel();
            return;
        }
        // AUTO 模式：仅 Fabric 端拦截（详见方法注释）
        if (Platform.isFabric()) {
            ci.cancel();
        }
    }

    /**
     * 定义同步数据初始值
     */
    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void betterlooting$defineSynchedData(CallbackInfo ci) {
        this.entityData.define(EXTRA_ITEM_COUNT, 0);
    }

    /**
     * 写入 NBT 保存数据
     */
    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void betterlooting$beginItemSave(CompoundTag tag, CallbackInfo ci) {
        // 原版保存也会调用 getItem；保存过程中不要向正在序列化的世界添加实体。
        betterlooting$savingItem = true;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void betterlooting$saveExtraCount(CompoundTag tag, CallbackInfo ci) {
        betterlooting$savingItem = false;
        int extraCount = this.betterlooting$getExtraCount();
        if (extraCount > 0) {
            tag.putInt(EXTRA_COUNT_NBT_KEY, extraCount);
        }
    }

    /**
     * 读取 NBT 恢复数据
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void betterlooting$readExtraCount(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(EXTRA_COUNT_NBT_KEY)) {
            this.betterlooting$setExtraCount(tag.getInt(EXTRA_COUNT_NBT_KEY));
        }
    }

    /**
     * 扩大掉落物的合并搜索范围
     * 修改 mergeWithNeighbours 中调用 getEntitiesOfClass 的搜索框参数
     */
    @ModifyArg(
            method = "mergeWithNeighbours",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            index = 1
    )
    private AABB betterlooting$expandMergeArea(AABB originalBox) {
        if (!betterlooting$canSuperMerge()) {
            return originalBox; // 关闭功能时使用原版搜索范围
        }

        // 分别获取 XZ 和 Y 的自定义合并半径
        double radiusXZ = BetterLootingConfig.get().mergeRangeXZ;
        double radiusY = BetterLootingConfig.get().mergeRangeY;

        return this.getBoundingBox().inflate(radiusXZ, radiusY, radiusXZ);
    }

    /**
     * 强制允许合并
     * 只要物品本身是可堆叠的（如泥土），无论数量是否已满 64，都允许尝试合并
     */
    @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
    private void betterlooting$forceMergable(CallbackInfoReturnable<Boolean> cir) {
        if (!betterlooting$canSuperMerge()) return;

        if (!this.getItem().isStackable()) return;

        // 跳过正在被运输的物品（如机械动力传送带/漏斗/溜槽），避免干扰其运输逻辑
        // 通过检测物品脚下方块的注册名来判断是否处于运输系统中
        if (betterlooting$isOnTransportBlock()) return;

        cir.setReturnValue(true);
    }

    /**
     * 接管并重写实体合并逻辑（核心方法）
     * 将两个相同的掉落物实体数量相加，并存入存活下来的那个实体的"额外数量"中
     */
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void betterlooting$superMerge(ItemEntity other, CallbackInfo ci) {
        if (!betterlooting$canSuperMerge()) return;

        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stackSelf = self.getItem();
        ItemStack stackOther = other.getItem();

        // 检查物品种类和 NBT 是否完全一致
        if (!Objects.equals(stackSelf.getItem(), stackOther.getItem()) ||
                !ItemStack.isSameItemSameTags(stackSelf, stackOther)) {
            return;
        }

        // 跳过正在被运输的物品（如机械动力传送带/漏斗/溜槽），避免干扰运输逻辑
        if (betterlooting$isOnTransportBlock()) return;

        ISuperStack selfDuck = (ISuperStack) self;
        ISuperStack otherDuck = (ISuperStack) other;

        // 获取双方中最年轻的存活时间（寿命）
        int youngestAge = Math.min(((ItemEntityAccessor) self).getAge(), ((ItemEntityAccessor) other).getAge());

        // 获取双方当前的拾取延迟，取较大值，与原版逻辑保持一致
        int maxPickupDelay = Math.max(((ItemEntityAccessor) self).getPickupDelay(), ((ItemEntityAccessor) other).getPickupDelay());

        // 计算双方的总数量（原版数量 + 我们的额外数量）
        int selfTotal = stackSelf.getCount() + selfDuck.betterlooting$getExtraCount();
        int otherTotal = stackOther.getCount() + otherDuck.betterlooting$getExtraCount();

        // 谁的数量多谁就存活，把另一个实体的数量吸收过来，然后销毁较小的那个实体
        if (selfTotal >= otherTotal) {
            selfDuck.betterlooting$addExtraCount(otherTotal);
            otherDuck.betterlooting$setExtraCount(0); // 清零防止 remove 时重复溢出
            self.setPickUpDelay(maxPickupDelay);
            ((ItemEntityAccessor) self).setAge(youngestAge); // 更新寿命为最年轻的
            other.discard();
        } else {
            otherDuck.betterlooting$addExtraCount(selfTotal);
            selfDuck.betterlooting$setExtraCount(0); // 清零防止 remove 时重复溢出
            other.setPickUpDelay(maxPickupDelay);
            ((ItemEntityAccessor) other).setAge(youngestAge); // 更新寿命为最年轻的
            self.discard();
        }

        // 取消原版的合并逻辑
        ci.cancel();
    }

    /**
     * 每 Tick 检查并自动补充物品数量
     * 当掉落物的原版栈被拾取一部分（比如从 64 变成了 30），
     * 从"额外数量"池中提取物品来把原版栈补满，直到"额外数量"耗尽。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void betterlooting$refillStack(CallbackInfo ci) {
        if (this.level().isClientSide) return; // 仅在服务端处理逻辑

        // 注: 此处的补货逻辑故意不加 enableSuperMerge 的判断
        // 这样可以保证玩家即便中途关掉了功能，地上那些已经被超大堆叠的物品依然能正常吐出货物
        int extraCount = this.betterlooting$getExtraCount();
        if (extraCount <= 0) return;

        ItemStack stack = this.getItem();
        // getItem 可能已经把旧超级堆叠拆开，不能用读取前的 extraCount 再补一次。
        extraCount = this.betterlooting$getExtraCount();
        if (extraCount <= 0) return;
        int maxStackSize = stack.getMaxStackSize();
        int currentCount = stack.getCount();

        if (currentCount < maxStackSize) {
            int spaceLeft = maxStackSize - currentCount;
            int amountToRefill = Math.min(spaceLeft, extraCount);

            ItemStack newStack = stack.copy();
            newStack.grow(amountToRefill);

            this.setItem(newStack);
            this.betterlooting$setExtraCount(extraCount - amountToRefill);
        }
    }

    /**
     * 当外部模组（如机械动力鼓风机）通过 setItem 改变物品类型/NBT 时，
     * 将 ExtraCount 中存储的旧物品溢出为新的掉落物实体，
     * 防止物品丢失或被错误地"洗白"为新产品。
     */
    @Inject(method = "setItem", at = @At("HEAD"))
    private void betterlooting$spillExtraOnItemChange(ItemStack newStack, CallbackInfo ci) {
        if (this.level().isClientSide) return;

        // getItem 在 Create 兼容模式下可能已经拆开额外数量。
        // 必须在读取旧栈之后重新读取 extraCount，避免重复生成物品。
        ItemStack oldStack = this.getItem();

        // 如果物品类型或 NBT 未发生变化，说明是模组自身的操作，不需要溢出
        if (ItemStack.isSameItemSameTags(oldStack, newStack)) return;

        betterlooting$spillExtra(oldStack);
    }

    /** 将隐藏数量物化为正常物品栈；先清零，避免生成实体时重入产生重复物品。 */
    @Unique
    private void betterlooting$spillExtra(ItemStack oldStack) {
        int extraCount = this.betterlooting$getExtraCount();
        if (extraCount <= 0 || oldStack.isEmpty()) return;

        ItemEntity self = (ItemEntity) (Object) this;
        ItemEntityAccessor accessor = (ItemEntityAccessor) self;
        this.betterlooting$setExtraCount(0);
        int maxStack = oldStack.getMaxStackSize();
        while (extraCount > 0) {
            int spillAmount = Math.min(extraCount, maxStack);
            ItemStack spillStack = oldStack.copy();
            spillStack.setCount(spillAmount);
            extraCount -= spillAmount;

            ItemEntity spill = new ItemEntity(self.level(), self.getX(), self.getY(), self.getZ(), spillStack);
            spill.setPickUpDelay(accessor.getPickupDelay());
            ((ItemEntityAccessor) spill).setAge(accessor.getAge());
            spill.setTarget(accessor.betterlooting$getTarget());
            spill.setThrower(accessor.betterlooting$getThrower());
            spill.setDeltaMovement(self.getDeltaMovement());
            self.level().addFreshEntity(spill);
        }
    }

    /**
     * 检测物品实体是否站在运输方块上（如机械动力传送带、漏斗、溜槽等）。
     * 通过检查物品所在位置及下方方块的注册名，与配置中的黑名单关键词做匹配。
     * @return true 表示物品处于运输系统中，应跳过超大堆叠合并
     */
    @Unique
    private boolean betterlooting$isOnTransportBlock() {
        String blacklist = BetterLootingConfig.get().mergeTransportBlacklist;
        if (blacklist == null || blacklist.trim().isEmpty()) return false;

        String[] keywords = blacklist.split(",");
        BlockPos pos = this.blockPosition();

        // 检查物品所在方块和下方方块的注册名
        String idAt = BuiltInRegistries.BLOCK.getKey(this.level().getBlockState(pos).getBlock()).toString();
        String idBelow = BuiltInRegistries.BLOCK.getKey(this.level().getBlockState(pos.below()).getBlock()).toString();

        for (String kw : keywords) {
            String keyword = kw.trim().toLowerCase();
            if (keyword.isEmpty()) continue;
            if (idAt.contains(keyword) || idBelow.contains(keyword)) return true;
        }
        return false;
    }

    // Create 通过 getItem 读取物品之前，历史隐藏数量已经拆开。
    // superMerge 中清零仅负责本模组自己的合并删除，不能保护外部模组的 discard。
}
