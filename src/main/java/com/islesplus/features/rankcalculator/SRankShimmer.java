package com.islesplus.features.rankcalculator;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Random;

/**
 * The S badge's animation from the Rank Badges mockup, in badge-local GUI pixels (badge = 64):
 * gold twinkles that come and go around it, and every 20-30 s a shimmer - a soft gold glow
 * pulse plus a slanted band of light that sweeps across the badge, clipped to its silhouette.
 */
final class SRankShimmer {
    private static final int SIZE = 64;
    private static final Identifier MASK = Identifier.of("islesplus", "textures/rank/tier_s_mask.png");
    private static final Identifier GLOW = Identifier.of("islesplus", "textures/rank/tier_s_glow.png");
    private static final int GLOW_SIZE = 72, GLOW_INSET = 4;

    private static final long SHIMMER_MS = 2000, FIRST_SHIMMER_MS = 900;   // mockup sweeps in 1.2 s; slowed on request
    private static final int SHIMMER_GAP_MIN_S = 20, SHIMMER_GAP_SPREAD_S = 11;
    private static final float BAND_W = SIZE * 0.38f, SLANT = 0.325f;   // tan(18 deg)
    private static final int BAND_STEPS = 6;
    private static final float BAND_PEAK = 0.9f;

    // x, y, size, arm thickness
    private static final int[][] STARS = {
        {50, 6, 10, 3}, {5, 51, 10, 3}, {52, 43, 9, 2}, {29, 1, 9, 2}, {1, 23, 9, 2}, {56, 25, 8, 2}
    };
    private static final int STAR_ARM = 0xD9A441, STAR_CORE = 0xF6E3B0;
    private static final long STAR_MS = 3000, STAR_SPAWN_MS = 1000;
    private static final int MAX_LIVE_STARS = 3;

    private static final Random RANDOM = new Random();
    private static final long[] starBornMs = new long[STARS.length];
    private static long nextStarMs, shimmerStartMs, lastFrameMs;

    private SRankShimmer() {}

    /** Twinkles and glow go under/around the badge; call before drawing it. */
    static void renderBehind(DrawContext ctx, int x, int y) {
        long now = Util.getMeasuringTimeMs();
        // Not drawn for a while (left S, or left the rift): start over with a prompt shimmer.
        if (now - lastFrameMs > 2000) {
            shimmerStartMs = now + FIRST_SHIMMER_MS;
            nextStarMs = now;
            java.util.Arrays.fill(starBornMs, 0L);
        }
        lastFrameMs = now;
        if (now >= shimmerStartMs + SHIMMER_MS) {
            shimmerStartMs = now + 1000L * (SHIMMER_GAP_MIN_S + RANDOM.nextInt(SHIMMER_GAP_SPREAD_S));
        }

        float t = shimmerProgress(now);
        if (t >= 0f) {
            float pulse = 0.85f * (float) Math.sin(Math.PI * t);
            int tint = ((int) (pulse * 255) << 24) | 0xFFFFFF;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, GLOW, x - GLOW_INSET, y - GLOW_INSET, 0, 0,
                GLOW_SIZE, GLOW_SIZE, GLOW_SIZE, GLOW_SIZE, tint);
        }
    }

    /** The sweeping band and the twinkles go over the badge; call after drawing it. */
    static void renderOver(DrawContext ctx, int x, int y) {
        long now = Util.getMeasuringTimeMs();
        float t = shimmerProgress(now);
        if (t >= 0f) drawBand(ctx, x, y, t);
        spawnStar(now);
        for (int i = 0; i < STARS.length; i++) drawStar(ctx, x, y, i, now);
    }

    private static float shimmerProgress(long now) {
        if (now < shimmerStartMs || now >= shimmerStartMs + SHIMMER_MS) return -1f;
        return (now - shimmerStartMs) / (float) SHIMMER_MS;
    }

    /** One row at a time, so the band can lean; each row is a few strips of the white
     * silhouette whose alpha rises to the middle of the band and falls away again. */
    private static void drawBand(DrawContext ctx, int x, int y, float t) {
        float left = BAND_W * (-1.4f + 4.8f * t);
        float stepW = BAND_W / BAND_STEPS;
        for (int row = 0; row < SIZE; row++) {
            float rowLeft = left + (SIZE / 2f - row) * SLANT;
            for (int s = 0; s < BAND_STEPS; s++) {
                int x0 = Math.max(0, Math.round(rowLeft + s * stepW));
                int x1 = Math.min(SIZE, Math.round(rowLeft + (s + 1) * stepW));
                if (x1 <= x0) continue;
                float mid = (s + 0.5f) / BAND_STEPS;
                float alpha = BAND_PEAK * (1f - Math.abs(2f * mid - 1f));
                int tint = ((int) (alpha * 255) << 24) | 0xFFF8DC;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, MASK, x + x0, y + row, x0, row,
                    x1 - x0, 1, SIZE, SIZE, tint);
            }
        }
    }

    private static void spawnStar(long now) {
        if (now < nextStarMs) return;
        nextStarMs = now + STAR_SPAWN_MS;
        int live = 0, free = 0;
        for (long born : starBornMs) { if (born != 0 && now - born < STAR_MS) live++; else free++; }
        if (live >= MAX_LIVE_STARS || free == 0) return;
        int pick = RANDOM.nextInt(free);
        for (int i = 0; i < STARS.length; i++) {
            boolean alive = starBornMs[i] != 0 && now - starBornMs[i] < STAR_MS;
            if (!alive && pick-- == 0) { starBornMs[i] = now; return; }
        }
    }

    private static void drawStar(DrawContext ctx, int x, int y, int i, long now) {
        long born = starBornMs[i];
        if (born == 0 || now - born >= STAR_MS) return;
        float p = (now - born) / (float) STAR_MS;
        float k = p < 0.22f ? p / 0.22f : p > 0.68f ? (1f - p) / 0.32f : 1f;   // 0..1 in, hold, out
        int alpha = (int) (k * 255) << 24;
        int[] star = STARS[i];
        int full = star[2], arm = star[3];
        int size = Math.max(arm, Math.round(full * (0.6f + 0.4f * k)));
        if ((size - arm) % 2 != 0) size--;   // keep the arms centred on whole pixels
        int cx = x + star[0] + full / 2, cy = y + star[1] + full / 2;
        int half = size / 2, armHalf = arm / 2;
        int ax = cx - armHalf, ay = cy - armHalf;
        ctx.fill(cx - half, ay, cx - half + size, ay + arm, alpha | STAR_ARM);
        ctx.fill(ax, cy - half, ax + arm, cy - half + size, alpha | STAR_ARM);
        ctx.fill(ax, ay, ax + arm, ay + arm, alpha | STAR_CORE);
    }
}
