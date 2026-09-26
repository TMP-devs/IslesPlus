package com.islesplus.screen.islesscreen.rows;

import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/** The oxblood "i" badge from the card headers, for use inside a drawer: hovering it shows a note
 * in the same floating tooltip. */
final class InfoMark extends Widget {
    private static final int SIZE = 9;
    private final Supplier<String> note;

    /** The text is read on every hover, so it can change (remote features_v2.json). */
    InfoMark(Supplier<String> note) { this.note = note; }

    @Override public int prefWidth() { return SIZE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = SIZE; this.h = SIZE;
        return SIZE;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.bevel(ctx, x, y, SIZE, SIZE, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
        int stemX = x + SIZE / 2;
        ctx.fill(stemX, y + 2, stemX + 1, y + 3, Theme.CREAM);            // dot
        ctx.fill(stemX, y + 4, stemX + 1, y + SIZE - 2, Theme.CREAM);     // stem
        if (mouseX >= x - 1 && mouseX < x + SIZE + 1 && mouseY >= y - 1 && mouseY < y + SIZE + 1) {
            FeatureRow.hoveredNote = note.get();
        }
    }
}
