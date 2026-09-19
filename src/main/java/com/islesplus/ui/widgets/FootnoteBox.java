package com.islesplus.ui.widgets;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Full-width bordered box of one or more footnote lines, each re-wrapped to fit the given
 * width; height is computed from the wrapped line count inside layout(). */
public class FootnoteBox extends Widget {
    private static final int PAD = 4;
    private static final int LINE_HEIGHT = Metrics.TITLE_LINE_H;

    private final String[] providedLines;
    private List<String> lines = List.of();

    public FootnoteBox(String... lines) {
        this.providedLines = lines;
    }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width;
        int wrapWidth = Math.max(0, width - PAD * 2);
        List<String> wrapped = new ArrayList<>();
        for (String line : providedLines) wrapped.addAll(Fonts.wrap(line, wrapWidth));
        this.lines = wrapped;
        this.h = PAD * 2 + Math.max(1, lines.size()) * LINE_HEIGHT;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.insetRing(ctx, x, y, w, h, Theme.SECONDARY);
        for (int i = 0; i < lines.size(); i++) {
            Fonts.draw(ctx, lines.get(i), x + PAD, y + PAD + i * LINE_HEIGHT, Theme.TEXT_FOOT);
        }
    }
}
