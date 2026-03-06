package com.islesplus.mixin;

import com.islesplus.IslesClient;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    private void islesplus$onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        String soundId = packet.getSound().getIdAsString();
        if (IslesClient.shouldMuteIncomingSound(soundId)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"), cancellable = true)
    private void islesplus$onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        String soundId = packet.getSound().getIdAsString();
        if (IslesClient.shouldMuteIncomingSound(soundId)) {
            ci.cancel();
        }
    }
}
