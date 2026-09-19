package com.islesplus.ui.widgets;

import com.islesplus.ui.ColorMath;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.SliderMath;
import com.islesplus.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** 12px-high lightness picker rail: a black-to-colour-to-white strip at the current hue AND
 * saturation, inside a well 2px inset on every side, with a small cream knob marking the
 * lightness.
 * <p>The strip is a genuine preview of what each lightness would produce, so it takes the live
 * saturation rather than assuming 1: after a grey hex is typed (which zeroes saturation) the
 * rail correctly shows a black-to-white ramp instead of a colour ramp the value cannot reach.
 * {@link HueRail} is different — it is a hue picker, so its strip stays at saturation 1 and
 * lightness .5 whatever the stored colour is. */
public class BrightnessRail extends DragTrack {
    private static final int KNOB_W = 6;
    private static final int PAD = 2;

    private final Supplier<Float> hue01;
    private final Supplier<Float> sat01;
    private final Supplier<Float> getLight01;
    private final Consumer<Float> setLight01;

    public BrightnessRail(Supplier<Float> hue01, Supplier<Float> sat01,
                           Supplier<Float> getLight01, Consumer<Float> setLight01, Runnable onRelease) {
        super(onRelease);
        this.hue01 = hue01;
        this.sat01 = sat01;
        this.getLight01 = getLight01;
        this.setLight01 = setLight01;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = Metrics.RAIL_THIN_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.well(ctx, x, y, w, h, Theme.INK_DEEP);

        int stripX = x + PAD, stripY = y + PAD;
        int n = w - 2 * PAD, stripH = h - 2 * PAD;
        float hue = hue01.get();
        float sat = sat01.get();
        int denom = Math.max(1, n - 1);
        for (int i = 0; i < n; i++) {
            int colour = ColorMath.hslToRgb(hue, sat, i / (float) denom) | 0xFF000000;
            ctx.fill(stripX + i, stripY, stripX + i + 1, stripY + stripH, colour);
        }

        float f = getLight01.get();
        int knobX = stripX + Math.round(f * n) - KNOB_W / 2;
        knobX = Math.max(x, Math.min(x + w - KNOB_W, knobX));
        int knobY = y - PAD;
        int knobH = h + 2 * PAD;
        Draw.bevel(ctx, knobX, knobY, KNOB_W, knobH, Theme.CREAM, 0xFFFFFFFF, 0xFFC9B58A, Theme.INK_DEEP);
    }

    @Override void applyValue(double mx) {
        setLight01.accept(SliderMath.fraction(mx, x + PAD, w - 2 * PAD));
    }
}
