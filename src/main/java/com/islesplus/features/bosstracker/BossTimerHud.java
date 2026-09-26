package com.islesplus.features.bosstracker;

import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

import java.util.ArrayList;
import java.util.List;

public final class BossTimerHud {

    // World Bosses mockup, in GUI pixels (the mockup is drawn at 2x).
    private static final int PAD_X = 7, HEADER_PAD_Y = 5, ROWS_PAD_TOP = 6, ROWS_PAD_BOTTOM = 7;
    private static final int ROW_GAP = 5, ICON = 16, ICON_GAP = 6, NAME_TIME_GAP = 7, MIN_NAME_TIME_GAP = 4, MIN_INNER_W = 150;
    /** 10 px rows: 2 px above body text. Not snapped to whole screen pixels - the next even size
     * up is 12 px, which was too big. */
    private static final float TEXT_SCALE = 1.25f;
    /** The "World Bosses" title is a step larger than the rows (snapped, so it stays even). */
    private static final float HEADER_SCALE = Fonts.TITLE;
    private static final Identifier SKULL = Identifier.of("islesplus", "textures/hud/skull.png");
    private static final int PANEL_BG = 0x9E1C1815, PANEL_RING = 0xCC15100A;
    private static final int PANEL_LIT = 0xB33F3931, PANEL_SHADE = 0xB31A1714, PANEL_LIT_SIDE = 0xB33A342D, PANEL_SHADE_SIDE = 0xB31F1B17;
    private static final int HEADER_BG = 0x993F3931, HEADER_LIT = 0xBF4D463C, HEADER_SHADE = 0xCC15100A;
    private static final String HEADER = "World Bosses", HINT = "/bossary to refresh";

    /** One boss row as it is drawn. */
    private record Row(String name, String time, int nameColor, int timeColor) {}

    /** Everything the panel needs, measured once. */
    private record Model(List<Row> rows, boolean hint, int innerW, int headerH, int panelW, int panelH) {}

