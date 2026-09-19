package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Full-width bordered placeholder box with centred, wrapped TEXT_FOOT text; height is
 * computed from the wrapped line count inside layout(). */
public class EmptyBox extends Widget {
    private static final int PAD = 4;

    private final String text;
    private List<String> lines = List.of();

    public EmptyBox(String text) {
        this.text = text;
    }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width;
        int wrapWidth = Math.max(0, width - PAD * 2);
        lines = Fonts.wrap(text, wrapWidth);
        this.h = PAD * 2 + Math.max(1, lines.size()) * Metrics.LINE_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.insetRing(ctx, x, y, w, h, Theme.SECONDARY);
        int cx = x + w / 2;
        for (int i = 0; i < lines.size(); i++) {
            Fonts.drawCentered(ctx, lines.get(i), cx, y + PAD + i * Metrics.LINE_H, Theme.TEXT_FOOT, Fonts.BODY);
        }
    }
}
