package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.core.ISuperStack;
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

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ISuperStack {

    @Shadow public abstract ItemStack getItem();
    @Shadow public abstract void setItem(ItemStack stack);

    @Unique
    private static final EntityDataAccessor<Integer> EXTRA_ITEM_COUNT = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);
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

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void betterlooting$defineSynchedData(CallbackInfo ci) {
        this.entityData.define(EXTRA_ITEM_COUNT, 0);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void betterlooting$saveExtraCount(CompoundTag tag, CallbackInfo ci) {
        int extraCount = this.betterlooting$getExtraCount();
        if (extraCount > 0) {
            tag.putInt(EXTRA_COUNT_NBT_KEY, extraCount);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void betterlooting$readExtraCount(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(EXTRA_COUNT_NBT_KEY)) {
            this.betterlooting$setExtraCount(tag.getInt(EXTRA_COUNT_NBT_KEY));
        }
    }

    @ModifyArg(
            method = "mergeWithNeighbours",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            index = 1
    )
    private AABB betterlooting$expandMergeArea(AABB originalBox) {
        double radius = 5.0;
        return this.getBoundingBox().inflate(radius, radius, radius);
    }

    @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
    private void betterlooting$forceMergable(CallbackInfoReturnable<Boolean> cir) {
        if (!this.getItem().isStackable()) return;
        cir.setReturnValue(true);
    }

    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void betterlooting$superMerge(ItemEntity other, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stackSelf = self.getItem();
        ItemStack stackOther = other.getItem();

        if (!Objects.equals(stackSelf.getItem(), stackOther.getItem()) ||
                !ItemStack.isSameItemSameTags(stackSelf, stackOther)) {
            return;
        }

        ISuperStack selfDuck = (ISuperStack) self;
        ISuperStack otherDuck = (ISuperStack) other;

        int selfTotal = stackSelf.getCount() + selfDuck.betterlooting$getExtraCount();
        int otherTotal = stackOther.getCount() + otherDuck.betterlooting$getExtraCount();

        if (selfTotal >= otherTotal) {
            selfDuck.betterlooting$addExtraCount(otherTotal);
            self.setPickUpDelay(15);
            other.discard();
        } else {
            otherDuck.betterlooting$addExtraCount(selfTotal);
            other.setPickUpDelay(15);
            self.discard();
        }

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void betterlooting$refillStack(CallbackInfo ci) {
        if (this.level().isClientSide) return;

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