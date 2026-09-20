package com.islesplus.mixin;

import com.islesplus.features.superjump.SuperJump;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The raw press / release of every key and mouse button, as it happens (not once a tick): Super
 * Jump holds two vanilla bindings for exactly as long as its own key is physically down. */
@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Inject(method = "setKeyPressed", at = @At("TAIL"))
    private static void islesplus$onRawKey(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        SuperJump.onRawKey(key, pressed);
    }
}
