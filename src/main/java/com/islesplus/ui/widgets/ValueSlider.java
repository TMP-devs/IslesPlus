package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.SliderMath;
import com.islesplus.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Horizontal 14px-high value slider: a dark well track, a fill bar to the current value
 * and a 7px-wide raised knob straddling the track. */
public class ValueSlider extends DragTrack {
    private final Supplier<Float> get;
    private final Consumer<Float> set;
    private final float min, max, step;

    private int fillC = Theme.OXBLOOD, litC = Theme.OXBLOOD_LIT, shadeC = Theme.OXBLOOD_SHADE;

    public ValueSlider(Supplier<Float> get, Consumer<Float> set, float min, float max, float step, Runnable onRelease) {
        super(onRelease);
        this.get = get;
        this.set = set;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    /** Overrides the default OXBLOOD fill trio (e.g. the PITCH_* trio). */
    public ValueSlider fill(int fill, int lit, int shade) {
        this.fillC = fill;
        this.litC = lit;
        this.shadeC = shade;
        return this;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = Metrics.SLIDER_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.well(ctx, x, y, w, h, Theme.INK_DEEP);

        // Clamped once, then shared by the bar and the knob: a stored value outside [min, max]
        // (an older config, or a range that shrank) must not paint the fill past the widget.
        float f = Math.max(0f, Math.min(1f, SliderMath.unlerp(get.get(), min, max)));
        int barW = Math.round(f * w);
        if (barW > 0) {
            ctx.fill(x, y, x + barW, y + h, fillC);
            ctx.fill(x, y, x + barW, y + 1, litC);
            ctx.fill(x, y + h - 1, x + barW, y + h, shadeC);
        }

        int knobX = x + Math.round(f * w) - Metrics.KNOB_W / 2;
        knobX = Math.max(x, Math.min(x + w - Metrics.KNOB_W, knobX));
        int knobY = y - 1;
        int knobH = h + 2;
        Draw.bevel(ctx, knobX, knobY, Metrics.KNOB_W, knobH, Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.INK_DEEP);
    }

    @Override void applyValue(double mx) {
        float f = SliderMath.fraction(mx, x, w);
        float v = SliderMath.lerp(f, min, max);
        set.accept(SliderMath.snap(v, min, max, step));
    }
}
