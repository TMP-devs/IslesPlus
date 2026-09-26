package com.islesplus.features.grounditemsnotifier;

import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class GroundItemsHudRenderer {

    private static final Identifier STAR_ICON = Identifier.of("islesplus", "textures/hud/star.png");
    /** The star art is 40x40, drawn at 20 GUI px: one art pixel per screen pixel at GUI scale 2. */
    private static final int STAR = 20, STAR_ART = 40, STAR_GAP = 5;
    private static final float TEXT_SCALE = 2f;
    private static final String MSG = "Item on ground!";

    /** Gold star, then the message, as one unit (top centre by default). */
    public static final HudElement ELEMENT = new HudElement("ground_items", "Item on Ground",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.START, 0, 6)) {
        @Override public boolean enabled() { return GroundItemsNotifier.groundItemsNotifierEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (FeatureFlags.isKilled("ground_items_notifier")) return false;
            if (!GroundItemsNotifier.hasScreenNotifiers()) return false;
            if (client.textRenderer == null) return false;
            return !client.options.hudHidden && client.currentScreen == null;
        }
        @Override public Size measure(boolean preview) {
            return new Size(STAR + STAR_GAP + Fonts.hudWidth(MSG, TEXT_SCALE), STAR);
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            int textH = Fonts.height(TEXT_SCALE);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, STAR_ICON, 0, 0, 0, 0, STAR, STAR,
                STAR_ART, STAR_ART, STAR_ART, STAR_ART);
            Fonts.drawShadowed(ctx, MSG, STAR + STAR_GAP, (STAR - textH) / 2, Theme.HUD_WARN, TEXT_SCALE);
        }
    };

    private GroundItemsHudRenderer() {}
}
