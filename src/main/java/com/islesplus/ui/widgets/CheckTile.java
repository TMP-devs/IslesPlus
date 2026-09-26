package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;

/** Full-width selectable grid tile (18 high): a bevelled cell with a small checkbox indicator
 * at the left and the label after it. Optional small "Beta" tag at the right, and an optional
 * disabled state (remote kill switch for a single option): greyed, unticked, not clickable. */
public class CheckTile extends Widget {
    private static final int PAD = 4;
    private static final String BETA_LABEL = "Beta", DISABLED_LABEL = "Disabled";

    private final String label;
    private final BooleanSupplier get;
    private final Runnable onClick;
    private boolean beta;
    private BooleanSupplier disabled;

    public CheckTile(String label, BooleanSupplier get, Runnable onClick) {
        this.label = label;
        this.get = get;
        this.onClick = onClick;
    }

    /** Small "Beta" tag at the tile's right edge (oxblood on an idle tile, cream on an active one). */
    public CheckTile beta() { this.beta = true; return this; }
    /** Lets something else have the last word on the BETA tag (e.g. the remote features json):
     * given the built-in choice, returns whether to show it. Checked every frame. */
    public CheckTile betaFrom(java.util.function.UnaryOperator<Boolean> resolver) { this.betaResolver = resolver; return this; }
    private java.util.function.UnaryOperator<Boolean> betaResolver = b -> b;
    private boolean showsBeta() { return betaResolver.apply(beta); }

    /** While the supplier is true the tile is greyed out, shows DISABLED and ignores clicks. */
    public CheckTile disabledWhen(BooleanSupplier disabled) { this.disabled = disabled; return this; }

    private boolean isDisabled() { return disabled != null && disabled.getAsBoolean(); }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    /** Width at which the whole label (and any tag) shows without an ellipsis. */
    public int naturalWidth() {
        String tag = isDisabled() ? DISABLED_LABEL : showsBeta() ? BETA_LABEL : null;
        int tagW = tag == null ? 0 : PAD + Fonts.width(tag, Fonts.SMALL);
        return PAD + Metrics.TILE_BOX + PAD + Fonts.width(label) + tagW + PAD;
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width; this.h = Metrics.TILE_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean off = isDisabled();
        boolean active = !off && get.getAsBoolean();
        int textColour;
        if (off) {
            Draw.bevel(ctx, x, y, w, h, Theme.DISABLED, Theme.DISABLED_LIT, Theme.DISABLED_SHADE, Theme.DISABLED_RING);
            textColour = Theme.DISABLED_TEXT;
        } else {
            int[] c = active ? ToggleColors.ON : ToggleColors.IDLE;
            Draw.bevel(ctx, x, y, w, h, c[ToggleColors.FILL], c[ToggleColors.LIT], c[ToggleColors.SHADE], c[ToggleColors.RING]);
            textColour = active ? Theme.CREAM : Theme.TEXT_LABEL;
        }

        int[] box = active ? ToggleColors.BOX_ACTIVE : ToggleColors.BOX_IDLE;
        int boxSize = Metrics.TILE_BOX;
        int boxX = x + PAD, boxY = y + (h - boxSize) / 2;
        ctx.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, off ? Theme.DISABLED_SHADE : box[0]);
        Draw.ring(ctx, boxX, boxY, boxSize, boxSize, off ? Theme.DISABLED_RING : box[1]);
        if (active) Draw.tick(ctx, boxX, boxY, boxSize, Theme.TICK);

        String tag = off ? DISABLED_LABEL : showsBeta() ? BETA_LABEL : null;
        int tagW = tag == null ? 0 : Fonts.width(tag, Fonts.SMALL);
        int right = x + w - PAD;
        if (tag != null) {
            int tagColour = off ? Theme.DISABLED_TEXT : active ? Theme.CREAM : Theme.OXBLOOD;
            Fonts.draw(ctx, tag, right - tagW, y + (h - Fonts.height(Fonts.SMALL)) / 2, tagColour, Fonts.SMALL);
            right -= tagW + PAD;
        }

        int labelX = boxX + boxSize + PAD;
        String shown = Fonts.ellipsize(label, Math.max(0, right - labelX));
        Fonts.draw(ctx, shown, labelX, y + (h - Fonts.height(Fonts.BODY)) / 2, textColour);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        if (isDisabled()) return true;   // swallow the click, change nothing
        IslesClient.playMenuClickSound();
        onClick.run();
        return true;
    }
}
