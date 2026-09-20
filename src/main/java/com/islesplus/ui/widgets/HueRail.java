package com.islesplus.ui.widgets;

import com.islesplus.ui.ColorMath;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.SliderMath;
import com.islesplus.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** 15px-high hue picker rail: a full-saturation, mid-lightness hue strip inside a well,
 * 2px inset on every side, with a small cream knob marking the current hue. */
public class HueRail extends DragTrack {
    private static final int KNOB_W = 6;
    private static final int PAD = 2;

    private final Supplier<Float> getHue01;
    private final Consumer<Float> setHue01;

    public HueRail(Supplier<Float> getHue01, Consumer<Float> setHue01, Runnable onRelease) {
        super(onRelease);
        this.getHue01 = getHue01;
        this.setHue01 = setHue01;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = Metrics.RAIL_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.well(ctx, x, y, w, h, Theme.INK_DEEP);

        int stripX = x + PAD, stripY = y + PAD;
        int n = w - 2 * PAD, stripH = h - 2 * PAD;
        for (int i = 0; i < n; i++) {
            int colour = ColorMath.hslToRgb(i / (float) n, 1f, .5f) | 0xFF000000;
            ctx.fill(stripX + i, stripY, stripX + i + 1, stripY + stripH, colour);
        }

        float f = getHue01.get();
        int knobX = stripX + Math.round(f * n) - KNOB_W / 2;
        knobX = Math.max(x, Math.min(x + w - KNOB_W, knobX));
        int knobY = y - PAD;
        int knobH = h + 2 * PAD;
        Draw.bevel(ctx, knobX, knobY, KNOB_W, knobH, Theme.CREAM, Theme.KNOB_LIT, Theme.KNOB_SHADE, Theme.INK_DEEP);
    }

    @Override void applyValue(double mx) {
        setHue01.accept(SliderMath.fraction(mx, x + PAD, w - 2 * PAD));
    }
}
