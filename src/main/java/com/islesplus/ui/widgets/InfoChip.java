package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/** Hug-width, non-interactive info pill: RAISED fill bevel with a SURFACE_RING ring and
 * centred dynamic text. Default is SMALL scale/CHIP_H high; {@link #large()} is BODY scale
 * and CHIP_H+2 high. {@link #fixed(int)} pins the width instead of hugging the text. */
public class InfoChip extends Widget {
    private final Supplier<String> text;
    private boolean large = false;
    private Integer fixedWidth;

    public InfoChip(Supplier<String> text) {
        this.text = text;
    }

    public InfoChip large() { this.large = true; return this; }
    public InfoChip fixed(int width) { this.fixedWidth = width; return this; }

    private float scale() { return large ? Fonts.BODY : Fonts.SMALL; }
    private Integer fixedHeight;

    /** Pins the height, e.g. to {@link Metrics#BUTTON_H} when the chip sits beside a button. */
    public InfoChip height(int height) { this.fixedHeight = height; return this; }

    private int chipHeight() {
        if (fixedHeight != null) return fixedHeight;
        return large ? Metrics.CHIP_H + 2 : Metrics.CHIP_H;
    }

    @Override public int prefWidth() {
        if (fixedWidth != null) return fixedWidth;
        return 2 * Metrics.CHIP_PAD_X + Fonts.width(text.get(), scale());
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y;
        // Never wider than the space given: a long value (a sound summary on a narrow card) is
        // shortened with an ellipsis rather than spilling past the card's edge.
        this.w = Math.max(1, Math.min(prefWidth(), width));
        this.h = chipHeight();
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.bevel(ctx, x, y, w, h, Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.SURFACE_RING);
        float scale = scale();
        int textH = Fonts.height(scale);
        String shown = text.get();
        int room = w - 2 * Metrics.CHIP_PAD_X;
        if (Fonts.width(shown, scale) > room) {
            shown = Fonts.ellipsize(shown, (int) Math.floor(Math.max(0, room) / Fonts.snap(scale)));
        }
        Fonts.drawCentered(ctx, shown, x + w / 2, y + (h - textH) / 2, Theme.TEXT_LABEL, scale);
    }
}
