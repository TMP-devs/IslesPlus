package com.islesplus.features.rankcalculator;

import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
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

import java.util.ArrayList;
import java.util.List;

/** The rift rank badge and its countdown lines, sitting on top of the scoreboard (it moves with it,
 * whatever the scoreboard's size); the HUD editor can nudge it off that spot and scale it. */
public final class RankHudRenderer {
    /** Drawn size in GUI pixels; the badge art itself is 128x128 so it stays sharp at GUI scale 2+. */
    private static final int TEXTURE_SIZE = 64;
    private static final int ART_SIZE = 128;
    private static final int LINE_GAP = 3;
    /** The badge (with its S animation) is drawn at 80%; the text above it stays at full size so
     * it keeps its whole-pixel snapping and reads crisply. */
    private static final float BADGE_SCALE = 0.8f;
    private static final int BADGE = Math.round(TEXTURE_SIZE * BADGE_SCALE);
    /** Between body (1x) and the next whole-pixel size up (1.5x at GUI scale 2): big enough to read
     * at a glance mid-run, small enough that the long lines mostly fit beside the screen edge. */
    private static final float TEXT_SCALE = 1.25f;
    private static final int LINE_H = Math.round(Fonts.GLYPH_H * TEXT_SCALE);
    /** Text never comes closer than this to a screen edge. */
    private static final int EDGE_MARGIN = 2;

    private static final Identifier RANK_S = Identifier.of("islesplus", "textures/rank/tier_s.png");
    private static final Identifier RANK_A = Identifier.of("islesplus", "textures/rank/tier_a.png");
    private static final Identifier RANK_B = Identifier.of("islesplus", "textures/rank/tier_b.png");
    private static final Identifier RANK_C = Identifier.of("islesplus", "textures/rank/tier_c.png");
    private static final Identifier RANK_D = Identifier.of("islesplus", "textures/rank/tier_d.png");
    private static final Identifier RANK_E = Identifier.of("islesplus", "textures/rank/tier_e.png");
    private static final Identifier RANK_F = Identifier.of("islesplus", "textures/rank/tier_f.png");

    /** One line of text: pieces drawn left to right, each in its own colour. */
    private record Line(List<String> parts, List<Integer> colours) {
        int width() {
            int w = 0;
            for (String p : parts) w += textW(p);
            return w;
        }
    }

    private record Model(String rank, List<Line> lines) {
        /** Width of the widest line or the badge. */
        int width() {
            int w = BADGE;
            for (Line l : lines) w = Math.max(w, l.width());
            return w;
        }
        int textHeight() { return lines.size() * (LINE_H + LINE_GAP); }
    }

    public static final HudElement ELEMENT = new HudElement("rift_rank", "Rift Rank",
        new HudPlacement(0, 0, 0, 0)) {
        @Override public boolean enabled() { return RankCalculator.rankCalculatorEnabled; }
        @Override public boolean active(MinecraftClient client) {
            return RankCalculator.rankCalculatorEnabled && WorldIdentification.world == PlayerWorld.RIFT
                && !FeatureFlags.isKilled("rank_calculator") && getTexture(RankCalculator.lastRank) != null;
        }
        @Override public boolean followsScoreboard() { return true; }
        @Override public Size measure(boolean preview) {
            Model m = model(preview);
            return new Size(m.width(), m.textHeight() + BADGE);
        }
        @Override public int topSlack(boolean preview) { return model(preview).textHeight(); }
        @Override public void draw(DrawContext ctx, Frame f) { RankHudRenderer.draw(ctx, f); }
    };

    private RankHudRenderer() {}

