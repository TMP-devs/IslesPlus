package com.islesplus.hud;

import com.google.gson.JsonObject;

/**
 * Where one HUD element sits and how big it is drawn: an anchor + offset per axis (see
 * {@link HudAnchor}) and a scale. An element that can sit on the scoreboard is stuck to it unless
 * {@link #detached}; while stuck the anchors are unused and it sits right on top of the scoreboard.
 */
public final class HudPlacement {
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 3f;

    public int ax, ay, ox, oy;
    public float scale = 1f;
    /** Only for an element that can sit on the scoreboard: placed freely instead. */
    public boolean detached = false;
    /** 0 - 1: how solid the element's background panel is (only for one that has one, see
     * {@link HudElement#hasBackgroundOpacity}). 0 = no background, 1 = the normal one. */
    public float opacity = 1f;

    public HudPlacement(int ax, int ay, int ox, int oy) {
        this.ax = ax; this.ay = ay; this.ox = ox; this.oy = oy;
    }

    public HudPlacement copy() {
        HudPlacement p = new HudPlacement(ax, ay, ox, oy);
        p.scale = scale;
        p.detached = detached;
        p.opacity = opacity;
        return p;
    }

    public boolean sameAs(HudPlacement o) {
        return o != null && ax == o.ax && ay == o.ay && ox == o.ox && oy == o.oy && scale == o.scale && detached == o.detached && opacity == o.opacity;
    }

    public static float clampScale(float s) {
        // Two decimals are plenty and keep the saved json tidy.
        float c = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        return Math.round(c * 100f) / 100f;
    }

    public static float clampOpacity(float o) {
        float c = Math.max(0f, Math.min(1f, o));
        return Math.round(c * 100f) / 100f;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("ax", ax);
        o.addProperty("ay", ay);
        o.addProperty("ox", ox);
        o.addProperty("oy", oy);
        o.addProperty("scale", scale);
        if (detached) o.addProperty("detached", true);
        if (opacity < 1f) o.addProperty("opacity", opacity);
        return o;
    }

    /** Null when the json is not a usable placement. */
    public static HudPlacement fromJson(JsonObject o) {
        try {
            HudPlacement p = new HudPlacement(
                clampAnchor(o.get("ax").getAsInt()), clampAnchor(o.get("ay").getAsInt()),
                o.get("ox").getAsInt(), o.get("oy").getAsInt());
            p.scale = o.has("scale") ? clampScale(o.get("scale").getAsFloat()) : 1f;
            p.detached = o.has("detached") && o.get("detached").getAsBoolean();
            p.opacity = o.has("opacity") ? clampOpacity(o.get("opacity").getAsFloat()) : 1f;
            return p;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int clampAnchor(int a) { return Math.max(HudAnchor.START, Math.min(HudAnchor.END, a)); }
}
