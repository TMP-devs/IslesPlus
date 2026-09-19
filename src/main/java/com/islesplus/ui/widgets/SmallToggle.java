package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;

/** Small sliding on/off track (24x12, or 21x10 via {@link #mini()}) with an optional trailing
 * label and a small trailing badge tag. If the label is null/empty the widget is just the track. */
public class SmallToggle extends Widget {
    private static final int GAP = 4;
    private static final int BADGE_H = 9, BADGE_PAD_X = 3;

    private final String label;
    private final BooleanSupplier get;
    private final Runnable onToggle;
    private boolean mini = false;
    private String badge;

    public SmallToggle(String label, BooleanSupplier get, Runnable onToggle) {
        this.label = label;
        this.get = get;
        this.onToggle = onToggle;
    }

    public SmallToggle mini() { this.mini = true; return this; }
    public SmallToggle badge(String text) { this.badge = text; return this; }

    private int trackW() { return mini ? Metrics.MINI_TOGGLE_W : Metrics.SMALL_TOGGLE_W; }
    private int trackH() { return mini ? Metrics.MINI_TOGGLE_H : Metrics.SMALL_TOGGLE_H; }
    private boolean hasLabel() { return label != null && !label.isEmpty(); }
    private boolean hasBadge() { return badge != null && !badge.isEmpty(); }
    private int badgeWidth() { return Fonts.width(badge, Fonts.SMALL); }

    @Override public int prefWidth() {
        int pw = trackW();
        if (hasLabel()) pw += GAP + Fonts.width(label);
        if (hasBadge()) pw += GAP + badgeWidth();
        return pw;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y;
        this.w = prefWidth();
        int h = trackH();
        if (hasLabel()) h = Math.max(h, Fonts.height(Fonts.BODY));
        if (hasBadge()) h = Math.max(h, BADGE_H);
        this.h = h;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean on = get.getAsBoolean();
        int tw = trackW(), th = trackH();
        int trackY = y + (h - th) / 2;
        int[] c = on ? ToggleColors.ON : ToggleColors.TRACK_OFF;
        Draw.bevel(ctx, x, trackY, tw, th, c[ToggleColors.FILL], c[ToggleColors.LIT], c[ToggleColors.SHADE], c[ToggleColors.RING]);

        // The knob fills its whole half of the track: only its own one-pixel ink ring separates it
        // from the track edge, so at small GUI scales it reads as a solid block, not a floating dot.
        int knobW = tw / 2 - 2, knobH = th - 2;
        int knobY = trackY + 1;
        int knobX = on ? x + tw - 1 - knobW : x + 1;
        int knobFill = on ? Theme.CREAM : Theme.KNOB_OFF;
        Draw.ring(ctx, knobX, knobY, knobW, knobH, Theme.INK);
        ctx.fill(knobX, knobY, knobX + knobW, knobY + knobH, knobFill);
        ctx.fill(knobX, knobY, knobX + knobW, knobY + 1, 0x59FFFFFF);
        ctx.fill(knobX, knobY + knobH - 1, knobX + knobW, knobY + knobH, 0x59000000);

        int curX = x + tw;
        if (hasLabel()) {
            curX += GAP;
            int textH = Fonts.height(Fonts.BODY);
            Fonts.draw(ctx, label, curX, y + (h - textH) / 2, Theme.TEXT_LABEL, Fonts.BODY);
            curX += Fonts.width(label);
        }
        if (hasBadge()) {
            curX += GAP;
            int bw = badgeWidth();
            int by = y + (h - BADGE_H) / 2;
            // Plain small oxblood text, the same BETA tag the card headers use - not a chip.
            int textH = Fonts.height(Fonts.SMALL);
            Fonts.draw(ctx, badge, curX, y + (h - textH) / 2, Theme.OXBLOOD, Fonts.SMALL);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        IslesClient.playMenuClickSound();
        onToggle.run();
        return true;
    }
}
