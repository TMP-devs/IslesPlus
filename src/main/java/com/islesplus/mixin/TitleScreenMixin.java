package com.islesplus.mixin;

import com.islesplus.IslesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final String ISLES_HOST = "play.skyblockisles.net";

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void islesplus$addQuickJoinButton(CallbackInfo ci) {
        int width = 170;
        int height = 20;
        int x = 8;
        int y = 8;

        ButtonWidget quickJoinButton = ButtonWidget.builder(
            Text.literal("Join Skyblock Isles").formatted(Formatting.GOLD, Formatting.BOLD),
            button -> islesplus$connectToIsles()
        )
            .dimensions(x, y, width, height)
            .tooltip(Tooltip.of(Text.literal("Quick connect: ").formatted(Formatting.YELLOW)
                .append(Text.literal(ISLES_HOST).formatted(Formatting.GOLD, Formatting.BOLD))))
            .build();
        this.addDrawableChild(quickJoinButton);
    }

    private void islesplus$connectToIsles() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerAddress address = ServerAddress.parse(ISLES_HOST);
        ServerInfo info = new ServerInfo("Skyblock Isles", ISLES_HOST, ServerInfo.ServerType.OTHER);
        IslesClient.connectingToIsles = true;
        ConnectScreen.connect(this, client, address, info, false, null);
    }
}
