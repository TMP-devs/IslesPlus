package com.islesplus.ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
public abstract class Widget {
    public int x, y, w, h;
    public boolean visible = true;
    /** Assign bounds for the given width; return height. Called every frame before render.
     * <p>Contract: must be idempotent, and may be called more than once per frame for the same
     * widget, a {@link Flow.WrapRow} re-lays out any child shorter than its line so it ends up
     * vertically centred, a {@link com.islesplus.ui.widgets.Dialog} measures its body at (0,0)
     * before positioning it, and the /ip grid measures both cards of a row before giving them
     * their shared height. It must therefore assign geometry only and never mutate model state
     * (no config writes, no committing a draft, no toggling a feature flag); rebuilding a child
     * list from an unchanged model is fine, because that is idempotent too. */
    public abstract int layout(int x, int y, int width);
    /** Natural width for WrapRow; Integer.MAX_VALUE means "fill the row". */
    public int prefWidth() { return Integer.MAX_VALUE; }
    public abstract void render(DrawContext ctx, int mouseX, int mouseY);
    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public boolean mouseDragged(double mx, double my) { return false; }
    public void mouseReleased() {}
    public boolean mouseScrolled(double mx, double my, double amount) { return false; }
    public boolean keyPressed(KeyInput in) { return false; }
    public boolean charTyped(CharInput in) { return false; }
    /** Give up keyboard focus, committing whatever draft this widget is holding.
     * <p>Contract: must be idempotent. Containers broadcast it on almost every click, once in
     * the pre-pass over every child that does not contain the click point, and again in the
     * sweep over every child but the one that consumed the click, and an overlay host calls it
     * on each overlay it removes, so it is routinely invoked on a widget that has nothing left
     * to give up and must then do nothing at all. */
    public void unfocus() {}
    public boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
}