    public static final HudElement ELEMENT = new HudElement("boss_timers", "World Bosses",
        new HudPlacement(HudAnchor.START, HudAnchor.START, 10, 10)) {
        @Override public boolean enabled() { return BossTracker.bossTrackerEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (client.textRenderer == null) return false;
            if (client.options.hudHidden || client.currentScreen != null) return false;
            if (!BossTracker.bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return false;
            if (WorldIdentification.world != PlayerWorld.ISLE) return false;
            return !liveRows().isEmpty();
        }
        @Override public Size measure(boolean preview) {
            Model m = model(preview);
            return new Size(m.panelW(), m.panelH());
        }
        @Override public boolean hasBackgroundOpacity() { return true; }
        @Override public void draw(DrawContext ctx, Frame f) { BossTimerHud.draw(ctx, model(f.preview()), f.backgroundOpacity()); }
    };

    private BossTimerHud() {}

    private static List<Row> liveRows() {
        List<Row> rows = new ArrayList<>();
        for (BossTracker.TrackedBoss boss : BossTracker.bosses) {
            if (BossTracker.hiddenBossNames.contains(boss.name)) continue;
            boolean stale = BossTracker.isStale(boss);
            String time;
            int timeColor, nameColor = Theme.HUD_TEXT;
            switch (boss.state) {
                case UNKNOWN  -> { time = "Available Now"; timeColor = Theme.HUD_LIME; }
                case READY    -> { time = "Ready";  timeColor = Theme.HUD_GOLD; }
                case SPAWNING -> { time = BossTracker.formatTime(boss.remainingMs); timeColor = Theme.HUD_RED; }
                case SPAWNED  -> { time = "Spawned";    timeColor = Theme.HUD_RED; }
                default       -> { // COOLDOWN: an estimate is marked "(stale)" and dimmed, name and all
                    time      = BossTracker.formatTime(boss.remainingMs) + (stale ? " (stale)" : "");
                    timeColor = stale ? Theme.HUD_DIM : Theme.HUD_TEXT;
                    if (stale) nameColor = Theme.HUD_DIM;
                }
            }
            rows.add(new Row(shorten(boss.name), time, nameColor, timeColor));
        }
        return rows;
    }

    /** For the HUD editor before /bossary has loaded anything. */
    private static List<Row> sampleRows() {
        return List.of(
            new Row("Sample Boss", "Ready", Theme.HUD_TEXT, Theme.HUD_GOLD),
            new Row("Another Boss", "12:34", Theme.HUD_TEXT, Theme.HUD_TEXT),
            new Row("Third Boss", "48:10 (stale)", Theme.HUD_DIM, Theme.HUD_DIM));
    }

    private static Model model(boolean preview) {
        List<Row> rows = liveRows();
        boolean sample = preview && rows.isEmpty();
        if (sample) rows = sampleRows();
        boolean hint = !sample && BossTracker.lastBossaryParseMs == 0;

        int innerW = Math.max(MIN_INNER_W, ICON + ICON_GAP + Fonts.width(HEADER));
        for (Row r : rows) {
            innerW = Math.max(innerW, Fonts.width(r.name()) + NAME_TIME_GAP + Fonts.width(r.time()));
            // ...and only ever wider than that if the larger text would otherwise collide
            innerW = Math.max(innerW, textW(r.name()) + MIN_NAME_TIME_GAP + textW(r.time()));
        }
        if (hint) innerW = Math.max(innerW, textW(HINT));
        innerW = Math.max(innerW, ICON + ICON_GAP + Fonts.width(HEADER, HEADER_SCALE));

        int rowH = Fonts.GLYPH_H + ROW_GAP;
        int headerH = HEADER_PAD_Y + Math.max(ICON, Fonts.height(HEADER_SCALE)) + HEADER_PAD_Y;
        int rowCount = rows.size() + (hint ? 1 : 0);
        int rowsH = ROWS_PAD_TOP + rowCount * rowH - ROW_GAP + ROWS_PAD_BOTTOM;
        return new Model(rows, hint, innerW, headerH, innerW + PAD_X * 2, headerH + rowsH);
    }

    /** {@code bg} fades the panel and header band (0 = gone, 1 = normal); the skull and text stay. */
    private static void draw(DrawContext ctx, Model m, float bg) {
        // The container is laid out for body-size (8 px) text and keeps that size; only the text
        // drawn inside it is larger, centred on the same row lines.
        int fontH = Math.round(Fonts.GLYPH_H * TEXT_SCALE);
        int rowH = Fonts.GLYPH_H + ROW_GAP;
        int textLift = (fontH - Fonts.GLYPH_H) / 2;
        int panelW = m.panelW(), panelH = m.panelH(), headerH = m.headerH();

        // Panel: translucent ink, 1 px bevel, ink ring outside
        if (bg > 0f) {
            Draw.ring(ctx, 0, 0, panelW, panelH, fade(PANEL_RING, bg));
            ctx.fill(0, 0, panelW, panelH, fade(PANEL_BG, bg));
            ctx.fill(0, panelH - 1, panelW, panelH, fade(PANEL_SHADE, bg));
            ctx.fill(0, 0, 1, panelH, fade(PANEL_LIT_SIDE, bg));
            ctx.fill(panelW - 1, 0, panelW, panelH, fade(PANEL_SHADE_SIDE, bg));

            // Header band
            ctx.fill(0, 0, panelW, headerH, fade(HEADER_BG, bg));
            ctx.fill(0, 0, panelW, 1, fade(HEADER_LIT, bg));
            ctx.fill(0, headerH - 1, panelW, headerH, fade(HEADER_SHADE, bg));
        }

        // Skull, then the title in the lifted oxblood
        int startX = PAD_X;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SKULL, startX, (headerH - ICON) / 2, 0, 0, ICON, ICON, ICON, ICON);
        int headerFontH = Fonts.height(HEADER_SCALE);
        Fonts.draw(ctx, HEADER, startX + ICON + ICON_GAP, (headerH - headerFontH) / 2, Theme.HUD_TITLE, HEADER_SCALE);

        // Boss rows: name left, time right
        int y = headerH + ROWS_PAD_TOP;
        for (Row r : m.rows()) {
            text(ctx, r.name(), startX, y - textLift, r.nameColor());
            text(ctx, r.time(), startX + m.innerW() - textW(r.time()), y - textLift, r.timeColor());
            y += rowH;
        }

        // Hint when no data yet
        if (m.hint()) text(ctx, HINT, startX, y - textLift, Theme.HUD_DIM);
    }

    /** The colour with its alpha scaled by {@code f}. */
    static int fade(int argb, float f) {
        if (f >= 1f) return argb;
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int textW(String s) { return (int) Math.ceil(Fonts.width(s) * TEXT_SCALE); }

    private static void text(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) x, (float) y);
            ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);
            Fonts.draw(ctx, s, 0, 0, argb);
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    private static String shorten(String name) {
        if (name.length() <= 20) return name;
        return name.substring(0, 19) + ".";
    }
}
