package com.islesplus.features.qtetracker;

import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class QteHudRenderer {
    private static final float TEXT_SCALE = 1.5f;

    /** The current bonus QTE's name, a little below the crosshair by default. */
    public static final HudElement ELEMENT = new HudElement("qte_bonus", "Bonus QTE",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.CENTER, 0, 47)) {
        @Override public boolean enabled() { return QteTracker.qteTrackerEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (!QteTracker.qteTrackerEnabled || FeatureFlags.isKilled("qte_tracker")) return false;
            return client.textRenderer != null && !QteTracker.getTracked().isEmpty();
        }
        @Override public Size measure(boolean preview) {
            return new Size(Fonts.hudWidth(label(preview), TEXT_SCALE),
                Fonts.height(TEXT_SCALE) + Math.max(1, Math.round(Fonts.snap(TEXT_SCALE))));
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            QteTracker.QteType type = type(f.preview());
            // The game font with the Isles+ drop shadow; the colour stays the QTE type's own.
            if (type != null) Fonts.drawShadowed(ctx, label(type), 0, 0, type.color, TEXT_SCALE);
        }
    };

    private QteHudRenderer() {}

    private static QteTracker.QteType type(boolean preview) {
        List<QteTracker.TrackedQte> tracked = QteTracker.getTracked();
        if (!tracked.isEmpty()) return tracked.getFirst().type();
        return preview ? QteTracker.QteType.LUCK : null;
    }

    private static String label(boolean preview) {
        QteTracker.QteType type = type(preview);
        return type == null ? "" : label(type);
    }

    private static String label(QteTracker.QteType type) {
        return switch (type) {
            case LUCK -> "BONUS: Luck";
            case EXP -> "BONUS: Exp";
            case CHANCE -> "BONUS: Chance";
            case COINS -> "BONUS: Coins";
            case TICK_SKIP -> "Tick Skip";
        };
    }
}
