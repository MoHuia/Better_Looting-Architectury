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

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow @Final public Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void betterLooting$interceptKeybinds(CallbackInfo ci) {
        if (Core.shouldIntercept()) {

            KeyMapping[] myKeys = {
                    KeyInit.PICKUP,
                    KeyInit.TOGGLE_FILTER,
                    KeyInit.TOGGLE_AUTO
            };
            for (KeyMapping modKey : myKeys) {
                if (modKey.isDown()) {
                    for (KeyMapping vanillaKey : this.options.keyMappings) {
                        if (vanillaKey != modKey && vanillaKey.same(modKey)) {
                            while (vanillaKey.consumeClick()) {
                            }
                            vanillaKey.setDown(false);
                        }
                    }
                }
            }
        }
    }
}