package com.islesplus.screen.islesscreen.rows;

import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Oxblood warning triangle with a cream "!" shown beside a keybind whose key is also bound to
 * another action (vanilla or modded). Always reserves its width so the key chips stay aligned;
 * draws nothing while there is no conflict. Hovering it lists the clashing actions.
 */
final class KeyConflictMark extends Widget {
    /** Half-width of the oxblood body on each row, top to bottom: a one-pixel apex widening to 11
     * pixels over 8 rows. The slope sits between "one pixel per row" (reads flat and squat) and
     * "one pixel every two rows" (reads tall and spiky); it was picked by rendering the candidates
     * side by side. With its one-pixel ink outline the mark is 13 x 10. */
    private static final int[] HALF = {0, 1, 1, 2, 3, 3, 4, 5};
    private static final int BODY_W = 11, BODY_H = HALF.length, W = BODY_W + 2, H = BODY_H + 2;

    private final KeyBinding binding;

    private java.util.function.Supplier<String> extraWarning;

    KeyConflictMark(KeyBinding binding) { this.binding = binding; }

    /** A second reason to show the mark: the supplier returns the warning text, or null for none. */
    KeyConflictMark alsoWarn(java.util.function.Supplier<String> warning) { this.extraWarning = warning; return this; }

    @Override public int prefWidth() { return W; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = W; this.h = H;
        return H;
    }

    private static final long RECHECK_MS = 250L;
    private List<String> cachedConflicts = List.of();
    private long checkedAtMs = Long.MIN_VALUE;

    /** {@link #scanConflicts()}, at most four times a second: this is asked every frame by every
     * keybind strip, and a scan walks every key binding in the game. */
    private List<String> conflicts() {
        long now = net.minecraft.util.Util.getMeasuringTimeMs();
        if (now - checkedAtMs >= RECHECK_MS || checkedAtMs == Long.MIN_VALUE) {
            cachedConflicts = scanConflicts();
            checkedAtMs = now;
        }
        return cachedConflicts;
    }

    /** Names of every other action sharing this binding's key; empty when unbound or unique. */
    private List<String> scanConflicts() {
        List<String> names = new ArrayList<>();
        if (binding.isUnbound()) return names;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return names;
        for (KeyBinding other : mc.options.allKeys) {
            if (other != binding && !other.isUnbound() && other.equals(binding)) {
                names.add(Text.translatable(other.getId()).getString());
            }
        }
        return names;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        List<String> clashing = conflicts();
        String extra = extraWarning == null ? null : extraWarning.get();
        if (clashing.isEmpty() && extra == null) return;

        int cx = x + W / 2;
        // Outline point: one ink pixel above the apex, so the mark ends in a point, not a flat top.
        ctx.fill(cx, y, cx + 1, y + 1, Theme.INK);
        for (int row = 0; row < BODY_H; row++) {
            int half = HALF[row];
            int ry = y + 1 + row;
            ctx.fill(cx - half - 1, ry, cx + half + 2, ry + 1, Theme.INK);
            ctx.fill(cx - half, ry, cx + half + 1, ry + 1, Theme.OXBLOOD);
        }
        ctx.fill(x, y + H - 1, x + W, y + H, Theme.INK);
        // "!"
        ctx.fill(cx, y + 3, cx + 1, y + 6, Theme.CREAM);
        ctx.fill(cx, y + 7, cx + 1, y + 8, Theme.CREAM);

        if (contains(mouseX, mouseY)) {
            String note = clashing.isEmpty() ? "" : "Also bound to: " + String.join(", ", clashing);
            if (extra != null) note = note.isEmpty() ? extra : extra + " " + note;
            FeatureRow.hoveredNote = note;
        }
    }
}
