package com.mohuia.better_looting.mixin;

import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.client.KeyInit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 主类的 Mixin
 * 负责在客户端全局处理模组专属按键与原版按键的冲突问题
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow @Final public Options options;

    /**
     * 在处理按键绑定前进行拦截
     * 当玩家按下了本模组的功能键时，强制取消与其冲突的原版按键事件，防止出现一键多用的误操作。
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void betterLooting$interceptKeybinds(CallbackInfo ci) {
        // 检查是否满足拦截条件（例如是否在游戏中等）
        if (Core.shouldIntercept()) {

            // 定义需要优先响应的模组按键
            KeyMapping[] modKeys = {
                    KeyInit.PICKUP,
                    KeyInit.TOGGLE_FILTER,
                    KeyInit.TOGGLE_AUTO
            };

            for (KeyMapping modKey : modKeys) {
                if (modKey.isDown()) {
                    // 遍历所有原版按键，如果发现有按键与模组按键绑定了同一个键位
                    for (KeyMapping vanillaKey : this.options.keyMappings) {
                        if (vanillaKey != modKey && vanillaKey.same(modKey)) {
                            // 消耗掉原版按键的点击事件，并强制将其状态设为未按下
                            while (vanillaKey.consumeClick()) {
                                // 循环消耗掉所有堆积的点击事件
                            }
                            vanillaKey.setDown(false);
                        }
                    }
                }
            }
        }
    }
}