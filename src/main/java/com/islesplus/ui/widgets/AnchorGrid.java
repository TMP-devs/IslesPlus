package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Fixed-column grid of selectable anchor-point cells (e.g. a 3x3 position picker) in a
 * bevelled frame; each cell shows a small bar on the edge/corner of the cell it represents. */
public class AnchorGrid extends Widget {
    private static final int PAD = 5;
    private static final int BAR_H = 4, BAR_INSET = 2;

    private final int cols;
    private final String[] labels;
    private final int rows;
    private final IntSupplier selected;
    private final IntConsumer onSelect;

    public AnchorGrid(int cols, String[] labels, IntSupplier selected, IntConsumer onSelect) {
        this.cols = cols;
        this.labels = labels;
        this.rows = labels.length / cols;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    /** The label of the currently selected cell, or "" if the index is out of range. */
    public String selectedLabel() {
        int idx = selected.getAsInt();
        return (idx >= 0 && idx < labels.length) ? labels[idx] : "";
    }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width;
        int contentH = rows * Metrics.ANCHOR_CELL_H + Math.max(0, rows - 1) * Metrics.ANCHOR_GAP;
        this.h = contentH + 2 * PAD;
        return this.h;
    }

    private int cellWidth() {
        int innerW = w - 2 * PAD;
        int gaps = Math.max(0, cols - 1) * Metrics.ANCHOR_GAP;
        return (innerW - gaps) / cols;
    }

    private int cellX(int col, int cellW) { return x + PAD + col * (cellW + Metrics.ANCHOR_GAP); }

    private int cellY(int row) { return y + PAD + row * (Metrics.ANCHOR_CELL_H + Metrics.ANCHOR_GAP); }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(x, y, x + w, y + h, Theme.RAISED);
        Draw.insetRing(ctx, x, y, w, h, Theme.RAISED_RING);
        Draw.ring(ctx, x, y, w, h, Theme.SURFACE_RING);

        int cellW = cellWidth();
        int sel = selected.getAsInt();
        int barW = Math.round(cellW * 0.55f);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= labels.length) continue;
                int cx = cellX(col, cellW), cy = cellY(row);
                boolean active = idx == sel;

                if (active) {
                    Draw.bevel(ctx, cx, cy, cellW, Metrics.ANCHOR_CELL_H,
                        Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
                } else {
                    Draw.bevel(ctx, cx, cy, cellW, Metrics.ANCHOR_CELL_H,
                        Theme.SURFACE, Theme.SURFACE_LIT, Theme.SURFACE_SHADE, Theme.RAISED_RING);
                }

                int barC = active ? Theme.CREAM : Theme.SECONDARY;
                int barX;
                if (col == 0) barX = cx + BAR_INSET;
                else if (col == cols - 1) barX = cx + cellW - BAR_INSET - barW;
                else barX = cx + (cellW - barW) / 2;
                int barY;
                if (row == 0) barY = cy + BAR_INSET;
                else if (row == rows - 1) barY = cy + Metrics.ANCHOR_CELL_H - BAR_INSET - BAR_H;
                else barY = cy + (Metrics.ANCHOR_CELL_H - BAR_H) / 2;
                ctx.fill(barX, barY, barX + barW, barY + BAR_H, barC);
            }
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        int cellW = cellWidth();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= labels.length) continue;
                int cx = cellX(col, cellW), cy = cellY(row);
                if (mx >= cx && mx < cx + cellW && my >= cy && my < cy + Metrics.ANCHOR_CELL_H) {
                    IslesClient.playMenuClickSound();
                    onSelect.accept(idx);
                    return true;
                }
            }
        }
        return false;
    }
}