    private static Model model(boolean preview) {
        if (preview && getTexture(RankCalculator.lastRank) == null) {
            // Sample: a rank below S, so the (wider) points line shows.
            List<Line> lines = new ArrayList<>();
            if (RankCalculator.showPlayerCount) lines.add(line("4 players", Theme.HUD_MUTED));
            Line pts = line("+120 pts for S", Theme.HUD_GOOD);
            if (RankCalculator.showRankDropTimer) { pts.parts().add(" 4:56"); pts.colours().add(Theme.HUD_ALERT); }
            lines.add(pts);
            return new Model("A", lines);
        }
        List<Line> lines = new ArrayList<>();
        if (RankCalculator.showPlayerCount) {
            int pc = RankCalculator.playerCount;
            lines.add(line(pc + (pc == 1 ? " player" : " players"), Theme.HUD_MUTED));
        }
        // Countdown until the S is lost
        int secsLeft = RankCalculator.getSecondsUntilDemotion();
        if (secsLeft >= 0) {
            int color = secsLeft > 60 ? Theme.HUD_GOOD : secsLeft > 30 ? Theme.HUD_WARN : Theme.HUD_ALERT;
            lines.add(line(String.format("%d:%02d", secsLeft / 60, secsLeft % 60), color));
        }
        // Points needed for the next grade when below S
        String nextRank = RankCalculator.getNextRank();
        if (nextRank != null) {
            int ptsNeeded = RankCalculator.getPointsUntilPromotion();
            Line l = line(ptsNeeded >= 0 ? "+" + ptsNeeded + " pts for " + nextRank : nextRank + " no longer possible",
                ptsNeeded >= 0 ? Theme.HUD_GOOD : Theme.HUD_ALERT);
            int dropSecs = RankCalculator.showRankDropTimer ? RankCalculator.getSecondsUntilRankDrop() : -1;
            if (dropSecs >= 0) {
                l.parts().add(String.format(" %d:%02d", dropSecs / 60, dropSecs % 60));
                l.colours().add(Theme.HUD_ALERT);
            }
            lines.add(l);
        }
        return new Model(RankCalculator.lastRank, lines);
    }

    private static Line line(String s, int colour) {
        List<String> parts = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        parts.add(s);
        colours.add(colour);
        return new Line(parts, colours);
    }

    /** Badge at the bottom centre; the text lines stack upward from it, the game font in the HUD tones
     * with the Isles+ drop shadow, no panel. ONE fixed text size whatever the lines say - the
     * numbers change every second, and a size that depended on their width made the whole block
     * jump. The badge sits over the sidebar, which hugs the screen edge, so a long line can be
     * wider than the room beside it: such a line slides back until it is on screen. */
    private static void draw(DrawContext context, HudElement.Frame f) {
        Model m = model(f.preview());
        int w = m.width(), h = m.textHeight() + BADGE;
        int centerX = w / 2;
        int badgeY = h - BADGE;

        // drawBadge works in a 64 px box; shrink that box onto the badge.
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate((float) (centerX - BADGE / 2), (float) badgeY);
            context.getMatrices().scale(BADGE / (float) TEXTURE_SIZE, BADGE / (float) TEXTURE_SIZE);
            drawBadge(context, m.rank());
        } finally {
            context.getMatrices().popMatrix();
        }

        // The screen edges in this element's own (scaled) pixels.
        float minX = (EDGE_MARGIN - f.x()) / f.scale();
        float maxX = (f.screenW() - EDGE_MARGIN - f.x()) / f.scale();
        int y = badgeY;
        for (int i = 0; i < m.lines().size(); i++) {   // the first line sits right on the badge
            Line l = m.lines().get(i);
            y -= LINE_H + LINE_GAP;
            int lw = l.width();
            int x = (int) Math.max(minX, Math.min(centerX - lw / 2, maxX - lw));
            for (int p = 0; p < l.parts().size(); p++) {
                text(context, l.parts().get(p), x, y, l.colours().get(p));
                x += textW(l.parts().get(p));
            }
        }
    }

    private static void drawBadge(DrawContext context, String rank) {
        Identifier texture = getTexture(rank);
        if (texture == null) return;
        boolean shimmer = texture == RANK_S;
        if (shimmer) SRankShimmer.renderBehind(context, 0, 0);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, TEXTURE_SIZE, TEXTURE_SIZE,
            ART_SIZE, ART_SIZE, ART_SIZE, ART_SIZE);
        if (shimmer) SRankShimmer.renderOver(context, 0, 0);
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
