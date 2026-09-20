package com.islesplus.features.rankcalculator;

import com.islesplus.screen.hudedit.ScoreboardTracker;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.ColorMath;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RankHudRenderer {
    /** Drawn size in GUI pixels; the badge art itself is 128x128 so it stays sharp at GUI scale 2+. */
    private static final int TEXTURE_SIZE = 64;
    private static final int ART_SIZE = 128;
    private static final int GAP = 4;
    private static final int LINE_GAP = 3;
    /** The badge (with its S animation) is drawn at 80%; the text above it stays at full size so
     * it keeps its whole-pixel snapping and reads crisply. */
    private static final float BADGE_SCALE = 0.8f;
    /** Between body (1x) and the next whole-pixel size up (1.5x at GUI scale 2): big enough to read
     * at a glance mid-run, small enough that the long lines mostly fit beside the screen edge. */
    private static final float TEXT_SCALE = 1.25f;
    /** Text never comes closer than this to a screen edge. */
    private static final int EDGE_MARGIN = 2;

    private static final Identifier RANK_S = Identifier.of("islesplus", "textures/rank/tier_s.png");
    private static final Identifier RANK_A = Identifier.of("islesplus", "textures/rank/tier_a.png");
    private static final Identifier RANK_B = Identifier.of("islesplus", "textures/rank/tier_b.png");
    private static final Identifier RANK_C = Identifier.of("islesplus", "textures/rank/tier_c.png");
    private static final Identifier RANK_D = Identifier.of("islesplus", "textures/rank/tier_d.png");
    private static final Identifier RANK_E = Identifier.of("islesplus", "textures/rank/tier_e.png");
    private static final Identifier RANK_F = Identifier.of("islesplus", "textures/rank/tier_f.png");

    private RankHudRenderer() {}

    public static void render(DrawContext context, MinecraftClient client) {
        if (!RankCalculator.rankCalculatorEnabled || WorldIdentification.world != PlayerWorld.RIFT || FeatureFlags.isKilled("rank_calculator")) return;
        if (!ScoreboardTracker.valid) return;
        renderAt(context, ScoreboardTracker.x, ScoreboardTracker.y, ScoreboardTracker.width,
            RankCalculator.showPlayerCount, RankCalculator.showRankDropTimer);
    }

    private static void renderAt(DrawContext context, int sbX, int sbY, int sbW, boolean playerCount, boolean dropTimer) {
        if (getTexture(RankCalculator.lastRank) == null) return;
        int size = Math.round(TEXTURE_SIZE * BADGE_SCALE);
        int centerX = sbX + sbW / 2;
        int x = centerX - size / 2;
        int y = Math.max(0, sbY - size - GAP);

        // drawBadge works in a 64 px box; shrink that box onto (x, y, size).
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate((float) x, (float) y);
            context.getMatrices().scale(size / (float) TEXTURE_SIZE, size / (float) TEXTURE_SIZE);
            drawBadge(context, 0, 0, RankCalculator.lastRank);
        } finally {
            context.getMatrices().popMatrix();
        }


        drawText(context, centerX, y,
            playerCount ? RankCalculator.playerCount : -1,
            RankCalculator.getSecondsUntilDemotion(), RankCalculator.getNextRank(),
            RankCalculator.getPointsUntilPromotion(),
            dropTimer ? RankCalculator.getSecondsUntilRankDrop() : -1);
    }

    private static void drawBadge(DrawContext context, int x, int y, String rank) {
        Identifier texture = getTexture(rank);
        if (texture == null) return;
        boolean shimmer = texture == RANK_S;
        if (shimmer) SRankShimmer.renderBehind(context, x, y);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE,
            ART_SIZE, ART_SIZE, ART_SIZE, ART_SIZE);
        if (shimmer) SRankShimmer.renderOver(context, x, y);
    }

    /** Text lines stack upward from the badge: Silkscreen in the HUD tones with the Isles+ drop
     * shadow, no panel. ONE fixed size ({@link #TEXT_SCALE}) whatever the lines say - the numbers
     * change every second, and a size that depended on their width made the whole block jump. The
     * badge is centred on the sidebar, which hugs the screen edge, so a long line
     * ("+120 pts for A 12:34") can be wider than the room either side of that centre: such a line
     * slides left until it is back on screen.
     * Pass -1 (or a null nextRank) for a line that should not show. */
    private static void drawText(DrawContext context, int centerX, int y, int playerCount, int secsLeft,
                                 String nextRank, int ptsNeeded, int dropSecs) {
        int screenW = context.getScaledWindowWidth();
        int lineH = Math.round(Fonts.GLYPH_H * TEXT_SCALE);

        int textLineY = y - lineH - LINE_GAP;
        if (playerCount >= 0) {
            String pcLabel = playerCount + (playerCount == 1 ? " player" : " players");
            text(context, pcLabel, lineX(centerX, textW(pcLabel), screenW), textLineY, Theme.HUD_MUTED);
            textLineY -= lineH + LINE_GAP;
        }

        // Countdown until the S is lost
        if (secsLeft >= 0) {
            String timer = String.format("%d:%02d", secsLeft / 60, secsLeft % 60);
            int color = secsLeft > 60 ? Theme.HUD_GOOD : secsLeft > 30 ? Theme.HUD_WARN : Theme.HUD_ALERT;
            text(context, timer, lineX(centerX, textW(timer), screenW), textLineY, color);
        }

        // Points needed for the next grade when below S
        if (nextRank != null) {
            String label = ptsNeeded >= 0 ? "+" + ptsNeeded + " pts for " + nextRank : nextRank + " no longer possible";
            int color = ptsNeeded >= 0 ? Theme.HUD_GOOD : Theme.HUD_ALERT;
            String dropStr = dropSecs >= 0 ? String.format(" %d:%02d", dropSecs / 60, dropSecs % 60) : "";
            int labelW = textW(label);
            int startX = lineX(centerX, labelW + textW(dropStr), screenW);
            text(context, label, startX, textLineY, color);
            if (!dropStr.isEmpty()) text(context, dropStr, startX + labelW, textLineY, Theme.HUD_ALERT);
        }
    }

    private static int textW(String s) { return (int) Math.ceil(Fonts.width(s) * TEXT_SCALE); }

    /** Text at exactly {@link #TEXT_SCALE} (not snapped to the GUI scale, like the World Bosses
     * panel), with the Isles+ drop shadow: quarter-brightness, one text pixel straight down. */
    private static void text(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) x, (float) y);
            ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);
            Fonts.draw(ctx, s, 0, 1, 0xFF000000 | ColorMath.darken(argb & 0xFFFFFF, 0.25f));
            Fonts.draw(ctx, s, 0, 0, argb);
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    /** Left edge of a line centred on centerX, pulled back on screen if it would run off either side. */
    private static int lineX(int centerX, int lineW, int screenW) {
        int x = centerX - lineW / 2;
        return Math.max(EDGE_MARGIN, Math.min(x, screenW - EDGE_MARGIN - lineW));
    }

    private static Identifier getTexture(String rank) {
        return switch (rank) {
            case "S" -> RANK_S;
            case "A" -> RANK_A;
            case "B" -> RANK_B;
            case "C" -> RANK_C;
            case "D" -> RANK_D;
            case "E" -> RANK_E;
            case "F" -> RANK_F;
            default  -> null;
        };
    }
}
