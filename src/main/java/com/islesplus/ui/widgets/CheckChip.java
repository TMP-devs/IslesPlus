package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.BooleanSupplier;

/** Hug-width selectable chip (13 high): a bevelled pill with a small checkbox indicator
 * and a label. */
public class CheckChip extends Widget {
    private final String label;
    private final BooleanSupplier get;
    private final Runnable onClick;

    public CheckChip(String label, BooleanSupplier get, Runnable onClick) {
        this.label = label;
        this.get = get;
        this.onClick = onClick;
    }

    @Override public int prefWidth() {
        return Metrics.CHIP_PAD_X * 3 + Metrics.CHIP_BOX + Fonts.width(label);
    }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = prefWidth(); this.h = Metrics.CHIP_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean active = get.getAsBoolean();
        int[] c = active ? ToggleColors.ON : ToggleColors.IDLE;
        int[] box = active ? ToggleColors.BOX_ACTIVE : ToggleColors.BOX_IDLE;
        int textColour = active ? Theme.CREAM : Theme.TEXT_LABEL;

        Draw.bevel(ctx, x, y, w, h, c[ToggleColors.FILL], c[ToggleColors.LIT], c[ToggleColors.SHADE], c[ToggleColors.RING]);

        int boxSize = Metrics.CHIP_BOX;
        int boxX = x + Metrics.CHIP_PAD_X, boxY = y + (h - boxSize) / 2;
        ctx.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, box[0]);
        Draw.ring(ctx, boxX, boxY, boxSize, boxSize, box[1]);
        if (active) Draw.tick(ctx, boxX, boxY, boxSize, Theme.TICK);

        int labelX = boxX + boxSize + Metrics.CHIP_PAD_X;
        int textH = Fonts.height(Fonts.BODY);
        Fonts.draw(ctx, label, labelX, y + (h - textH) / 2, textColour);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        IslesClient.playMenuClickSound();
        onClick.run();
        return true;
    }
}
