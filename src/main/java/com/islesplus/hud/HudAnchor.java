package com.islesplus.hud;

/**
 * Pure maths for where a HUD element sits. An element is pinned to one of three spots on each
 * axis - the start edge (0), the centre (1) or the end edge (2) - plus a pixel offset from it
 * (from the edge inward for 0 and 2, from centre for 1). That keeps a panel dragged into the
 * bottom-right corner in that corner, the same distance from the edges, at any window size or
 * GUI scale, and a panel that grows (more boss rows) grows away from the edge it is pinned to.
 */
public final class HudAnchor {
    public static final int START = 0, CENTER = 1, END = 2;

    private HudAnchor() {}

    /** Top-left coordinate on one axis of a {@code size}-long element pinned at {@code anchor} + {@code offset}. */
    public static int resolve(int anchor, int offset, int screen, int size) {
        return switch (anchor) {
            case START -> offset;
            case CENTER -> (screen - size) / 2 + offset;
            default -> screen - size - offset;
        };
    }

    /** Keeps an element on screen; one bigger than the screen is pinned to the start edge. */
    public static int clamp(int pos, int screen, int size) {
        return Math.max(0, Math.min(pos, screen - size));
    }

    /** The spot an element at {@code pos} is nearest to: whichever third of the screen its centre is in. */
    public static int anchorFor(int pos, int screen, int size) {
        double centre = pos + size / 2.0;
        if (centre < screen / 3.0) return START;
        if (centre > screen * 2 / 3.0) return END;
        return CENTER;
    }

    /** The offset that makes {@link #resolve} give back {@code pos} for this anchor. */
    public static int offsetFor(int anchor, int pos, int screen, int size) {
        return switch (anchor) {
            case START -> pos;
            case CENTER -> pos - (screen - size) / 2;
            default -> screen - size - pos;
        };
    }

    /** Size after scaling, rounded up so nothing drawn at the far edge is cut off. */
    public static int scaled(int size, float scale) {
        return (int) Math.ceil(size * scale);
    }
}
