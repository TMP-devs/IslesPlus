package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

/** Padded bevelled panel wrapping a single child widget: fills its row, bevels the full rect
 * around the child, and forwards every input hook straight through to the child. Handy for a
 * "sub-card" body (e.g. one row in a dynamic list) that needs its own bevelled border. */
public class Panel extends Widget {
    private final Widget child;
    private final int padding;
    private final int fill, lit, shade, ring;

    public Panel(Widget child, int padding, int fill, int lit, int shade, int ring) {
        this.child = child;
        this.padding = padding;
        this.fill = fill;
        this.lit = lit;
        this.shade = shade;
        this.ring = ring;
    }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = width;
        int innerW = Math.max(1, width - 2 * padding);
        int innerH = child.layout(x + padding, y + padding, innerW);
        this.h = innerH + 2 * padding;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.bevel(ctx, x, y, w, h, fill, lit, shade, ring);
        child.render(ctx, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) { return child.mouseClicked(mx, my, button); }
    @Override public boolean mouseDragged(double mx, double my) { return child.mouseDragged(mx, my); }
    @Override public void mouseReleased() { child.mouseReleased(); }
    @Override public boolean mouseScrolled(double mx, double my, double amount) { return child.mouseScrolled(mx, my, amount); }
    @Override public boolean keyPressed(KeyInput in) { return child.keyPressed(in); }
    @Override public boolean charTyped(CharInput in) { return child.charTyped(in); }
    @Override public void unfocus() { child.unfocus(); }
}
