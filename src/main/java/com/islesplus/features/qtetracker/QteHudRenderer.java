package com.islesplus.features.qtetracker;

import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class QteHudRenderer {
    private static final float TEXT_SCALE = 1.5f;

    private QteHudRenderer() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (!QteTracker.qteTrackerEnabled || FeatureFlags.isKilled("qte_tracker")) return;
        if (client.textRenderer == null) return;

        List<QteTracker.TrackedQte> tracked = QteTracker.getTracked();
        if (tracked.isEmpty()) return;

        QteTracker.TrackedQte qte = tracked.getFirst();
        String labelStr = switch (qte.type()) {
            case LUCK -> "BONUS: Luck";
            case EXP -> "BONUS: Exp";
            case CHANCE -> "BONUS: Chance";
            case COINS -> "BONUS: Coins";
            case TICK_SKIP -> "Tick Skip";
        };

        int color = qte.type().color;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;
        int y = screenH / 2 + 40;

        // Silkscreen with the Isles+ drop shadow; the colour stays the QTE type's own.
        Fonts.drawShadowed(ctx, labelStr, centerX - Fonts.width(labelStr, TEXT_SCALE) / 2, y, color, TEXT_SCALE);
    }
}
