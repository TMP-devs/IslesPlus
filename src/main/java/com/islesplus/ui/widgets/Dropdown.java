package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** Bevelled dropdown button (18 high, fills its row): shows the selected item's name with a
 * caret, and opens a scrollable row list as a further host overlay anchored under (or, if it
 * would run off-screen, flipped above) the button. */
public class Dropdown extends Widget {
    public enum Style { DARK, LIGHT }

    private static final int LIGHT_HOVER = 0xFFE2D3AA;
    private static final int CARET_INSET = 6;

    private final OverlayHost host;
    private final Style style;
    private final IntSupplier count;
    private final IntFunction<String> name;
    private final IntFunction<String> meta;
    private final IntSupplier selected;
    private final IntConsumer onSelect;

    private final ListOverlay list = new ListOverlay();
    private boolean open = false;

    public Dropdown(OverlayHost host, Style style, IntSupplier count, IntFunction<String> name,
                     IntFunction<String> meta, IntSupplier selected, IntConsumer onSelect) {
        this.host = host;
        this.style = style;
        this.count = count;
        this.name = name;
        this.meta = meta;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width; this.h = Metrics.DROPDOWN_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY);
        int textC, caretC;
        if (style == Style.DARK) {
            Draw.well(ctx, x, y, w, h, Theme.INK_DEEP);
            textC = Theme.CREAM;
            caretC = Theme.WELL_TEXT_DIM;
        } else {
            // LIGHT: transparent on its parent; no background/ring when closed, hover fill only.
            if (hover) ctx.fill(x, y, x + w, y + h, LIGHT_HOVER);
            textC = Theme.INK_DEEP;
            caretC = Theme.SURFACE_RING;
        }

        int caretCx = x + w - CARET_INSET - 4;
        int caretCy = y + h / 2;
        Draw.caret(ctx, caretCx, caretCy, open ? Draw.Dir.UP : Draw.Dir.DOWN, caretC);

