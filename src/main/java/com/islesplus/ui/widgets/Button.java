package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/** Bevelled push button in one of four kinds; centred label, optional leading "+ " glyph,
 * optional fixed-fill width, and a static or dynamic (re-measured each layout) label. */
public class Button extends Widget {
    private static final int QUIET_HOVER = 0xFFE2D3AA;

    public enum Kind { PRIMARY, SECONDARY, QUIET, ICON }

    private final Kind kind;
    private final Runnable onClick;
    private Supplier<String> label;
    private boolean small = false;
    private boolean fill = false;
    private boolean plus = false;
    private boolean pressed = false;

    public Button(String label, Kind kind, Runnable onClick) {
        this.label = () -> label;
        this.kind = kind;
        this.onClick = onClick;
    }

    public Button small() { this.small = true; return this; }
    public Button fill() { this.fill = true; return this; }
    public Button plus() { this.plus = true; return this; }
    public Button label(Supplier<String> label) { this.label = label; return this; }

    private String displayText() {
        String s = label.get();
        return plus ? "+ " + s : s;
    }

    @Override public int prefWidth() {
        if (kind == Kind.ICON) return Metrics.ICON_BUTTON;
        if (fill) return Integer.MAX_VALUE;
        return Fonts.width(displayText()) + 2 * Metrics.BUTTON_PAD_X;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y;
        if (kind == Kind.ICON) {
            this.w = Metrics.ICON_BUTTON;
            this.h = Metrics.ICON_BUTTON;
            return this.h;
        }
        this.w = fill ? width : prefWidth();
        this.h = small ? Metrics.BUTTON_SMALL_H : Metrics.BUTTON_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        int fillC, litC, shadeC, ringC, textC;
        switch (kind) {
            case PRIMARY -> {
                fillC = hover ? Theme.OXBLOOD_HOVER : Theme.OXBLOOD;
                litC = Theme.OXBLOOD_LIT; shadeC = Theme.OXBLOOD_SHADE; ringC = Theme.INK; textC = Theme.CREAM;
            }
            case SECONDARY -> {
                fillC = hover ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
                litC = Theme.SECONDARY_LIT; shadeC = Theme.SECONDARY_SHADE; ringC = Theme.SURFACE_RING; textC = Theme.TEXT_LABEL;
            }
            case QUIET -> {
                fillC = hover ? QUIET_HOVER : Theme.RAISED;
                litC = Theme.RAISED_LIT; shadeC = Theme.RAISED_SHADE; ringC = Theme.RAISED_RING; textC = Theme.TEXT_LABEL;
            }
            case ICON -> {
                if (hover) {
                    fillC = Theme.OXBLOOD; litC = Theme.OXBLOOD_LIT; shadeC = Theme.OXBLOOD_SHADE; ringC = Theme.INK; textC = Theme.CREAM;
                } else {
                    fillC = Theme.SECONDARY; litC = Theme.SECONDARY_LIT; shadeC = Theme.SECONDARY_SHADE; ringC = Theme.SURFACE_RING; textC = Theme.TEXT_LABEL;
                }
            }
            default -> throw new IllegalStateException("Unknown Kind: " + kind);
        }
        if (pressed) litC = shadeC;

        Draw.bevel(ctx, x, y, w, h, fillC, litC, shadeC, ringC);

        if (kind == Kind.ICON) {
            Draw.cross(ctx, x + w / 2, y + h / 2, textC);
        } else {
            String s = displayText();
            int textH = Fonts.height(Fonts.BODY);
            Fonts.drawCentered(ctx, s, x + w / 2, y + (h - textH) / 2, textC, Fonts.BODY);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        pressed = true;
        IslesClient.playMenuClickSound();
        onClick.run();
        return true;
    }

    @Override public void mouseReleased() { pressed = false; }
}
