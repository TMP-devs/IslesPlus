package com.islesplus.features.voidrift;

import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.time.ZoneId;

/**
 * The Void Rift's next spawn ({@link VoidRift}), one of three ways:
 * <ul>
 * <li>{@link Mode#ALERT}: a "Void Rift" / "in 1 hr" title mid-screen, like Inventory Full, then
 *     again at 10 and 5 minutes, with a countdown in the bottom-right corner for that last hour.</li>
 * <li>{@link Mode#COUNTDOWN}: the whole 7-hour countdown in the corner, always.</li>
 * <li>{@link Mode#LOCAL_TIME}: when it spawns, in the player's own time, in the corner.</li>
 * </ul>
 * Isles only. Its corner line is the bottom one; the egg timer stacks above it.
 */
public final class VoidRiftTimer {
    public enum Mode { OFF, ALERT, COUNTDOWN, LOCAL_TIME }

    public static Mode mode = Mode.ALERT;
    public static SoundConfig soundConfig = new SoundConfig("minecraft:block.note_block.chime", 1.0f, 1.0f);

    /** The alert rings three times, a few ticks apart, like an alarm. */
    private static final int RINGS = 3, TICKS_BETWEEN_RINGS = 6;
    private static int ringsLeft, ringTicks;
    public static final int CORNER_MARGIN = 10;
    private static final String LABEL = "Void Rift ";

    private static final VoidRift.Alerts alerts = new VoidRift.Alerts();
    private static VoidRift.Alert shown;
    private static long shownAtMs;

    private VoidRiftTimer() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        if (ringsLeft > 0 && ringTicks++ % TICKS_BETWEEN_RINGS == 0) {
            ModSounds.playAlert(client, soundConfig);
            ringsLeft--;
        }
        if (!active()) return;
        long now = System.currentTimeMillis();
        VoidRift.Alert due = alerts.update(VoidRift.nextSpawn(now), now);
        if (due != null && mode == Mode.ALERT) show(due);
    }

    private static void show(VoidRift.Alert alert) {
        shown = alert;
        shownAtMs = Util.getMeasuringTimeMs();
        ringsLeft = RINGS;
        ringTicks = 0;
    }

    private static final String SAMPLE_CORNER = "42:17";

    /** "Void Rift" / "in 1 hr" where a vanilla title sits by default, fading in and out. */
    public static final HudElement ALERT_ELEMENT = new HudElement("void_rift_alert", "Void Rift Alert",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.CENTER, 0, -6)) {
        @Override public boolean enabled() { return mode == Mode.ALERT; }
        @Override public boolean active(MinecraftClient client) {
            if (!VoidRiftTimer.active() || mode != Mode.ALERT || shown == null || client.options.hudHidden) return false;
            if (VoidRift.titleAlpha(Util.getMeasuringTimeMs() - shownAtMs) < 0) { shown = null; return false; }
            return true;
        }
        @Override public Size measure(boolean preview) {
            String sub = subtitle(preview);
            int w = Math.max(Fonts.width(TITLE, TITLE_SCALE), Fonts.width(sub, SUBTITLE_SCALE));
            return new Size(w, SUBTITLE_Y + Fonts.height(SUBTITLE_SCALE) + Math.round(Fonts.snap(SUBTITLE_SCALE)));
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            float alpha = f.preview() || shown == null ? 1f : VoidRift.titleAlpha(Util.getMeasuringTimeMs() - shownAtMs);
            drawTitle(ctx, subtitle(f.preview()), measure(f.preview()).w(), alpha);
        }
    };

    /** "Void Rift 42:17" in the bottom-right corner by default; the egg timer stacks above it. */
    public static final HudElement CORNER_ELEMENT = new HudElement("void_rift_timer", "Void Rift Timer",
        new HudPlacement(HudAnchor.END, HudAnchor.END, CORNER_MARGIN, CORNER_MARGIN)) {
        @Override public boolean enabled() { return mode != Mode.OFF; }
        @Override public boolean active(MinecraftClient client) {
            return !client.options.hudHidden && cornerShowing();
        }
        @Override public Size measure(boolean preview) {
            String v = cornerText(preview);
            return new Size(Fonts.hudWidth(LABEL) + Fonts.hudWidth(v.replaceAll("[0-9]", "8")), Fonts.height(Fonts.BODY));
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            String v = cornerText(f.preview());
            int labelW = Fonts.hudWidth(LABEL);
            Fonts.drawHud(ctx, LABEL, 0, 0, Theme.HUD_WHITE);
            Fonts.drawHud(ctx, v, labelW, 0, Theme.HUD_VOID);
        }
    };

    private static String subtitle(boolean preview) {
        return shown != null ? shown.subtitle : VoidRift.Alert.HOUR.subtitle;
    }

    private static String cornerText(boolean preview) {
        String v = cornerValue();
        return v != null ? v : SAMPLE_CORNER;
    }

    private static final float TITLE_SCALE = 4f, SUBTITLE_SCALE = 2f;
    private static final String TITLE = "Void Rift";

    /** Subtitle top, below the title's top (a vanilla title and subtitle sit at H/2-40 and H/2+10). */
    private static final int SUBTITLE_Y = 50;

    /** Title and subtitle centred in a {@code w}-wide box, purple on a deep purple drop shadow. */
    private static void drawTitle(DrawContext ctx, String subtitle, int w, float alpha) {
        int a = Math.round(alpha * 255);
        // the game draws a colour with next to no alpha as fully opaque, so skip those frames
        if (a < 5) return;
        drawShadowed(ctx, TITLE, (w - Fonts.width(TITLE, TITLE_SCALE)) / 2, 0, a, TITLE_SCALE);
        drawShadowed(ctx, subtitle, (w - Fonts.width(subtitle, SUBTITLE_SCALE)) / 2, SUBTITLE_Y, a, SUBTITLE_SCALE);
    }

    /** One line in purple, its deep purple shadow one text pixel straight down. */
    private static void drawShadowed(DrawContext ctx, String s, int x, int y, int alpha, float scale) {
        int drop = Math.max(1, Math.round(Fonts.snap(scale)));
        Fonts.draw(ctx, s, x, y + drop, (alpha << 24) | (Theme.HUD_VOID_SHADOW & 0xFFFFFF), scale);
        Fonts.draw(ctx, s, x, y, (alpha << 24) | (Theme.HUD_VOID & 0xFFFFFF), scale);
    }

    /** Whether the corner line is up this frame, for what stacks above it. */
    public static boolean cornerShowing() {
        return active() && cornerValue() != null;
    }

    private static String cornerValue() {
        long now = System.currentTimeMillis();
        long spawn = VoidRift.nextSpawn(now);
        long left = spawn - now;
        return switch (mode) {
            case ALERT -> VoidRift.inLastHour(left) ? VoidRift.countdown(left) : null;
            case COUNTDOWN -> VoidRift.countdown(left);
            case LOCAL_TIME -> VoidRift.localTime(spawn, ZoneId.systemDefault());
            case OFF -> null;
        };
    }

    private static boolean active() {
        return mode != Mode.OFF && WorldIdentification.world == PlayerWorld.ISLE
            && !FeatureFlags.isKilled("void_rift_timer");
    }

    public static void reset() {
        shown = null;
        ringsLeft = 0;
    }
}
