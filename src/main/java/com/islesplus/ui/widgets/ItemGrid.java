package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Scrollable grid of every item, filtered by a query (item name or id, ignoring case). Click a
 * cell to pick it. Fills its row; as many 18 px columns as fit, a fixed number of rows. */
public class ItemGrid extends Widget {
    public static final int CELL = 18;
    private static final int ROWS = 6, SCROLLBAR = 4;

    private record Entry(String id, String search, ItemStack stack) {}
    /** built once, on first use (item names need the language loaded) */
    private static List<Entry> all;

    private final Supplier<String> query;
    private final Supplier<String> selectedId;
    private final Consumer<String> onPick;

    private final List<Entry> matches = new ArrayList<>();
    private String lastQuery;
    private int scroll;
    private int cols = 1, gridX;
    /** item under the mouse at the last render, "" = none */
    private String hoveredName = "";

    public ItemGrid(Supplier<String> query, Supplier<String> selectedId, Consumer<String> onPick) {
        this.query = query;
        this.selectedId = selectedId;
        this.onPick = onPick;
    }

    /** Display name of the item under the mouse, "" when none. */
    public String hoveredName() { return hoveredName; }

    private static List<Entry> all() {
        if (all == null) {
            List<Entry> list = new ArrayList<>();
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) continue;
                String id = Registries.ITEM.getId(item).toString();
                ItemStack stack = new ItemStack(item);
                String name = stack.getName().getString();
                list.add(new Entry(id, (name + " " + id).toLowerCase(Locale.ROOT), stack));
            }
            all = list;
        }
        return all;
    }

    private void refilter() {
        String q = query.get() == null ? "" : query.get().trim().toLowerCase(Locale.ROOT);
        if (q.equals(lastQuery)) return;
        lastQuery = q;
        matches.clear();
        for (Entry e : all()) if (q.isEmpty() || e.search.contains(q)) matches.add(e);
        scroll = 0;
    }

    private int totalRows() { return (matches.size() + cols - 1) / cols; }

    private int maxScroll() { return Math.max(0, totalRows() - ROWS); }

    @Override public int layout(int x, int y, int width) {
        refilter();
        this.x = x; this.y = y; this.w = width;
        // keep 4 px on the right for the scroll bar
        cols = Math.max(1, (width - SCROLLBAR) / CELL);
        gridX = x + (width - SCROLLBAR - cols * CELL) / 2;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        this.h = ROWS * CELL;
        return this.h;
    }

    private int indexAt(double mx, double my) {
        if (mx < gridX || mx >= gridX + cols * CELL || my < y || my >= y + h) return -1;
        int col = (int) ((mx - gridX) / CELL), row = (int) ((my - y) / CELL);
        int i = (scroll + row) * cols + col;
        return i < matches.size() ? i : -1;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        Draw.bevel(ctx, gridX, y, cols * CELL, h, Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.INK);
        String sel = selectedId.get();
        int hover = indexAt(mouseX, mouseY);
        hoveredName = hover >= 0 ? matches.get(hover).stack.getName().getString() : "";

        if (matches.isEmpty()) {
            Fonts.draw(ctx, "No matches", gridX + 4, y + 5, Theme.TEXT_FOOT);
            return;
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < cols; col++) {
                int i = (scroll + row) * cols + col;
                if (i >= matches.size()) break;
                Entry e = matches.get(i);
                int cx = gridX + col * CELL, cy = y + row * CELL;
                if (e.id.equals(sel)) {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, Theme.OXBLOOD);
                } else if (i == hover) {
                    ctx.fill(cx, cy, cx + CELL, cy + CELL, Theme.RAISED_HOVER);
                }
                ctx.drawItem(e.stack, cx + 1, cy + 1);
            }
        }
        if (maxScroll() > 0) {
            int thumbH = Math.max(4, h * ROWS / totalRows());
            int thumbY = y + (h - thumbH) * scroll / maxScroll();
            int barX = gridX + cols * CELL + 2;
            ctx.fill(barX, thumbY, barX + 2, thumbY + thumbH, Theme.SECONDARY);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible) return false;
        int i = indexAt(mx, my);
        if (i < 0) return false;
        IslesClient.playMenuClickSound();
        onPick.accept(matches.get(i).id);
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        if (amount > 0) scroll = Math.max(0, scroll - 1);
        else if (amount < 0) scroll = Math.min(maxScroll(), scroll + 1);
        return true;
    }
}
