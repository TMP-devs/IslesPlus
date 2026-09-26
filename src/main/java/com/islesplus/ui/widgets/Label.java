package com.islesplus.ui.widgets;

import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Supplier;

/** Plain, non-interactive text label. Hug width by default; {@link #wrap()} fills the row and
 * wraps onto multiple lines (height computed from the given width each layout);
 * {@link #fixed(int)} lays out left-aligned at an exact width instead of hugging the text.
 * The supplier constructor re-reads (and re-measures) its text on every layout/render. */
public class Label extends Widget {
    private final Supplier<String> text;
    private final int colour;
    private final float scale;
    private boolean wrap = false;
    private Integer fixedWidth;
    private List<String> lines = List.of();

    public Label(String text, int colour, float scale) {
        this(() -> text, colour, scale);
    }

    public Label(Supplier<String> text, int colour, float scale) {
        this.text = text;
        this.colour = colour;
        this.scale = scale;
    }

    /** Wrapped line pitch: the glyph height at this scale plus the usual leading - never less than
     * LINE_H, so body-size and smaller text is spaced exactly as it always was. */
    private int lineH() { return Math.max(Metrics.LINE_H, Fonts.height(scale) + (Metrics.LINE_H - Fonts.GLYPH_H)); }

    public Label wrap() { this.wrap = true; return this; }
    public Label fixed(int width) { this.fixedWidth = width; return this; }

    @Override public int prefWidth() {
        if (wrap) return Integer.MAX_VALUE;
        if (fixedWidth != null) return fixedWidth;
        return Fonts.width(text.get(), scale);
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y;
        if (wrap) {
            this.w = width;
            lines = Fonts.wrap(text.get(), width, scale);
            this.h = Math.max(1, lines.size()) * lineH();
        } else {
            int natural = fixedWidth != null ? fixedWidth : Fonts.width(text.get(), scale);
            this.w = Math.max(1, Math.min(natural, width));   // never past the space given; render() shortens
            this.h = Fonts.height(scale);
        }
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        if (wrap) {
            for (int i = 0; i < lines.size(); i++) {
                Fonts.draw(ctx, lines.get(i), x, y + i * lineH(), colour, scale);
            }
        } else {
            String shown = text.get();
            if (Fonts.width(shown, scale) > w) shown = Fonts.ellipsize(shown, w, scale);
            Fonts.draw(ctx, shown, x, y, colour, scale);
        }
    }
}
