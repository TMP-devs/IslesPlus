package com.islesplus.features.grounditemsnotifier;

import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class GroundItemsHudRenderer {

    private static final Identifier STAR_ICON = Identifier.of("islesplus", "textures/hud/star.png");
    /** The star art is 40x40, drawn at 20 GUI px: one art pixel per screen pixel at GUI scale 2. */
    private static final int STAR = 20, STAR_ART = 40, STAR_GAP = 5;
    private static final float TEXT_SCALE = 2f;

    private GroundItemsHudRenderer() {}

    public static void render(DrawContext context, MinecraftClient client) {
        if (FeatureFlags.isKilled("ground_items_notifier")) return;
        if (!GroundItemsNotifier.hasScreenNotifiers()) return;
        if (client.textRenderer == null) return;
        if (client.options.hudHidden || client.currentScreen != null) return;

        drawAlert(context, client.getWindow().getScaledWidth(), 8);
    }

    /** Gold star, then the message, centred as one unit. */
    public static void drawAlert(DrawContext context, int screenW, int y) {
        String msg = "Item on ground!";
        int textH = Fonts.height(TEXT_SCALE);
        int x = (screenW - (STAR + STAR_GAP + Fonts.width(msg, TEXT_SCALE))) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, STAR_ICON, x, y + (textH - STAR) / 2, 0, 0, STAR, STAR,
            STAR_ART, STAR_ART, STAR_ART, STAR_ART);
        Fonts.drawShadowed(context, msg, x + STAR + STAR_GAP, y, Theme.HUD_WARN, TEXT_SCALE);
    }
}
