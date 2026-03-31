package com.islesplus.features.grounditemsnotifier;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class GroundItemsHudRenderer {
    private static final int COLOR = 0xFFFFAA00;
    private static final int SHADOW = 0xFF2A2A2A;

    private GroundItemsHudRenderer() {}

    public static void render(DrawContext context, MinecraftClient client) {
        if (FeatureFlags.isKilled("ground_items_notifier")) return;
        if (!GroundItemsNotifier.hasScreenNotifiers()) return;
        if (client.textRenderer == null) return;
        if (client.options.hudHidden || client.currentScreen != null) return;

        Text msg = Text.literal("★ Item on ground!").styled(s -> s.withBold(true));
        int screenW = client.getWindow().getScaledWidth();
        int textW = client.textRenderer.getWidth(msg);
        int x = (screenW - textW) / 2;
        context.drawText(client.textRenderer, msg, x + 1, 9, SHADOW, false);
        context.drawText(client.textRenderer, msg, x, 8, COLOR, false);
    }
}
