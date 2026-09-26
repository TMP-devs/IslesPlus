package com.islesplus.ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
public final class Flow {
    private Flow() {}
    public enum Align { START, CENTER, END }

    /** Shared child list + input forwarding for Column/WrapRow: visible children only for
     * click/drag/scroll/key/char (reverse order, stop at first true); all children for
     * mouseReleased/unfocus.
     * <p>mouseClicked commits before it applies: a pre-pass first calls unfocus() on every
     * child whose bounds do NOT contain the click point (visible or not, such a child cannot
     * be the click's target), before the click is forwarded to whichever child does contain
     * it. This matters when a focused field (e.g. a HexField) sits beside a live control
     * bound to the same value (e.g. a HueRail): without committing the field first, forwarding
     * the click straight to the rail would apply the rail's change and only afterwards unfocus
     * the field, whose stale draft would then clobber it. A container that contains the point
     * recurses through this same pre-pass in its own mouseClicked, and a container that
     * doesn't contain the point is unfocused as a whole (Container.unfocus() recurses into its
     * children), so a focused field anywhere else in the tree is always committed first.
     * <p>The post-consume sweep (every OTHER child sent unfocus() once one consumes the click)
     * is kept alongside the pre-pass: it additionally catches an overlapping sibling that DOES
     * contain the point but isn't the one that consumed the click. Both sweeps are safe to
     * call more than once, a well-behaved unfocus() is idempotent.
     * <p>BOTH sweeps are left-click only. A non-left click is still forwarded to the children
     * (that is the only way a listening {@link com.islesplus.ui.widgets.KeyChip} can bind a
     * mouse button), but it must not commit a focused field, and must not unfocus, and so
     * cancel, the very chip that is waiting for it. Every widget other than KeyChip ignores
     * non-left buttons outright. */
    private static abstract class Container extends Widget {
        final List<Widget> children = new ArrayList<>();
        public void clear() { children.clear(); }
        public List<Widget> children() { return children; }
        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0) {
                for (Widget c : children) {
                    if (!c.contains(mx, my)) c.unfocus();
                }
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget c = children.get(i);
                if (c.visible && c.mouseClicked(mx, my, button)) {
                    if (button == 0) {
                        for (int j = 0; j < children.size(); j++) {
                            if (j != i) children.get(j).unfocus();
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        @Override public boolean mouseDragged(double mx, double my) {
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget c = children.get(i);
                if (c.visible && c.mouseDragged(mx, my)) return true;
            }
            return false;
        }
        @Override public void mouseReleased() { for (Widget c : children) c.mouseReleased(); }
        /** Forwarded like a click: only to visible children whose bounds contain the pointer,
         * so a scroll over one child never reaches a sibling somewhere else in the container. */
        @Override public boolean mouseScrolled(double mx, double my, double amount) {
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget c = children.get(i);
                if (c.visible && c.contains(mx, my) && c.mouseScrolled(mx, my, amount)) return true;
            }
            return false;
        }
        @Override public boolean keyPressed(KeyInput in) {
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget c = children.get(i);
                if (c.visible && c.keyPressed(in)) return true;
            }
            return false;
        }
        @Override public boolean charTyped(CharInput in) {
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget c = children.get(i);
                if (c.visible && c.charTyped(in)) return true;
            }
            return false;
        }
        @Override public void unfocus() { for (Widget c : children) c.unfocus(); }
        @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
            for (Widget c : children) if (c.visible) c.render(ctx, mouseX, mouseY);
        }
    }

    public static class Column extends Container {
        private final int gap;
        public Column(int gap) { this.gap = gap; }
        public Column add(Widget w) { children.add(w); return this; }
        @Override public int prefWidth() { return Integer.MAX_VALUE; }
        @Override public int layout(int x, int y, int width) {
            this.x = x; this.y = y; this.w = width;
            int curY = y;
            boolean first = true;
            for (Widget c : children) {
                if (!c.visible) continue;
                if (!first) curY += gap;
                first = false;
                curY += c.layout(x, curY, width);
            }
            this.h = curY - y;
            return this.h;
        }
    }

    /** Fixed-column grid of equal-width cells that collapses to a single column on narrow widths.
     * Cells are filled left to right, top to bottom; each grid row is as tall as its tallest
     * visible child; invisible children are skipped entirely (they take no cell). */
    public static class Grid extends Container {
        private final int cols, gap, singleColumnBelowWidth;
        private java.util.function.IntSupplier minCellWidth;
        public Grid(int cols, int gap, int singleColumnBelowWidth) {
            this.cols = Math.max(1, cols);
            this.gap = gap;
            this.singleColumnBelowWidth = singleColumnBelowWidth;
        }
        public Grid add(Widget w) { children.add(w); return this; }
        /** Also fall back to one column whenever a multi-column cell would be narrower than this. */
        public Grid minCellWidth(java.util.function.IntSupplier minCellWidth) { this.minCellWidth = minCellWidth; return this; }
        @Override public int prefWidth() { return Integer.MAX_VALUE; }
        @Override public int layout(int x, int y, int width) {
            this.x = x; this.y = y; this.w = width;
            int n = width < singleColumnBelowWidth ? 1 : cols;
            if (n > 1 && minCellWidth != null && (width - (n - 1) * gap) / n < minCellWidth.getAsInt()) n = 1;
            int cellW = Math.max(1, (width - (n - 1) * gap) / n);
            int curY = y, rowH = 0, col = 0;
            for (Widget c : children) {
                if (!c.visible) continue;
                rowH = Math.max(rowH, c.layout(x + col * (cellW + gap), curY, cellW));
                if (++col == n) { curY += rowH + gap; col = 0; rowH = 0; }
            }
            if (col != 0) curY += rowH + gap;   // trailing partial row
            this.h = Math.max(0, curY - y - gap);
            return this.h;
        }
    }

    public static class WrapRow extends Container {
        private static final int FILL_MIN_WIDTH = 60;
        private final int gapX, gapY;
        private Align align = Align.START;
        public WrapRow(int gapX, int gapY) { this.gapX = gapX; this.gapY = gapY; }
        public WrapRow add(Widget w) { children.add(w); return this; }
        public WrapRow align(Align a) { this.align = a; return this; }
        @Override public int prefWidth() { return Integer.MAX_VALUE; }
        @Override public int layout(int x, int y, int width) {
            this.x = x; this.y = y; this.w = width;
            List<Widget> visible = new ArrayList<>();
            for (Widget c : children) if (c.visible) visible.add(c);

            int curY = y;
            int i = 0;
            boolean firstLine = true;
            while (i < visible.size()) {
                List<Widget> line = new ArrayList<>();
                int used = 0;
                while (i < visible.size()) {
                    Widget c = visible.get(i);
                    boolean isFill = c.prefWidth() == Integer.MAX_VALUE;
                    // A fill child is measured at its 60px minimum for fit purposes only; any
                    // number of fill children can share a line, each wrapping to a new line only
                    // when even its minimum (plus gap) would overflow.
                    int naturalW = isFill ? FILL_MIN_WIDTH : c.prefWidth();
                    if (line.isEmpty()) {
                        // A line always gets at least one child, even if it must be clamped later.
                        line.add(c);
                        used += naturalW;
                        i++;
                        continue;
                    }
                    int gapNeeded = gapX;
                    if (used + gapNeeded + naturalW > width) break; // doesn't fit: wrap
                    line.add(c);
                    used += gapNeeded + naturalW;
                    i++;
                }
                if (!firstLine) curY += gapY;
                firstLine = false;
                int lineY = curY;
                curY = lineY + layoutLine(line, x, lineY, width);
            }
            this.h = curY - y;
            return this.h;
        }
        private int layoutLine(List<Widget> line, int x, int y, int width) {
            int n = line.size();
            int[] widths = new int[n];
            int fixedTotal = 0;
            List<Integer> fillIndices = new ArrayList<>();
            for (int idx = 0; idx < n; idx++) {
                int pw = line.get(idx).prefWidth();
                if (pw == Integer.MAX_VALUE) { fillIndices.add(idx); }
                else { widths[idx] = pw; fixedTotal += pw; }
            }
            int gaps = (n - 1) * gapX;
            int startX;
            if (!fillIndices.isEmpty()) {
                // Leftover width is split equally among the line's fill children (integer
                // division); the last fill child absorbs the remainder so the line ends exactly
                // at the right edge. The fit-check in layout() guarantees this leftover is at
                // least FILL_MIN_WIDTH per fill child.
                int leftover = Math.max(0, width - fixedTotal - gaps);
                int fillCount = fillIndices.size();
                int share = leftover / fillCount;
                for (int k = 0; k < fillCount; k++) {
                    int idx = fillIndices.get(k);
                    widths[idx] = (k == fillCount - 1) ? leftover - share * (fillCount - 1) : share;
                }
                startX = x; // align has no effect on a line containing a fill child
            } else {
                int contentWidth = fixedTotal + gaps;
                if (n == 1 && widths[0] > width) {
                    widths[0] = width; // never overflow a lone oversized child
                    contentWidth = width;
                }
                int leftover = Math.max(0, width - contentWidth);
                startX = switch (align) {
                    case CENTER -> x + leftover / 2;
                    case END -> x + leftover;
                    default -> x;
                };
            }
            int curX = startX;
            int[] xs = new int[n];
            int[] heights = new int[n];
            int lineHeight = 0;
            for (int idx = 0; idx < n; idx++) {
                if (idx > 0) curX += gapX;
                xs[idx] = curX;
                heights[idx] = line.get(idx).layout(curX, y, widths[idx]);
                lineHeight = Math.max(lineHeight, heights[idx]);
                curX += widths[idx];
            }
            // Vertically centre any child shorter than the tallest child in the line.
            for (int idx = 0; idx < n; idx++) {
                if (heights[idx] != lineHeight) {
                    int centeredY = y + (lineHeight - heights[idx]) / 2;
                    // Re-invokes layout() on this child (already laid out once above at the
                    // line's top y) with the centred y instead; layout() must stay idempotent
                    // for a given (x, y, width) so calling it twice per frame is safe.
                    line.get(idx).layout(xs[idx], centeredY, widths[idx]);
                }
            }
            return lineHeight;
        }
    }
}
