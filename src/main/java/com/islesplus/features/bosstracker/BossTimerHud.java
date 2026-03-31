package com.islesplus.features.bosstracker;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

public final class BossTimerHud {

    private static final int HPAD    = 5;  // horizontal padding each side
    private static final int SPAD    = 5;  // vertical padding inside each section
    private static final int ROW_GAP = 2;  // extra px between rows
    private static final int COLOR_HEADER     = 0xFFAA8800;
    private static final int COLOR_NAME       = 0xFFCCCCCC;
    private static final int COLOR_NAME_STALE = 0xFF888888;
    private static final int COLOR_STALE      = 0xFF777777;
    private static final int COLOR_HINT       = 0xFF666666;
    private static final int COLOR_ACCENT     = 0xFFAA8800;
    private static final int COLOR_BG         = 0xAA000000;

    private BossTimerHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (!BossTracker.bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return;
        if (WorldIdentification.world != PlayerWorld.ISLE) return;
        if (BossTracker.bosses.isEmpty()) return;
        if (client.textRenderer == null) return;
        if (client.options.hudHidden || client.currentScreen != null) return;

        java.util.List<BossTracker.TrackedBoss> visible = new java.util.ArrayList<>();
        for (BossTracker.TrackedBoss b : BossTracker.bosses) {
            if (!BossTracker.hiddenBossNames.contains(b.name)) visible.add(b);
        }
        if (visible.isEmpty()) return;

        boolean neverParsed = BossTracker.lastBossaryParseMs == 0;
        var tr = client.textRenderer;
        int fontH = tr.fontHeight;
        int rowH = fontH + ROW_GAP + 1;

        // Pre-compute display strings and colours
        String[] names      = new String[visible.size()];
        String[] times      = new String[visible.size()];
        int[]    timeColors = new int[visible.size()];
        int[]    nameColors = new int[visible.size()];

        for (int i = 0; i < visible.size(); i++) {
            BossTracker.TrackedBoss boss = visible.get(i);
            boolean stale = BossTracker.isStale(boss);
            names[i]      = shorten(boss.name);
            nameColors[i] = stale && boss.state == BossTracker.BossState.COOLDOWN
                            ? COLOR_NAME_STALE : COLOR_NAME;
            switch (boss.state) {
                case UNKNOWN  -> { times[i] = "Available Now"; timeColors[i] = 0xFFBB9933; }
                case READY    -> { times[i] = "Ready";  timeColors[i] = 0xFFFFFF55; }
                case SPAWNING -> { times[i] = BossTracker.formatTime(boss.remainingMs); timeColors[i] = 0xFFFF5555; }
                case SPAWNED  -> { times[i] = "Spawned";    timeColors[i] = 0xFFFF5555; }
                default       -> { // COOLDOWN
                    times[i]      = (stale ? "~" : "") + BossTracker.formatTime(boss.remainingMs);
                    timeColors[i] = stale ? COLOR_STALE : 0xFFFFFFFF;
                }
            }
        }

        // Compute inner content width
        String header = "\u2620 World Bosses";
        int innerW = tr.getWidth(header);
        for (int i = 0; i < visible.size(); i++) {
            innerW = Math.max(innerW, tr.getWidth(names[i]) + 10 + tr.getWidth(times[i]));
        }
        if (neverParsed) innerW = Math.max(innerW, tr.getWidth("/bossary to refresh"));
        innerW = Math.max(innerW, 110);

        int panelW = innerW + HPAD * 2;

        // Two-section layout:
        //   [accent 1px] [SPAD] [header] [SPAD] [separator 1px] [SPAD] [bosses] [SPAD]
        // Header is centered between accent and separator.
        // Bosses are centered between separator and panel bottom.
        int headerSectionH = SPAD + fontH + SPAD;
        int bossContentH   = visible.size() * rowH - (ROW_GAP + 1); // strip trailing gap
        if (neverParsed) bossContentH += ROW_GAP + 1 + fontH;       // add hint row
        int bossSectionH   = SPAD + bossContentH + SPAD;
        int panelH         = 1 + headerSectionH + 1 + bossSectionH;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int bx, by;
        switch (BossTracker.hudPosition) {
            case TOP_LEFT     -> { bx = 10;                    by = 10; }
            case BOTTOM_LEFT  -> { bx = 10;                    by = screenH - 10 - panelH; }
            case BOTTOM_RIGHT -> { bx = screenW - 10 - panelW; by = screenH - 10 - panelH; }
            default           -> { bx = screenW - 10 - panelW; by = 10; } // TOP_RIGHT
        }
        int startX = bx + HPAD;

        // Background + gold top accent
        ctx.fill(bx, by, bx + panelW, by + panelH, COLOR_BG);
        ctx.fill(bx, by, bx + panelW, by + 1,      COLOR_ACCENT);

        // Header -- centered between accent and separator
        ctx.drawTextWithShadow(tr, header, startX, by + 1 + SPAD, COLOR_HEADER);

        // Separator
        int sepY = by + 1 + headerSectionH;
        ctx.fill(bx, sepY, bx + panelW, sepY + 1, 0x55AA8800);

        // Boss rows -- centered between separator and panel bottom
        int y = sepY + 1 + SPAD;
        for (int i = 0; i < visible.size(); i++) {
            ctx.drawTextWithShadow(tr, names[i], startX, y, nameColors[i]);
            int timeX = startX + innerW - tr.getWidth(times[i]);
            ctx.drawTextWithShadow(tr, times[i], timeX, y, timeColors[i]);
            y += rowH;
        }

        // Hint when no data yet
        if (neverParsed) {
            ctx.drawTextWithShadow(tr, "/bossary to refresh", startX, y, COLOR_HINT);
        }
    }

    private static String shorten(String name) {
        if (name.length() <= 14) return name;
        return name.substring(0, 13) + ".";
    }
}
