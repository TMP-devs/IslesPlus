package com.islesplus.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.islesplus.screen.hudedit.HudEditScreen;
import com.islesplus.screen.hudedit.ScoreboardTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Where every {@link HudElement} goes: the player's saved placements (or each element's default),
 * resolved to a screen rectangle every frame, and the HUD pass that draws them.
 */
public final class HudLayout {
    /** Gap between the scoreboard's top edge and the bottom of an element sitting on it. */
    public static final int SCOREBOARD_GAP = 4;

    /** Screen rectangle of a placed element (scaled size) plus the scale it is drawn at. */
    public record Box(int x, int y, int w, int h, float scale, float opacity) {
        public boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private static final Map<String, HudPlacement> custom = new HashMap<>();

    /** Scoreboard stand-in the editor draws when the real one is not on screen (x, y, w, h), or null. */
    public static int[] mockScoreboard;

    private HudLayout() {}

    public static HudPlacement placement(HudElement e) {
        HudPlacement p = custom.get(e.id);
        return p != null ? p : e.defaults();
    }

    public static boolean isCustom(HudElement e) { return custom.containsKey(e.id); }

    public static void set(HudElement e, HudPlacement p) {
        if (p.sameAs(e.defaults())) custom.remove(e.id);
        else custom.put(e.id, p.copy());
    }

    public static void reset(HudElement e) { custom.remove(e.id); }

    /** Whether the element is stuck to the scoreboard right now (it can be, and is not detached). */
    public static boolean stuck(HudElement e) { return e.scoreboardAttached() && !placement(e).detached; }

    /** Gap between the scoreboard's bottom and an element stuck under it; and how far in from the
     * scoreboard's right edge that element ends (where the scoreboard's own text ends). */
    public static final int BELOW_SCOREBOARD_GAP = 3, SCOREBOARD_TEXT_RIGHT = 3;

    /** Sticks the element on top of the scoreboard, or frees it where it is now, keeping its size. */
    public static void setStuck(HudElement e, boolean stick, int screenW, int screenH) {
        HudPlacement now = placement(e);
        if (stick) {
            HudPlacement p = new HudPlacement(0, 0, 0, 0);
            p.scale = now.scale;
            p.opacity = now.opacity;
            set(e, p);
            return;
        }
        Box b = place(e, screenW, screenH, true);
        HudPlacement p = b == null ? new HudPlacement(HudAnchor.END, HudAnchor.CENTER, 4, 0)
            : placementAt(e, true, b.x(), b.y(), b.w(), b.h(), now.scale, screenW, screenH);
        p.scale = now.scale;
        p.detached = true;
        set(e, p);
    }

    public static void resetAll() { custom.clear(); }

    /** The scoreboard's bounds (x, y, w, h), the editor's stand-in if it is not drawn, else null. */
    public static int[] scoreboard() {
        if (ScoreboardTracker.valid) {
            return new int[]{ScoreboardTracker.x, ScoreboardTracker.y, ScoreboardTracker.width, ScoreboardTracker.height};
        }
        return mockScoreboard;
    }

    /** Where the element goes this frame, or null if it follows the scoreboard and there is none. */
    public static Box place(HudElement e, int screenW, int screenH, boolean preview) {
        return place(e, placement(e), e.measure(preview), screenW, screenH, preview);
    }

    public static Box place(HudElement e, HudPlacement p, HudElement.Size size, int screenW, int screenH, boolean preview) {
        float s = p.scale;
        int w = HudAnchor.scaled(size.w(), s), h = HudAnchor.scaled(size.h(), s);
        if (e.belowScoreboard() && !p.detached) {
            int[] sb = scoreboard();
            if (sb == null) return null;
            return new Box(sb[0] + sb[2] - SCOREBOARD_TEXT_RIGHT - w, sb[1] + sb[3] + BELOW_SCOREBOARD_GAP, w, h, s, p.opacity);
        }
        if (e.followsScoreboard() && !p.detached) {
            int[] sb = scoreboard();
            if (sb == null) return null;
            // Stuck: centred right on top of the scoreboard, always.
            int x = sb[0] + sb[2] / 2 - w / 2;
            int bottom = sb[1] - SCOREBOARD_GAP;
            int y = Math.max(bottom - h, -HudAnchor.scaled(e.topSlack(preview), s));
            return new Box(x, y, w, h, s, p.opacity);
        }
        int x = HudAnchor.clamp(HudAnchor.resolve(p.ax, p.ox, screenW, w), screenW, w);
        int y = HudAnchor.clamp(HudAnchor.resolve(p.ay, p.oy, screenH, h), screenH, h);
        return new Box(x, y, w, h, s, p.opacity);
    }

    /** The placement that puts an element's top-left at (x, y) at this scale (the inverse of
     * {@link #place}); {@code detached} only matters for an element that can sit on the scoreboard. */
    public static HudPlacement placementAt(HudElement e, boolean detached, int x, int y, int w, int h, float scale, int screenW, int screenH) {
        if (e.scoreboardAttached() && !detached) {
            HudPlacement p = new HudPlacement(0, 0, 0, 0);   // stuck: the scoreboard decides where
            p.scale = scale;
            p.opacity = placement(e).opacity;   // moving or resizing never touches the background
            return p;
        }
        int ax = HudAnchor.anchorFor(x, screenW, w), ay = HudAnchor.anchorFor(y, screenH, h);
        HudPlacement p = new HudPlacement(ax, ay,
            HudAnchor.offsetFor(ax, x, screenW, w), HudAnchor.offsetFor(ay, y, screenH, h));
        p.scale = scale;
        p.detached = e.scoreboardAttached();
        p.opacity = placement(e).opacity;   // moving or resizing never touches the background
        return p;
    }

    /** Draws the element into its box: translated and scaled, so it draws itself at (0, 0). */
    public static void draw(DrawContext ctx, HudElement e, Box box, int screenW, int screenH, boolean preview) {
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) box.x(), (float) box.y());
            ctx.getMatrices().scale(box.scale(), box.scale());
            e.draw(ctx, new HudElement.Frame(box.x(), box.y(), box.scale(), box.opacity(), screenW, screenH, preview));
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    /** The in-game HUD pass. Skipped while the editor is open: it draws its own previews. */
    public static void renderHud(DrawContext ctx, MinecraftClient client) {
        if (client.currentScreen instanceof HudEditScreen) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (HudElement e : HudElements.all()) {
            if (!e.inHud() || !e.active(client)) continue;
            Box box = place(e, sw, sh, false);
            if (box != null) draw(ctx, e, box, sw, sh, false);
        }
    }

    // ==============================
    // Config
    // ==============================

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        for (Map.Entry<String, HudPlacement> en : custom.entrySet()) o.add(en.getKey(), en.getValue().toJson());
        return o;
    }

    public static void load(JsonObject o) {
        custom.clear();
        if (o == null) return;
        for (Map.Entry<String, JsonElement> en : o.entrySet()) {
            if (!en.getValue().isJsonObject()) continue;
            HudPlacement p = HudPlacement.fromJson(en.getValue().getAsJsonObject());
            if (p != null) custom.put(en.getKey(), p);
        }
    }

    /** For settings saved before the editor, which only had fixed corners: keeps that corner. */
    public static void migrate(HudElement e, HudPlacement p) {
        if (!custom.containsKey(e.id)) set(e, p);
    }
}
