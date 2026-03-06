package com.islesplus.features.qtetracker;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class QteHudRenderer {
    private static final int SHADOW_COLOR = 0xFF2A2A2A;
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
        Text label = Text.literal(labelStr).styled(s -> s.withBold(true));

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;
        int y = screenH / 2 + 40;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);

        // Scale coordinates to match the scaled matrix
        float scaledCenterX = centerX / TEXT_SCALE;
        float scaledY = y / TEXT_SCALE;

        // Draw QTE type label (bold) with drop shadow
        int labelW = client.textRenderer.getWidth(label);
        int labelX = (int) (scaledCenterX - labelW / 2.0f);
        int labelY = (int) scaledY;
        Text shadow = Text.literal(labelStr).styled(s -> s.withBold(true));
        ctx.drawText(client.textRenderer, shadow, labelX + 1, labelY + 1, SHADOW_COLOR, false);
        ctx.drawText(client.textRenderer, label, labelX, labelY, color, false);

        ctx.getMatrices().popMatrix();
    }
}
