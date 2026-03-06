package com.islesplus.mixin;

import com.islesplus.IslesClient;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfirmScreen.class)
public abstract class ConfirmScreenMixin extends Screen {
    @Shadow @Final protected BooleanConsumer callback;

    protected ConfirmScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void islesplus$autoAcceptResourcePack(CallbackInfo ci) {
        if (!IslesClient.connectingToIsles) return;
        String title = this.getTitle().getString().toLowerCase();
        if (!title.contains("resource pack") && !title.contains("texture prompt")) return;

        IslesClient.connectingToIsles = false;
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> this.callback.accept(true));
    }
}
