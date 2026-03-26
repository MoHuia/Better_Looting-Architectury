package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.core.ISuperStack;
import com.mohuia.better_looting.config.BetterLootingConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
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
 * ItemEntity 的 Mixin，用于实现“掉落物超大堆叠”功能。
 * 通过实现 ISuperStack 接口（鸭子类型），为原版掉落物附加一个“额外数量”属性。
 * 这可以有效减少地上掉落物实体的数量，从而大幅优化游戏性能。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ISuperStack {

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);

    // 注册实体同步数据，用于在服务端和客户端之间同步“额外物品数量”
    @Unique
    private static final EntityDataAccessor<Integer> EXTRA_ITEM_COUNT = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);

    // NBT 标签键名，用于将额外数量保存到硬盘
    @Unique
    private static final String EXTRA_COUNT_NBT_KEY = "BetterLootingExtraCount";

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
     * 定义同步数据初始值
     */
    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void betterlooting$defineSynchedData(CallbackInfo ci) {
        this.entityData.define(EXTRA_ITEM_COUNT, 0);
    }

    /**
     * 写入 NBT 保存数据
     */
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void betterlooting$saveExtraCount(CompoundTag tag, CallbackInfo ci) {
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
        if (!BetterLootingConfig.get().enableSuperMerge) {
            return originalBox; // 如果玩家关闭了功能，则不扩大搜索框，直接返回原版参数
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
        if (!BetterLootingConfig.get().enableSuperMerge) return; // 关闭功能时不干预

        if (!this.getItem().isStackable()) return;
        cir.setReturnValue(true);
    }

    /**
     * 接管并重写实体合并逻辑（核心方法）
     * 将两个相同的掉落物实体数量相加，并存入存活下来的那个实体的“额外数量”中
     */
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void betterlooting$superMerge(ItemEntity other, CallbackInfo ci) {
        if (!BetterLootingConfig.get().enableSuperMerge) return; // 关闭功能时不接管合并逻辑

        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stackSelf = self.getItem();
        ItemStack stackOther = other.getItem();

        // 检查物品种类和 NBT 是否完全一致
        if (!Objects.equals(stackSelf.getItem(), stackOther.getItem()) ||
                !ItemStack.isSameItemSameTags(stackSelf, stackOther)) {
            return;
        }

        ISuperStack selfDuck = (ISuperStack) self;
        ISuperStack otherDuck = (ISuperStack) other;

        // 计算双方的总数量（原版数量 + 我们的额外数量）
        int selfTotal = stackSelf.getCount() + selfDuck.betterlooting$getExtraCount();
        int otherTotal = stackOther.getCount() + otherDuck.betterlooting$getExtraCount();

        // 谁的数量多谁就存活，把另一个实体的数量吸收过来，然后销毁较小的那个实体
        if (selfTotal >= otherTotal) {
            selfDuck.betterlooting$addExtraCount(otherTotal);
            self.setPickUpDelay(15);
            other.discard();
        } else {
            otherDuck.betterlooting$addExtraCount(selfTotal);
            other.setPickUpDelay(15);
            self.discard();
        }

        // 取消原版的合并逻辑
        ci.cancel();
    }

    /**
     * 每 Tick 检查并自动补充物品数量
     * 当掉落物的原版栈被拾取一部分（比如从 64 变成了 30），
     * 从“额外数量”池中提取物品来把原版栈补满，直到“额外数量”耗尽。
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void betterlooting$refillStack(CallbackInfo ci) {
        if (this.level().isClientSide) return; // 仅在服务端处理逻辑

        // 注: 此处的补货逻辑故意不加 enableSuperMerge 的判断
        // 这样可以保证玩家即便中途关掉了功能，地上那些已经被超大堆叠的物品依然能正常吐出货物
        int extraCount = this.betterlooting$getExtraCount();
        if (extraCount <= 0) return;

        ItemStack stack = this.getItem();
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
}