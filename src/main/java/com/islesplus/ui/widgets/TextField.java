package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Single-line well text field (16 high, fills its row): reads the current text from
 * {@code get} every frame and writes through {@code set} rather than holding its own copy.
 * Shows a blinking "_" cursor while focused and left-truncates overlong text so the cursor
 * stays visible.
 * <p>Contract: a host must deliver every left click either to this widget or call
 * {@code unfocus()} on it, so a click elsewhere is always seen; Flow containers already do
 * this for siblings. */
public class TextField extends Widget {
    protected final Supplier<String> get;
    protected final Consumer<String> set;
    protected final String placeholder;
    protected final int maxLen;

    private boolean noSpaces = false;
    private int ring = Theme.INK_DEEP;
    private Runnable onCommit;
    private Runnable onCancel;
    private Runnable onBlur;
    private boolean light = false;
    protected boolean centered = false;
    /** Draw what the user typed in the vanilla font so upper and lower case are distinguishable.
     * On by default; HexField turns it off (hex is case-insensitive and shown in capitals). */
    protected boolean mixedCase = true;

    public boolean focused;
    /** Ctrl/Cmd+A state: the whole text is selected (there is no partial selection). */
    private boolean allSelected;

    public TextField(Supplier<String> get, Consumer<String> set, String placeholder, int maxLen) {
        this.get = get;
        this.set = set;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLen = maxLen;
    }

    public TextField noSpaces() { this.noSpaces = true; return this; }
    public TextField ring(int colour) { this.ring = colour; return this; }
    public TextField onCommit(Runnable r) { this.onCommit = r; return this; }
    public TextField onCancel(Runnable r) { this.onCancel = r; return this; }
    /** Runs once whenever the field loses focus (click away, Enter, Escape, screen closing). */
    public TextField onBlur(Runnable r) { this.onBlur = r; return this; }
    /** CALC_NUM well with INK_DEEP text (instead of the default dark well with CREAM text). */
    public TextField light() { this.light = true; return this; }

    /** The field that currently has keyboard focus, if any (at most one does at a time). */
    private static TextField focusedField;

    /** Whether the player is typing into some text field right now. */
    public static boolean anyFocused() { return focusedField != null && focusedField.focused; }

    public void focus() {
        if (!focused) onFocusGained();
        focused = true;
        allSelected = false;
        focusedField = this;
    }

    /** Idempotent. {@code onBlur} runs once, on the focused -> unfocused transition: the place to
     * persist what was typed (never save per keystroke - that is a disk write per character). */
    @Override public void unfocus() {
        boolean wasFocused = this.focused;
        this.focused = false;
        if (focusedField == this) focusedField = null;
        this.allSelected = false;
        if (wasFocused && onBlur != null) onBlur.run();
    }

    /** Current text, reading through {@code get} each call; never null. */
    protected String text() {
        String s = get.get();
        return s == null ? "" : s;
    }

    /** Hook for subclasses restricting which typed characters are accepted (e.g. HexField). */
    protected boolean acceptChar(char c) {
        return !(noSpaces && c == ' ');
    }

    /** Hook run exactly once when focus transitions from false to true. */
    protected void onFocusGained() {}

    @Override public int layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = Metrics.FIELD_H;
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        int ringC = focused ? Theme.OXBLOOD : ring;
        int textC, placeholderC;
        if (light) {
            textC = Theme.INK_DEEP; placeholderC = Theme.TEXT_META;
            Draw.bevel(ctx, x, y, w, h, Theme.CALC_NUM, Theme.CALC_NUM_LIT, Theme.CALC_NUM_SHADE, ringC);
        } else {
            textC = Theme.CREAM; placeholderC = Theme.WELL_TEXT_DIM;
            Draw.well(ctx, x, y, w, h, ringC);
        }

        String t = text();
        boolean cursorOn = focused && (Util.getMeasuringTimeMs() / 500L) % 2 == 0;
        int maxW = Math.max(0, w - 8);
        int textH = Fonts.height(Fonts.BODY);
        int ty = y + (h - textH) / 2;

