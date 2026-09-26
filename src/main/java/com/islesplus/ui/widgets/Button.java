package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Bevelled push button in one of four kinds; centred label, optional leading "+ " glyph,
 * optional fixed-fill width, a static or dynamic (re-measured each layout) label, and an optional
 * disabled state (greyed, ignores clicks - for an action that is on cooldown or unavailable). */
public class Button extends Widget {

    public enum Kind { PRIMARY, SECONDARY, QUIET, ICON }

    private final Kind kind;
    private final Runnable onClick;
    private Supplier<String> label;
    private boolean small = false;
    private boolean fill = false;
    private boolean plus = false;
    private boolean pressed = false;
    private BooleanSupplier disabled;

    public Button(String label, Kind kind, Runnable onClick) {
        this.label = () -> label;
        this.kind = kind;
        this.onClick = onClick;
    }

    public Button small() { this.small = true; return this; }
    public Button fill() { this.fill = true; return this; }
    public Button plus() { this.plus = true; return this; }
    public Button label(Supplier<String> label) { this.label = label; return this; }
    /** While the supplier is true the button is greyed out and ignores clicks. */
    public Button disabledWhen(BooleanSupplier disabled) { this.disabled = disabled; return this; }

    private boolean isDisabled() { return disabled != null && disabled.getAsBoolean(); }

    private String displayText() {
        String s = label.get();
        return plus ? "+ " + s : s;
    }

    @Override public int prefWidth() {
        if (kind == Kind.ICON) return Metrics.ICON_BUTTON;
        if (fill) return Integer.MAX_VALUE;
        return Fonts.controlWidth(displayText(), Fonts.BODY) + 2 * Metrics.BUTTON_PAD_X;
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
        boolean hover = contains(mouseX, mouseY) && !isDisabled();
        int fillC, litC, shadeC, ringC, textC;
        if (isDisabled()) {
            fillC = Theme.DISABLED; litC = Theme.DISABLED_LIT; shadeC = Theme.DISABLED_SHADE;
            ringC = Theme.DISABLED_RING; textC = Theme.DISABLED_TEXT;
            pressed = false;
            drawFace(ctx, fillC, litC, shadeC, ringC, textC);
            return;
        }
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
                fillC = hover ? Theme.RAISED_HOVER : Theme.RAISED;
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
        // A release can go elsewhere (the click opened a dialog or another screen): only look
        // pressed while the mouse button really is down.
        if (pressed && !Draw.leftMouseDown()) pressed = false;
        if (pressed) litC = shadeC;

        drawFace(ctx, fillC, litC, shadeC, ringC, textC);
    }

    private void drawFace(DrawContext ctx, int fillC, int litC, int shadeC, int ringC, int textC) {
        Draw.bevel(ctx, x, y, w, h, fillC, litC, shadeC, ringC);

        if (kind == Kind.ICON) {
            Draw.cross(ctx, x + w / 2, y + h / 2, textC);
        } else {
            String s = displayText();
            int textH = Fonts.height(Fonts.BODY);
            Fonts.drawControlCentered(ctx, s, x + w / 2, y + (h - textH) / 2, textC, Fonts.BODY);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        if (isDisabled()) return true;   // swallow the click, change nothing
        pressed = true;
        IslesClient.playMenuClickSound();
        onClick.run();
        return true;
    }

    @Override public void mouseReleased() { pressed = false; }
}