        int idx = selected.getAsInt();
        int cnt = count.getAsInt();
        // While the list is open and the user is typing, the button doubles as the search box.
        String label = open && !list.query.isEmpty() ? list.query + "_"
            : (idx >= 0 && idx < cnt) ? name.apply(idx) : "";
        int textLeft = x + 4;
        int textRight = caretCx - 3 - 4; // clear of the caret's left edge, plus a small gap
        String display = Fonts.ellipsize(label, Math.max(0, textRight - textLeft));
        int textH = Fonts.height(Fonts.BODY);
        Fonts.draw(ctx, display, textLeft, y + (h - textH) / 2, textC, Fonts.BODY);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        IslesClient.playMenuClickSound();
        int sel = selected.getAsInt();
        int cnt = count.getAsInt();
        list.query = "";
        list.refilter();
        int maxScroll = list.maxScroll();
        list.scroll = (sel >= 0 && sel < cnt)
            ? Math.max(0, Math.min(maxScroll, sel - Metrics.DROPDOWN_MAX_ROWS / 2))
            : 0;
        open = true;
        host.openOverlay(list);
        return true;
    }

    /** The dropdown's row list, opened as a further overlay on the host; it positions itself
     * from the owning Dropdown's freshly laid-out bounds rather than the x/y/width passed in.
     * Typing while it is open filters the rows (case-insensitive). */
    private final class ListOverlay extends Widget {
        private int scroll = 0;
        /** Type-to-search text; rows whose name does not contain it (ignoring case) are hidden. */
        private String query = "";
        /** Item indices that pass {@link #query}, rebuilt every layout. */
        private final List<Integer> matches = new ArrayList<>();

        private void refilter() {
            matches.clear();
            String q = query.toLowerCase(Locale.ROOT);
            int cnt = count.getAsInt();
            for (int i = 0; i < cnt; i++) {
                String n = name.apply(i);
                if (q.isEmpty() || (n != null && n.toLowerCase(Locale.ROOT).contains(q))) matches.add(i);
            }
        }

        /** At least one row is always shown so an empty result can say so. */
        private int visibleRows() { return Math.max(1, Math.min(matches.size(), Metrics.DROPDOWN_MAX_ROWS)); }

        /** Furthest the list can be scrolled down given the current (filtered) row count. */
        private int maxScroll() { return Math.max(0, matches.size() - Metrics.DROPDOWN_MAX_ROWS); }

        @Override public int layout(int x, int y, int width) {
            refilter();
            int rows = visibleRows();
            int maxScroll = maxScroll();
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;

            int listH = rows * Metrics.DROPDOWN_ROW_H;
            this.w = Dropdown.this.w;
            this.x = Dropdown.this.x;
            int screenH = host.screenHeight();
            if (Dropdown.this.y + Metrics.DROPDOWN_H + listH > screenH) {
                this.y = Dropdown.this.y - listH;
            } else {
                this.y = Dropdown.this.y + Metrics.DROPDOWN_H;
            }
            this.h = listH;
            return this.h;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
            int rows = visibleRows();
            int sel = selected.getAsInt();
            int rowH = Metrics.DROPDOWN_ROW_H;
            int textH = Fonts.height(Fonts.BODY);

            // Ink frame 1 px outside the list (2 px below): the open list then lines up with the
            // button's own ring, and the rings of whatever lies underneath cannot peek out past it.
            int frameRows = Math.max(1, matches.isEmpty() ? 1 : rows);
            int frameC = style == Style.DARK ? Theme.INK_DEEP : Theme.SURFACE_RING;
            ctx.fill(x - 1, y - 1, x + w + 1, y + frameRows * rowH + 2, frameC);

            if (matches.isEmpty()) {
                int bg = style == Style.DARK ? Theme.WELL : Theme.RAISED;
                int textC = style == Style.DARK ? Theme.WELL_TEXT_DIM : Theme.TEXT_FOOT;
                ctx.fill(x, y, x + w, y + rowH, bg);
                Fonts.draw(ctx, "NO MATCHES", x + 4, y + (rowH - textH) / 2, textC, Fonts.BODY);
                return;
            }

            for (int slot = 0; slot < rows; slot++) {
                int idx = matches.get(scroll + slot);
                int rowY = y + slot * rowH;
                boolean isSelected = idx == sel;
                int bg, textC, dotC, metaC;
                if (isSelected) {
                    bg = Theme.OXBLOOD; textC = Theme.CREAM; dotC = Theme.CREAM; metaC = 0xFFE8CFC6;
                } else if (style == Style.DARK) {
                    bg = Theme.WELL; textC = Theme.WELL_ROW_TEXT; dotC = Theme.OFF_TRACK; metaC = 0xFFA89C88;
                } else {
                    bg = Theme.RAISED; textC = Theme.INK_DEEP; dotC = Theme.SECONDARY; metaC = Theme.TEXT_FOOT;
                }
                ctx.fill(x, rowY, x + w, rowY + rowH, bg);
                if (!isSelected) {
                    int lineC = style == Style.DARK ? Theme.WELL_SHADE : Theme.RAISED_SHADE;
                    ctx.fill(x, rowY + rowH - 1, x + w, rowY + rowH, lineC);
                }

                int dotSize = 3;
                int dotX = x + 4, dotY = rowY + (rowH - dotSize) / 2;
                ctx.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, dotC);

                int nameX = dotX + dotSize + 4;
                int textY = rowY + (rowH - textH) / 2;

                String metaText = meta == null ? null : meta.apply(idx);
                boolean hasMeta = metaText != null && !metaText.isEmpty();
                int metaW = hasMeta ? Fonts.width(metaText, Fonts.SMALL) : 0;
                int metaX = x + w - 4 - metaW;
                int nameMaxW = Math.max(0, (hasMeta ? metaX - 4 : x + w - 4) - nameX);

                String display = Fonts.ellipsize(name.apply(idx), nameMaxW);
                Fonts.draw(ctx, display, nameX, textY, textC, Fonts.BODY);

                if (hasMeta) {
                    int metaTextH = Fonts.height(Fonts.SMALL);
                    Fonts.draw(ctx, metaText, metaX, rowY + (rowH - metaTextH) / 2, metaC, Fonts.SMALL);
                }
            }

            if (matches.size() > Metrics.DROPDOWN_MAX_ROWS) {
                int maxScroll = maxScroll();
                int thumbH = Math.max(4, h * rows / matches.size());
                int track = h - thumbH;
                int thumbY = maxScroll <= 0 ? y : y + track * scroll / maxScroll;
                int barC = style == Style.DARK ? Theme.WELL_LIT : Theme.RAISED_SHADE;
                ctx.fill(x + w - 2, thumbY, x + w, thumbY + thumbH, barC);
            }
        }

        private void choose(int itemIndex) {
            onSelect.accept(itemIndex);
            IslesClient.playMenuClickSound();
            host.closeOverlay(this);   // the host calls unfocus(), which resets open + query
        }

        @Override public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) return false;
            if (contains(mx, my) && !matches.isEmpty()) {
                int slot = (int) ((my - y) / Metrics.DROPDOWN_ROW_H);
                if (slot >= 0 && slot < visibleRows() && scroll + slot < matches.size()) {
                    choose(matches.get(scroll + slot));
                    return true;
                }
            }
            IslesClient.playMenuClickSound();
            host.closeOverlay(this);
            return true;
        }

        @Override public boolean mouseScrolled(double mx, double my, double amount) {
            int maxScroll = maxScroll();
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
            if (amount > 0) scroll = Math.max(0, scroll - 1);
            else if (amount < 0) scroll = Math.min(maxScroll, scroll + 1);
            return true;
        }

        /** Type to filter (case-insensitive "contains"). */
        @Override public boolean charTyped(CharInput in) {
            String s = in.asString();
            if (s == null) return true;
            StringBuilder sb = new StringBuilder(query);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c >= ' ' && c != 127 && sb.length() < 32) sb.append(c);
            }
            query = sb.toString();
            scroll = 0;
            return true;
        }

        /** Backspace edits the search, Enter picks the first match. Escape is NOT consumed, so
         * the host closes this list. */
        @Override public boolean keyPressed(KeyInput in) {
            int key = in.key();
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!query.isEmpty()) query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
                scroll = 0;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                refilter();
                if (!matches.isEmpty()) choose(matches.get(0));
                return true;
            }
            return false;
        }

        /** The host calls this on every overlay it removes, which is the only signal this list
         * gets when it is dismissed by Escape or by a Dialog below it closing rather than by a
         * click on one of its rows. Without it the owning Dropdown would stay {@code open} and
         * keep drawing its caret pointing up at a list that is no longer there. */
        @Override public void unfocus() { open = false; query = ""; }
    }
}
