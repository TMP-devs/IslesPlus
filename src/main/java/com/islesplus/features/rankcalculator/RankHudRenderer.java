package com.islesplus.features.rankcalculator;

import com.islesplus.screen.hudedit.ScoreboardTracker;
import com.islesplus.sync.FeatureFlags;
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
    /** Slightly larger than body text so it reads at a glance mid-run. */
    private static final float TEXT_SCALE = Fonts.TITLE;

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
     * shadow, no panel.
     * Pass -1 (or a null nextRank) for a line that should not show. */
    private static void drawText(DrawContext context, int centerX, int y, int playerCount, int secsLeft,
                                 String nextRank, int ptsNeeded, int dropSecs) {
        int textLineY = y - Fonts.height(TEXT_SCALE) - LINE_GAP;
        if (playerCount >= 0) {
            String pcLabel = playerCount + (playerCount == 1 ? " player" : " players");
            Fonts.drawShadowed(context, pcLabel, centerX - Fonts.width(pcLabel, TEXT_SCALE) / 2, textLineY, Theme.HUD_MUTED, TEXT_SCALE);
            textLineY -= Fonts.height(TEXT_SCALE) + LINE_GAP;
        }

        // Countdown until the S is lost
        if (secsLeft >= 0) {
            String timer = String.format("%d:%02d", secsLeft / 60, secsLeft % 60);
            int color = secsLeft > 60 ? Theme.HUD_GOOD : secsLeft > 30 ? Theme.HUD_WARN : Theme.HUD_ALERT;
            Fonts.drawShadowed(context, timer, centerX - Fonts.width(timer, TEXT_SCALE) / 2, textLineY, color, TEXT_SCALE);
        }

        // Points needed for the next grade when below S
        if (nextRank != null) {
            String label = ptsNeeded >= 0 ? "+" + ptsNeeded + " pts for " + nextRank : nextRank + " no longer possible";
            int color = ptsNeeded >= 0 ? Theme.HUD_GOOD : Theme.HUD_ALERT;
            String timerStr = dropSecs >= 0 ? String.format(" %d:%02d", dropSecs / 60, dropSecs % 60) : "";
            int labelW = Fonts.width(label, TEXT_SCALE);
            int startX = centerX - (labelW + Fonts.width(timerStr, TEXT_SCALE)) / 2;
            Fonts.drawShadowed(context, label, startX, textLineY, color, TEXT_SCALE);
            if (!timerStr.isEmpty()) Fonts.drawShadowed(context, timerStr, startX + labelW, textLineY, Theme.HUD_ALERT, TEXT_SCALE);
        }
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
