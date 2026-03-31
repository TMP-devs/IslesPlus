package com.islesplus.features.rankcalculator;

import com.islesplus.screen.hudedit.ScoreboardTracker;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RankHudRenderer {
    private static final int TEXTURE_SIZE = 64;
    private static final int GAP = 4;

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

        Identifier texture = getTexture(RankCalculator.lastRank);
        if (texture == null) return;

        int x = ScoreboardTracker.x + ScoreboardTracker.width / 2 - TEXTURE_SIZE / 2;
        int y = ScoreboardTracker.y - TEXTURE_SIZE - GAP;
        if (y < 0) y = 0;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);

        // Show player count above icon if enabled
        int textLineY = y - client.textRenderer.fontHeight - 2;
        if (RankCalculator.showPlayerCount) {
            String pcLabel = RankCalculator.playerCount + " players";
            int pcW = client.textRenderer.getWidth(pcLabel);
            context.drawTextWithShadow(client.textRenderer, pcLabel,
                x + TEXTURE_SIZE / 2 - pcW / 2, textLineY, 0xFF888888);
            textLineY -= client.textRenderer.fontHeight + 2;
        }

        // Show countdown timer above the icon when at S rank
        int secsLeft = RankCalculator.getSecondsUntilDemotion();
        if (secsLeft >= 0) {
            String timer = String.format("%d:%02d", secsLeft / 60, secsLeft % 60);
            int textWidth = client.textRenderer.getWidth(timer);
            int color = secsLeft > 60 ? 0xFFD4AF37 : secsLeft > 30 ? 0xFFFFAA00 : 0xFFE74C3C;
            context.drawTextWithShadow(client.textRenderer, timer,
                x + TEXTURE_SIZE / 2 - textWidth / 2, textLineY, color);
        }

        // Show points needed for next rank when below S
        String nextRank = RankCalculator.getNextRank();
        int ptsNeeded = RankCalculator.getPointsUntilPromotion();
        if (nextRank != null) {
            String label = ptsNeeded >= 0 ? "+" + ptsNeeded + " pts for " + nextRank : nextRank + " no longer possible";
            int color = ptsNeeded >= 0 ? 0xFFD4AF37 : 0xFFE74C3C;
            int dropSecs = RankCalculator.showRankDropTimer ? RankCalculator.getSecondsUntilRankDrop() : -1;
            String timerStr = dropSecs >= 0 ? String.format(" %d:%02d", dropSecs / 60, dropSecs % 60) : "";
            int totalW = client.textRenderer.getWidth(label) + client.textRenderer.getWidth(timerStr);
            int startX = x + TEXTURE_SIZE / 2 - totalW / 2;
            context.drawTextWithShadow(client.textRenderer, label, startX, textLineY, color);
            if (!timerStr.isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, timerStr,
                    startX + client.textRenderer.getWidth(label), textLineY, 0xFFE74C3C);
            }
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
