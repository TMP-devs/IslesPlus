package com.islesplus.ui.widgets;

import com.islesplus.ui.ColorMath;
import com.islesplus.ui.Fonts;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Hex colour field, sized so a full "#RRGGBB" plus the cursor always fits. Reuses TextField's well/text drawing and input handling,
 * but keeps a local draft while focused instead of writing straight through: on commit or
 * unfocus, a valid "#RRGGBB" draft is applied via {@code setRgb}, otherwise it is discarded
 * and the field reverts to showing the current colour. */
public class HexField extends TextField {
    private final Supplier<Integer> getRgb;
    private final Consumer<Integer> setRgb;
    private final String[] draft;

    public HexField(Supplier<Integer> getRgb, Consumer<Integer> setRgb) {
        this(getRgb, setRgb, new String[]{""});
    }

    private HexField(Supplier<Integer> getRgb, Consumer<Integer> setRgb, String[] draft) {
        super(() -> draft[0], s -> draft[0] = s, "", 7);
        this.getRgb = getRgb;
        this.setRgb = setRgb;
        this.draft = draft;
        this.draft[0] = ColorMath.toHex(rgb());
        this.centered = true;
        this.mixedCase = false;
        // Escape cancels: put the draft back to the live colour first, so the unfocus() that follows
        // (which applies any valid draft) has nothing new to apply.
        onCancel(() -> draft[0] = ColorMath.toHex(rgb()));
    }

    /** {@code getRgb.get()}, treating a null result as black rather than NPE-ing. */
    private int rgb() {
        Integer v = getRgb.get();
        return v == null ? 0 : v;
    }

    @Override protected boolean acceptChar(char c) {
        return c == '#' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    @Override protected String text() {
        return focused ? draft[0] : ColorMath.toHex(rgb());
    }

    @Override protected void onFocusGained() {
        draft[0] = ColorMath.toHex(rgb());
    }

    /** Idempotent: containers broadcast unfocus() to every child on any click (tab switch,
     * screen close, a sibling consuming the click), so this must be a no-op unless the field
     * was actually focused — otherwise a broadcast unfocus after the colour changed elsewhere
     * (e.g. a hue/brightness rail bound to the same value) would silently revert it to the
     * stale draft. */
    @Override public void unfocus() {
        if (focused) {
            Integer parsed = ColorMath.parseHex(draft[0]);
            if (parsed != null) setRgb.accept(parsed);
        }
        super.unfocus();
    }

    /** Widest possible value ("#" + six of the widest hex glyph) + the cursor + 4 px padding each side. */
    public static int fieldWidth() { return Fonts.width("#DDDDDD_") + 8; }

    @Override public int prefWidth() { return fieldWidth(); }

    @Override public int layout(int x, int y, int width) {
        return super.layout(x, y, fieldWidth());
    }
}
