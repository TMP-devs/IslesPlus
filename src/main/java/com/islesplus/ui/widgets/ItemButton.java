package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.function.Supplier;

/** A square raised button showing one item (e.g. "the icon; click to change it"). 20 x 20.
 * An empty stack draws a "+" instead: nothing chosen yet. */
public class ItemButton extends Widget {
    public static final int SIZE = 20;

    private final Supplier<ItemStack> stack;
    private final Runnable onClick;

    public ItemButton(Supplier<ItemStack> stack, Runnable onClick) {
        this.stack = stack;
        this.onClick = onClick;
    }

    @Override public int prefWidth() { return SIZE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = SIZE; this.h = SIZE;
        return SIZE;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        Draw.bevel(ctx, x, y, w, h, hover ? Theme.RAISED_HOVER : Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE,
            hover ? Theme.OXBLOOD : Theme.INK);
        ItemStack s = stack.get();
        if (s.isEmpty()) Draw.plus(ctx, x + SIZE / 2, y + SIZE / 2, Theme.TEXT_META);
        else ctx.drawItem(s, x + (SIZE - 16) / 2, y + (SIZE - 16) / 2);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        IslesClient.playMenuClickSound();
        onClick.run();
        return true;
    }
}
