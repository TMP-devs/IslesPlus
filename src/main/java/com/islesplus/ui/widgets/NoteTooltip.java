package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** The floating note every Isles+ hover uses: tan panel, oxblood left edge, wrapped text, placed
 * beside the cursor and kept inside the window. */
public final class NoteTooltip {
    private NoteTooltip() {}

    public static void draw(DrawContext ctx, String text, int mouseX, int mouseY, int screenW, int screenH) {
        int pad = 4, edge = 2, maxTextW = 150;
        List<String> lines = Fonts.wrap(text, maxTextW);
        int textW = 0;
        for (String line : lines) textW = Math.max(textW, Fonts.width(line));
        int w = edge + pad + textW + pad;
        int h = pad + lines.size() * Metrics.LINE_H - (Metrics.LINE_H - Fonts.GLYPH_H) + pad;
        int x = Math.min(mouseX + 8, screenW - w - 4);
        int y = mouseY + 10 + h > screenH - 4 ? mouseY - h - 6 : mouseY + 10;
        x = Math.max(4, x);
        y = Math.max(4, y);

        ctx.fill(x + 2, y + 3, x + w + 2, y + h + 3, Theme.DROP_SHADOW);
        Draw.bevel(ctx, x, y, w, h, Theme.NOTE_BG, Theme.RAISED, Theme.SECONDARY, Theme.INK);
        ctx.fill(x, y, x + edge, y + h, Theme.OXBLOOD);
        for (int i = 0; i < lines.size(); i++) {
            Fonts.draw(ctx, lines.get(i), x + edge + pad, y + pad + i * Metrics.LINE_H, Theme.NOTE_TEXT);
        }
    }
}
