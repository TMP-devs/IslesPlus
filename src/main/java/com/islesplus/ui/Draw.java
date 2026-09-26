package com.islesplus.ui;
import net.minecraft.client.gui.DrawContext;

/** Pixel-art primitives: bevelled panels, rings and small icon glyphs. */
public final class Draw {
    private Draw() {}

    /** Whether the left mouse button is physically down right now. For widgets that draw a pressed
     * state: the release event can go elsewhere (a click that opens a dialog or another screen). */
    public static boolean leftMouseDown() {
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    /** Fill + 1px top lit + 1px bottom shade inside, 1px ring outside the rect. */
    public static void bevel(DrawContext c, int x, int y, int w, int h, int fill, int lit, int shade, int ring) {
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, ring);
        c.fill(x, y, x + w, y + h, fill);
        c.fill(x, y, x + w, y + 1, lit);
        c.fill(x, y + h - 1, x + w, y + h, shade);
    }

    /** As bevel, plus 1px left/right inner edges between the lit and shade rows. */
    public static void bevel4(DrawContext c, int x, int y, int w, int h, int fill, int lit, int shade, int litSide, int shadeSide, int ring) {
        bevel(c, x, y, w, h, fill, lit, shade, ring);
        c.fill(x, y + 1, x + 1, y + h - 1, litSide);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, shadeSide);
    }

    /** Dark input well: WELL fill, WELL_SHADE top (sunken), WELL_LIT bottom, ring. */
    public static void well(DrawContext c, int x, int y, int w, int h, int ring) {
        bevel(c, x, y, w, h, Theme.WELL, Theme.WELL_SHADE, Theme.WELL_LIT, ring);
    }

    /** 1px outline just outside the rect. */
    public static void ring(DrawContext c, int x, int y, int w, int h, int colour) {
        c.fill(x - 1, y - 1, x + w + 1, y, colour);
        c.fill(x - 1, y + h, x + w + 1, y + h + 1, colour);
        c.fill(x - 1, y, x, y + h, colour);
        c.fill(x + w, y, x + w + 1, y + h, colour);
    }

    /** 1px outline just inside the rect. */
    public static void insetRing(DrawContext c, int x, int y, int w, int h, int colour) {
        c.fill(x, y, x + w, y + 1, colour);
        c.fill(x, y + h - 1, x + w, y + h, colour);
        c.fill(x, y, x + 1, y + h, colour);
        c.fill(x + w - 1, y, x + w, y + h, colour);
    }

    public enum Dir { RIGHT, DOWN, UP, LEFT }

    /** Row widths (top to bottom) for the RIGHT caret; DOWN/UP/LEFT are algebraic transposes, no matrix rotation. */
    private static final int[] CARET_W = {1, 2, 3, 4, 3, 2, 1};

    /** 7-row pixel triangle, 4 wide x 7 tall at RIGHT, centred on cx,cy. */
    public static void caret(DrawContext c, int cx, int cy, Dir d, int colour) {
        switch (d) {
            case RIGHT -> {
                int x0 = cx - 2, y0 = cy - 3;
                for (int i = 0; i < 7; i++) c.fill(x0, y0 + i, x0 + CARET_W[i], y0 + i + 1, colour);
            }
            case LEFT -> {
                int x0 = cx - 2, y0 = cy - 3;
                for (int i = 0; i < 7; i++) c.fill(x0 + 4 - CARET_W[i], y0 + i, x0 + 4, y0 + i + 1, colour);
            }
            case DOWN -> {
                int x0 = cx - 3, y0 = cy - 2;
                for (int i = 0; i < 7; i++) c.fill(x0 + i, y0, x0 + i + 1, y0 + CARET_W[i], colour);
            }
            case UP -> {
                int x0 = cx - 3, y0 = cy - 2;
                for (int i = 0; i < 7; i++) c.fill(x0 + i, y0 + 4 - CARET_W[i], x0 + i + 1, y0 + 4, colour);
            }
        }
    }

    /** 3x3 plus sign centred on cx,cy: centre row 3 wide, top and bottom 1px. */
    public static void rivet(DrawContext c, int cx, int cy, int colour) {
        c.fill(cx - 1, cy, cx + 2, cy + 1, colour);
        c.fill(cx, cy - 1, cx + 1, cy, colour);
        c.fill(cx, cy + 1, cx + 1, cy + 2, colour);
    }

    /** Pixel-art check mark inside a size x size box at (x,y), on the GUI pixel grid like the rest
     * of the kit: a short down-stroke and a longer up-stroke, both exactly 45 degrees, so every
     * column steps by exactly one pixel. Two pixels thick, with a one-pixel margin to the box. */
    public static void tick(DrawContext c, int x, int y, int size, int colour) {
        int margin = 1, thick = 2;
        int span = size - 2 * margin;                       // usable columns (and rows)
        int down = Math.max(2, Math.round(span * 0.375f));  // columns in the down-stroke, elbow included
        int up = span - down;                               // columns in the up-stroke
        int used = up + thick;                              // rows the mark occupies
        int elbowTop = y + margin + (span - used) / 2 + up; // top of the lowest (elbow) column
        for (int col = 0; col < span; col++) {
            int rise = col < down ? (down - 1 - col) : (col - (down - 1));
            int top = elbowTop - rise;
            c.fill(x + margin + col, top, x + margin + col + 1, top + thick, colour);
        }
    }

    /** 5x5 pixel X: two diagonals centred on cx,cy. */
    public static void cross(DrawContext c, int cx, int cy, int colour) {
        for (int i = -2; i <= 2; i++) {
            c.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, colour);
            c.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, colour);
        }
    }

    /** 5x5 hollow square lens with a 3px diagonal handle to the lower right, within an 8x8 box at (x,y). */
    public static void magnifier(DrawContext c, int x, int y, int colour) {
        c.fill(x, y, x + 5, y + 1, colour);
        c.fill(x, y + 4, x + 5, y + 5, colour);
        c.fill(x, y, x + 1, y + 5, colour);
        c.fill(x + 4, y, x + 5, y + 5, colour);
        for (int i = 0; i < 3; i++) c.fill(x + 5 + i, y + 5 + i, x + 6 + i, y + 6 + i, colour);
    }

    /** 7 x 7 plus, 1 px strokes, centred on (cx, cy): "empty, click to add". */
    public static void plus(DrawContext ctx, int cx, int cy, int argb) {
        ctx.fill(cx - 3, cy, cx + 4, cy + 1, argb);
        ctx.fill(cx, cy - 3, cx + 1, cy + 4, argb);
    }
}
