package com.islesplus.ui.widgets;

import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

/** Full-width 1 px hairline that separates two sections of a drawer. */
public class Rule extends Widget {
    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width; this.h = 1;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(x, y, x + w, y + 1, Theme.SURFACE_SHADE);
    }
}
