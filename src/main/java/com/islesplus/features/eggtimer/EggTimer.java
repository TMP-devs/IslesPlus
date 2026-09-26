package com.islesplus.features.eggtimer;

import com.islesplus.features.rankcalculator.TabListReader;
import com.islesplus.features.voidrift.VoidRiftTimer;
import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudLayout;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.HudBanner;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

/**
 * Eggs ({@link Eggs}): at 4 AM on the Isles clock, "Eggs spawn in about 5 min" under the boss
 * bars and a sound; then "[egg]: 4:59" counting down in the bottom-right corner, and "[egg]
 * Eggs spawning" from 6 AM until the window closes at 10 - or, once the server says "PET NESTS!",
 * "[egg] Eggs spawned" for 30 seconds and then nothing until the next day's warning. Isles only.
 */
public final class EggTimer {
    public static boolean eggTimerEnabled = true;
    public static SoundConfig soundConfig = new SoundConfig("minecraft:entity.chicken.egg", 1.0f, 1.0f);

    /** The tab is read twice a second; its clock ticks every 2.5 s. */
    private static final int READ_EVERY_TICKS = 10;
    private static final long WARNING_SHOWN_MS = 5_000L;
    private static final String WARNING = "Eggs spawn in about 5 min";
    /** The icon at 12 px (an item is 16), beside 8 px text; the row sits this far above the
     * Void Rift line when that is up. */
    private static final int ICON = 12, GAP = 3, COLON_GAP = 1, ROW_GAP = 3;
    private static final ItemStack EGG = new ItemStack(Items.EGG);

    private static Eggs.Clock clock = new Eggs.Clock();
    private static final Eggs.Warning warning = new Eggs.Warning();
    /** Kept across joins: a relog inside the window must not bring the countdown back. */
    private static final Eggs.Nests nests = new Eggs.Nests();
    private static long warnedAtMs = -1;
    private static int ticks;

    private EggTimer() {}

    public static void tick(MinecraftClient client) {
        if (!active() || client.player == null) return;
        if (ticks++ % READ_EVERY_TICKS == 0) {
            int minute = Eggs.minuteOfDay(TabListReader.lines(client));
            if (minute >= 0) clock.update(minute, Util.getMeasuringTimeMs());
        }
        long position = clock.position(Util.getMeasuringTimeMs());
        if (position >= 0 && warning.update(position)) {
            warnedAtMs = Util.getMeasuringTimeMs();
            ModSounds.playConfig(client, soundConfig);
        }
    }

    /** What the corner row shows now: a label (": " for the countdown) and a value; null = nothing. */
    private record Row(String label, String value) {}

    private static Row liveRow() {
        long now = Util.getMeasuringTimeMs();
        long position = clock.position(now);
        Eggs.Phase phase = position < 0 ? Eggs.Phase.NONE : Eggs.phase(position);
        return switch (nests.view(phase, now)) {
            case COUNTDOWN -> new Row(": ", Eggs.countdown(Eggs.msUntilSpawn(position)));
            case SPAWNING -> new Row("", "Eggs spawning");
            case SPAWNED -> new Row("", "Eggs spawned");
            case NONE -> null;
        };
    }

    private static Row row(boolean preview) {
        Row r = liveRow();
        return r != null ? r : new Row(": ", "4:59");
    }

    /** "[egg]: 4:59" in the bottom-right corner by default, just above the Void Rift line while
     * that line is up (only while both are where they started). */
    public static final HudElement ROW_ELEMENT = new HudElement("egg_timer", "Egg Timer",
        new HudPlacement(HudAnchor.END, HudAnchor.END, VoidRiftTimer.CORNER_MARGIN, VoidRiftTimer.CORNER_MARGIN)) {
        @Override public HudPlacement defaults() {
            HudPlacement p = super.defaults();
            if (VoidRiftTimer.cornerShowing() && !HudLayout.isCustom(VoidRiftTimer.CORNER_ELEMENT)) {
                p.oy += Fonts.height(Fonts.BODY) + ROW_GAP;
            }
            return p;
        }
        @Override public boolean enabled() { return eggTimerEnabled; }
        @Override public boolean active(MinecraftClient client) {
            return EggTimer.active() && !client.options.hudHidden && liveRow() != null;
        }
        @Override public Size measure(boolean preview) {
            Row r = row(preview);
            return new Size(ICON + gapAfterIcon(r) + Fonts.hudWidth(r.label()) + Fonts.hudWidth(r.value().replaceAll("[0-9]", "8")), ICON);
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            Row r = row(f.preview());
            int textY = (ICON - Fonts.height(Fonts.BODY)) / 2;
            ctx.getMatrices().pushMatrix();
            try {
                ctx.getMatrices().scale(ICON / 16f, ICON / 16f);
                ctx.drawItem(EGG, 0, 0);
            } finally {
                ctx.getMatrices().popMatrix();
            }
            int labelX = ICON + gapAfterIcon(r);
            if (!r.label().isEmpty()) Fonts.drawHud(ctx, r.label(), labelX, textY, Theme.HUD_MUTED);
            Fonts.drawHud(ctx, r.value(), labelX + Fonts.hudWidth(r.label()), textY, Theme.HUD_GOOD);
        }
    };

    /** "[egg]: 4:59" - the colon belongs to the icon, so it sits right against it. */
    private static int gapAfterIcon(Row r) { return r.label().startsWith(":") ? COLON_GAP : GAP; }

    private static final float WARNING_SCALE = 2f;

    /** "Eggs spawn in about 5 min" for 5 s, under the boss bars by default. */
    public static final HudElement WARNING_ELEMENT = new HudElement("egg_warning", "Egg Warning",
        new HudPlacement(HudAnchor.CENTER, HudAnchor.START, 0, 12)) {
        @Override public HudPlacement defaults() {
            HudPlacement p = super.defaults();
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.inGameHud != null) p.oy = HudBanner.underBossBars(c, c.getWindow().getScaledHeight());
            return p;
        }
        @Override public boolean enabled() { return eggTimerEnabled; }
        @Override public boolean active(MinecraftClient client) {
            return EggTimer.active() && !client.options.hudHidden
                && warnedAtMs >= 0 && Util.getMeasuringTimeMs() - warnedAtMs < WARNING_SHOWN_MS;
        }
        @Override public Size measure(boolean preview) {
            return new Size(Fonts.hudWidth(WARNING, WARNING_SCALE), Fonts.height(WARNING_SCALE) + 2);
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            Fonts.drawHud(ctx, WARNING, 1, 1, Theme.HUD_WARN, WARNING_SCALE);
        }
    };

    /** The server's "PET NESTS!" line: the eggs are out, so the countdown is done with. */
    public static void onMessage(String text) {
        if (eggTimerEnabled && Eggs.isNestsMessage(text)) nests.heard(Util.getMeasuringTimeMs());
    }

    private static boolean active() {
        return eggTimerEnabled && WorldIdentification.world == PlayerWorld.ISLE
            && !FeatureFlags.isKilled("egg_timer");
    }

    /** On joining: the clock is read afresh. */
    public static void reset() {
        clock = new Eggs.Clock();
        warnedAtMs = -1;
    }
}
