package com.islesplus.features.bosstracker;

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

    private BossTimerHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.textRenderer == null) return;
        if (client.options.hudHidden || client.currentScreen != null) return;

        if (!BossTracker.bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return;
        if (WorldIdentification.world != PlayerWorld.ISLE) return;
        if (BossTracker.bosses.isEmpty()) return;

        java.util.List<BossTracker.TrackedBoss> visible = new java.util.ArrayList<>();
        for (BossTracker.TrackedBoss b : BossTracker.bosses) {
            if (!BossTracker.hiddenBossNames.contains(b.name)) visible.add(b);
        }
        if (visible.isEmpty()) return;

        boolean neverParsed = BossTracker.lastBossaryParseMs == 0;
        // The container is laid out for body-size (8 px) text and keeps that size; only the text
        // drawn inside it is larger, centred on the same row lines.
        int fontH = Math.round(Fonts.GLYPH_H * TEXT_SCALE);
        int rowH = Fonts.GLYPH_H + ROW_GAP;
        int textLift = (fontH - Fonts.GLYPH_H) / 2;

        // Pre-compute display strings and colours
        String[] names      = new String[visible.size()];
        String[] times      = new String[visible.size()];
        int[]    timeColors = new int[visible.size()];
        int[]    nameColors = new int[visible.size()];

        for (int i = 0; i < visible.size(); i++) {
            BossTracker.TrackedBoss boss = visible.get(i);
            boolean stale = BossTracker.isStale(boss);
            names[i]      = shorten(boss.name);
            nameColors[i] = Theme.HUD_TEXT;
            switch (boss.state) {
                case UNKNOWN  -> { times[i] = "Available Now"; timeColors[i] = Theme.HUD_LIME; }
                case READY    -> { times[i] = "Ready";  timeColors[i] = Theme.HUD_GOLD; }
                case SPAWNING -> { times[i] = BossTracker.formatTime(boss.remainingMs); timeColors[i] = Theme.HUD_RED; }
                case SPAWNED  -> { times[i] = "Spawned";    timeColors[i] = Theme.HUD_RED; }
                default       -> { // COOLDOWN: an estimate ("~") is dimmed, name and all
                    times[i]      = (stale ? "~" : "") + BossTracker.formatTime(boss.remainingMs);
                    timeColors[i] = stale ? Theme.HUD_DIM : Theme.HUD_TEXT;
                    if (stale) nameColors[i] = Theme.HUD_DIM;
                }
            }
        }

        String header = "World Bosses";
        String hint = "/bossary to refresh";
        int innerW = Math.max(MIN_INNER_W, ICON + ICON_GAP + Fonts.width(header));
        for (int i = 0; i < visible.size(); i++) {
            innerW = Math.max(innerW, Fonts.width(names[i]) + NAME_TIME_GAP + Fonts.width(times[i]));
            // ...and only ever wider than that if the larger text would otherwise collide
            innerW = Math.max(innerW, textW(names[i]) + MIN_NAME_TIME_GAP + textW(times[i]));
        }
        if (neverParsed) innerW = Math.max(innerW, textW(hint));
        innerW = Math.max(innerW, ICON + ICON_GAP + Fonts.width(header, HEADER_SCALE));

        int panelW = innerW + PAD_X * 2;
        int headerFontH = Fonts.height(HEADER_SCALE);
        int headerH = HEADER_PAD_Y + Math.max(ICON, headerFontH) + HEADER_PAD_Y;
        int rowCount = visible.size() + (neverParsed ? 1 : 0);
        int rowsH = ROWS_PAD_TOP + rowCount * rowH - ROW_GAP + ROWS_PAD_BOTTOM;
        int panelH = headerH + rowsH;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int bx, by;
        switch (BossTracker.hudPosition) {
            case TOP_LEFT     -> { bx = 10;                    by = 10; }
            case BOTTOM_LEFT  -> { bx = 10;                    by = screenH - 10 - panelH; }
            case BOTTOM_RIGHT -> { bx = screenW - 10 - panelW; by = screenH - 10 - panelH; }
            default           -> { bx = screenW - 10 - panelW; by = 10; } // TOP_RIGHT
        }

        // Panel: translucent ink, 1 px bevel, ink ring outside
        Draw.ring(ctx, bx, by, panelW, panelH, PANEL_RING);
        ctx.fill(bx, by, bx + panelW, by + panelH, PANEL_BG);
        ctx.fill(bx, by + panelH - 1, bx + panelW, by + panelH, PANEL_SHADE);
        ctx.fill(bx, by, bx + 1, by + panelH, PANEL_LIT_SIDE);
        ctx.fill(bx + panelW - 1, by, bx + panelW, by + panelH, PANEL_SHADE_SIDE);

        // Header band: skull, then the title in the lifted oxblood
        ctx.fill(bx, by, bx + panelW, by + headerH, HEADER_BG);
        ctx.fill(bx, by, bx + panelW, by + 1, HEADER_LIT);
        ctx.fill(bx, by + headerH - 1, bx + panelW, by + headerH, HEADER_SHADE);
        int startX = bx + PAD_X;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SKULL, startX, by + (headerH - ICON) / 2, 0, 0, ICON, ICON, ICON, ICON);
        Fonts.draw(ctx, header, startX + ICON + ICON_GAP, by + (headerH - headerFontH) / 2, Theme.HUD_TITLE, HEADER_SCALE);

        // Boss rows: name left, time right
        int y = by + headerH + ROWS_PAD_TOP;
        for (int i = 0; i < visible.size(); i++) {
            text(ctx, names[i], startX, y - textLift, nameColors[i]);
            text(ctx, times[i], startX + innerW - textW(times[i]), y - textLift, timeColors[i]);
            y += rowH;
        }

        // Hint when no data yet
        if (neverParsed) text(ctx, hint, startX, y - textLift, Theme.HUD_DIM);
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
