package com.islesplus.mixin;

import com.islesplus.IslesClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void islesplus$filterFocusSounds(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        String soundId = sound.getId().toString();
        if (!IslesClient.shouldAllowLocalSoundInFocus(soundId)) {
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }
}
