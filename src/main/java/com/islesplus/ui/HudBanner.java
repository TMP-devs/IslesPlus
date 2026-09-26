package com.islesplus.ui;

import com.islesplus.mixin.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * A timed warning over the world, centred just under the boss bars: "Void Rift in 1 hr", "Eggs
 * spawn in about 5 min". {@code Fonts.drawHud} at twice body size; the caller picks the tone and
 * how long it stays up.
 */
public final class HudBanner {
    private static final float SCALE = 2f;
    /** Vanilla's boss bar layout: the first bar at 12, each one 19 lower, the bar 5 tall. */
    private static final int BAR_TOP = 12, BAR_STEP = 19, BAR_H = 5, UNDER_BARS = 6;

    private HudBanner() {}

    public static void draw(DrawContext ctx, MinecraftClient client, String text, int argb) {
        int w = ctx.getScaledWindowWidth();
        Fonts.drawHud(ctx, text, (w - Fonts.hudWidth(text, SCALE)) / 2,
            underBossBars(client, ctx.getScaledWindowHeight()), argb, SCALE);
    }

    /** Just under the last boss bar, or where the first would be when there are none. */
    public static int underBossBars(MinecraftClient client, int screenH) {
        int bars = ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).getBossBars().size();
        if (bars == 0) return BAR_TOP;
        // vanilla stops drawing bars a third of the way down
        int lastTop = Math.min(BAR_TOP + (bars - 1) * BAR_STEP, screenH / 3);
        return lastTop + BAR_H + UNDER_BARS;
    }
}