        if (t.isEmpty()) {
            // Empty: the placeholder is a dim background hint and the cursor sits at the START,
            // over its first letter. The hint disappears as soon as anything is typed.
            String hint = Fonts.ellipsize(placeholder, maxW);
            int hintX = centered ? x + (w - Fonts.width(hint, Fonts.BODY)) / 2 : x + 4;
            Fonts.draw(ctx, hint, hintX, ty, placeholderC, Fonts.BODY);
            if (cursorOn) Fonts.draw(ctx, "_", hintX, ty, textC, Fonts.BODY);
            return;
        }

        // Typed text is drawn in a mixed-case font when the field is case sensitive (Silkscreen has
        // no lowercase glyphs, so "Scrolls" and "SCROLLS" would look identical in it).
        String display = cursorOn ? t + "_" : t;
        while (!display.isEmpty() && typedWidth(display) > maxW) {
            display = display.substring(1);
        }
        // Centre on the text alone so it does not shift sideways each time the cursor blinks.
        String shown = cursorOn ? display.substring(0, display.length() - 1) : display;
        int shownW = typedWidth(shown);
        int tx = centered ? x + (w - shownW) / 2 : x + 4;
        if (allSelected && focused) {
            // Select-all highlight: oxblood block behind the text, cream text on top.
            ctx.fill(tx - 1, ty - 1, tx + shownW + 1, ty + textH + 1, Theme.OXBLOOD);
            drawTyped(ctx, shown, tx, ty, Theme.CREAM);
        } else {
            drawTyped(ctx, display, tx, ty, textC);
        }
    }

    private int typedWidth(String s) {
        return mixedCase ? Fonts.plainWidth(s) : Fonts.width(s, Fonts.BODY);
    }

    private void drawTyped(DrawContext ctx, String s, int tx, int ty, int colour) {
        if (mixedCase) Fonts.drawPlain(ctx, s, tx, ty, colour);
        else Fonts.draw(ctx, s, tx, ty, colour, Fonts.BODY);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible) return false;
        if (contains(mx, my)) {
            focus();
            IslesClient.playMenuClickSound();
            return true;
        }
        if (focused) unfocus();
        return false;
    }

    @Override public boolean keyPressed(KeyInput in) {
        if (!focused) return false;
        int key = in.key();
        // Ctrl (Cmd on macOS) + A / C / X / V. The field has no caret movement, so the only
        // selection is "everything".
        if (in.isSelectAll()) { allSelected = !text().isEmpty(); return true; }
        if (in.isCopy()) { if (!text().isEmpty()) clipboard(text()); return true; }
        if (in.isCut()) {
            if (!text().isEmpty()) { clipboard(text()); set.accept(""); }
            allSelected = false;
            return true;
        }
        if (in.isPaste()) { insert(clipboard()); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            String cur = text();
            if (allSelected) set.accept("");
            else if (key == GLFW.GLFW_KEY_BACKSPACE && !cur.isEmpty()) set.accept(cur.substring(0, cur.offsetByCodePoints(cur.length(), -1)));
            allSelected = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (onCommit != null) onCommit.run();
            unfocus();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (onCancel != null) onCancel.run();
            unfocus();
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(CharInput in) {
        if (!focused) return false;
        insert(in.asString());
        return true;
    }

    /** Adds typed or pasted text: replaces the selection if everything is selected, drops any
     * character the field does not accept (so a paste is filtered, not refused) and clips the
     * result to the field's maximum length. */
    private void insert(String s) {
        if (s == null || s.isEmpty()) return;
        StringBuilder ok = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c != 127 && acceptChar(c)) ok.append(c);
        }
        if (ok.length() == 0) return;
        String cur = allSelected ? "" : text();
        int room = maxLen <= 0 ? Integer.MAX_VALUE : maxLen - cur.length();
        if (room <= 0) return;
        if (ok.length() > room) {
            int cut = room;
            if (Character.isHighSurrogate(ok.charAt(cut - 1))) cut--;   // never split a surrogate pair
            ok.setLength(cut);
        }
        if (ok.length() == 0) return;
        allSelected = false;
        set.accept(cur + ok);
    }

    private static String clipboard() {
        String s = MinecraftClient.getInstance().keyboard.getClipboard();
        return s == null ? "" : s;
    }

    private static void clipboard(String s) { MinecraftClient.getInstance().keyboard.setClipboard(s); }
}
