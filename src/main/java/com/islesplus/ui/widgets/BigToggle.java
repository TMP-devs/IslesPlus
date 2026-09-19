package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Sliding on/off track split into two equal halves: the knob fills the selected half edge to
 * edge, and the ON/OFF text is centred in the other half so it never touches the knob.
 */
public class BigToggle extends Widget {
    /** Track pixels left visible around the knob's 1-px ring. */
    private static final int KNOB_INSET = 2;

    private final BooleanSupplier get;
    private final Consumer<Boolean> set;

    public BigToggle(BooleanSupplier get, Consumer<Boolean> set) {
        this.get = get;
        this.set = set;
    }

    @Override public int prefWidth() { return Metrics.BIG_TOGGLE_W; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y;
        this.w = Metrics.BIG_TOGGLE_W;
        this.h = Metrics.BIG_TOGGLE_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean on = get.getAsBoolean();
        int[] c = on ? ToggleColors.ON : ToggleColors.TRACK_OFF;
        Draw.bevel(ctx, x, y, w, h, c[ToggleColors.FILL], c[ToggleColors.LIT], c[ToggleColors.SHADE], c[ToggleColors.RING]);

        int half = w / 2;
        // Knob fills its whole half (ON = right half, OFF = left half).
        int knobW = half - 2 * KNOB_INSET;
        int knobH = h - 2 * KNOB_INSET;
        int knobX = on ? x + w - KNOB_INSET - knobW : x + KNOB_INSET;
        int knobY = y + KNOB_INSET;

        // Text centred in the opposite half, clear of the knob's ring.
        int textH = Fonts.height(Fonts.SMALL);
        int textY = y + (h - textH) / 2;
        int textCx = on ? x + half / 2 : x + w - half / 2;
        Fonts.drawCentered(ctx, on ? "ON" : "OFF", textCx, textY, on ? Theme.CREAM : 0xFFF4EFE6, Fonts.SMALL);

        int knobFill = on ? Theme.CREAM : Theme.KNOB_OFF;
        Draw.ring(ctx, knobX, knobY, knobW, knobH, Theme.INK);
        ctx.fill(knobX, knobY, knobX + knobW, knobY + knobH, knobFill);
        ctx.fill(knobX, knobY, knobX + knobW, knobY + 1, 0x59FFFFFF);
        ctx.fill(knobX, knobY + knobH - 1, knobX + knobW, knobY + knobH, 0x59000000);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        IslesClient.playMenuClickSound();
        set.accept(!get.getAsBoolean());
        return true;
    }
}
