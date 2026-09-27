package com.islesplus.mixin;

import com.islesplus.features.chatchannel.ChatChannels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.screen.ChatScreen")
public class ChatScreenMixin {
    @ModifyVariable(
        method = "sendMessage(Ljava/lang/String;Z)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private String islesplus$routeChatMessage(String chatText) {
        return ChatChannels.route(chatText);
    }
}