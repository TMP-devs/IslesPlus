package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.BigToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One feature card in the scroll UI's two-column grid: a SURFACE-bevelled header (optional caret
 * button, upper-cased title, wrapped description, optional warning badge with a hover tooltip, right-hand toggle) plus an
 * optional expandable drawer holding an arbitrary widget body.
 *
 * <p>{@link #layout(int, int, int)} is idempotent and is called twice per frame by the grid: once
 * with {@link #minHeight} 0 to measure, then again with the row's shared maximum height.
 */
public final class FeatureRow extends Widget {
    private static final int PAD = Metrics.CARD_PAD_X;          // header/drawer padding (6)
    private static final int GAP = Metrics.HEADER_GAP;          // caret/toggle to text gap (6)
    private static final int TITLE_GAP = 2, BADGE = 9, BADGE_GAP = 4;

    /** Tooltip requested by whichever badge the mouse is over this frame; the screen draws it last,
     * above every card and outside the grid scissor, then clears it. */
    public static String hoveredNote;
    private static final String DISABLED_LABEL = "DISABLED";
    /** Shown when the pointer is over a remotely disabled card's DISABLED chip. */
    private static final String DISABLED_NOTE = "This feature is currently under maintenance and will return shortly.";
    private static final int DISABLED_PAD_X = 6;
    private static final String BETA_LABEL = "BETA";

    private final String title;
    private final String titleUpper;
    private final String description;                           // nullable
    private String killedKey;
    private String note;                                        // nullable; shown as a badge + tooltip
    private boolean beta;                                       // small oxblood "BETA" after the badge
    private BigToggle toggle;
    private Widget drawer;

    /** Whether the drawer is open. Ignored when the row has no drawer or is remotely killed. */
    public boolean expanded;
    /** Minimum height, set by the grid so both cards on a grid row match. */
    public int minHeight;

    // --- geometry, recomputed every layout() ---
    private int headerH, naturalH;
    private int textX, textW, titleY, descY;
    private int caretX, caretY;
    private int badgeX, badgeY;
    private int betaX, betaY;
    private int titleSpace;                                     // width left for the title text
    private int controlX, controlY, controlW, controlH;
    private int drawerBodyH;
    private List<String> descLines = List.of();

    public FeatureRow(String title, String description) {
        this.title = title;
        this.titleUpper = title.toUpperCase(Locale.ROOT);
        this.description = description;
    }

    /** A note for this feature: drawn as a small oxblood "i" badge after the title, text shown on hover. */
    public FeatureRow note(String text) { this.note = text; return this; }

    /** Marks the feature as beta: a small oxblood "BETA" tag after the title (and the info badge). */
    public FeatureRow beta() { this.beta = true; return this; }

    public FeatureRow killedKey(String key) { this.killedKey = key; return this; }

    public FeatureRow toggle(BooleanSupplier get, Consumer<Boolean> set) {
        this.toggle = new BigToggle(get, set);
        return this;
    }

    /** Giving the row a drawer body is what makes the caret button appear. */
    public FeatureRow drawer(Widget body) { this.drawer = body; return this; }

    /** Card width at which the upper-cased title fits on one line beside the caret and the toggle. */
    public int widthForFullTitle() {
        // A card that can be remotely disabled must also fit its DISABLED chip, which is wider than
        // the toggle (the caret is hidden then, which gives some of that width back).
        int control = toggle != null ? Metrics.BIG_TOGGLE_W : 0;
        if (killedKey != null) control = Math.max(control, disabledChipW() - (drawer != null ? Metrics.CARET + GAP : 0));
        int chrome = 2 * PAD + (drawer != null ? Metrics.CARET + GAP : 0) + (control > 0 ? GAP + control : 0)
            + (note != null ? BADGE + BADGE_GAP : 0)
            + (beta ? Fonts.width(BETA_LABEL, Fonts.SMALL) + BADGE_GAP : 0);
        return chrome + Fonts.width(titleUpper, Fonts.TITLE) + 2;
    }

    private static int disabledChipW() { return 2 * DISABLED_PAD_X + Fonts.width(DISABLED_LABEL, Fonts.BODY); }

    /** True when this feature is switched off by the remote kill switch. */
    public boolean killed() { return killedKey != null && FeatureFlags.isKilled(killedKey); }

    private boolean drawerOpen() { return drawer != null && expanded && !killed(); }

    @Override public int prefWidth() { return Integer.MAX_VALUE; }

    @Override public int layout(int x, int y, int width) {
        this.x = x; this.y = y; this.w = width;
        boolean killed = killed();
        boolean hasCaret = drawer != null && !killed;
        boolean showNote = note != null && !killed;

        int innerX = x + PAD;
        int innerW = Math.max(1, width - 2 * PAD);

        if (killed) {
            controlW = disabledChipW();
            controlH = Metrics.BIG_TOGGLE_H;   // same height as the toggle it replaces
        } else if (toggle != null) {
            controlW = Metrics.BIG_TOGGLE_W;
            controlH = Metrics.BIG_TOGGLE_H;
        } else {
            controlW = 0;
            controlH = 0;
        }

        // One header line: the caret button, the title and the toggle share a centre line. The
        // description sits below it and may use the full width under the toggle.
        int titleRowH = Math.max(hasCaret ? Metrics.CARET : Metrics.TITLE_LINE_H, controlH);
        textX = innerX + (hasCaret ? Metrics.CARET + GAP : 0);
        int textRight = innerX + innerW - (controlW > 0 ? controlW + GAP : 0);
        textW = Math.max(1, textRight - textX);              // title only
        int bodyW = Math.max(1, innerX + innerW - textX);     // description

        descLines = (description == null || description.isEmpty())
            ? List.of() : Fonts.wrap(description, bodyW);

        int top = y + PAD;
        caretX = innerX;
        caretY = top + (titleRowH - Metrics.CARET) / 2;
        titleY = top + (titleRowH - Fonts.height(Fonts.TITLE)) / 2;
        descY = top + titleRowH + TITLE_GAP;

        int contentH = titleRowH;
        if (!descLines.isEmpty()) contentH += TITLE_GAP + descLines.size() * Metrics.LINE_H;
        // After the title, left to right: the info badge, then the BETA tag.
        boolean showBeta = beta && !killed;
        int betaW = showBeta ? Fonts.width(BETA_LABEL, Fonts.SMALL) : 0;
        int extrasW = (showBeta ? betaW + BADGE_GAP : 0) + (showNote ? BADGE + BADGE_GAP : 0);
        titleSpace = Math.max(1, textW - extrasW);
        int titleW = Math.min(Fonts.width(titleUpper, Fonts.TITLE), titleSpace);
        badgeX = textX + titleW + BADGE_GAP;
        badgeY = top + (titleRowH - BADGE) / 2;
        betaX = badgeX + (showNote ? BADGE + BADGE_GAP : 0);
        betaY = top + (titleRowH - Fonts.height(Fonts.SMALL)) / 2;

        headerH = 2 * PAD + contentH;
        controlX = x + width - PAD - controlW;
        controlY = top + (titleRowH - controlH) / 2;
        if (toggle != null && !killed) toggle.layout(controlX, controlY, controlW);

        naturalH = headerH;
        drawerBodyH = 0;
        if (drawerOpen()) {
            drawerBodyH = drawer.layout(innerX, y + headerH + 1 + PAD, innerW);
            naturalH += 1 + PAD + drawerBodyH + PAD;
        }
        this.h = Math.max(naturalH, minHeight);
        return this.h;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
        boolean killed = killed();
        Draw.bevel4(ctx, x, y, w, h, Theme.SURFACE, Theme.SURFACE_LIT, Theme.SURFACE_SHADE,
            Theme.SURFACE_LIT_SIDE, Theme.SURFACE_SHADE_SIDE, Theme.SURFACE_RING);

        if (drawerOpen()) {
            int bandTop = y + headerH;
            int bandBottom = Math.min(y + naturalH, y + h - 1);
            ctx.fill(x + 1, bandTop, x + w - 1, bandTop + 1, Theme.SURFACE_RING);
            ctx.fill(x + 1, bandTop + 1, x + w - 1, bandBottom, Theme.DRAWER);
        }

        if (drawer != null && !killed) {
            Draw.bevel(ctx, caretX, caretY, Metrics.CARET, Metrics.CARET,
                Theme.SECONDARY, Theme.SECONDARY_LIT, Theme.SECONDARY_SHADE, Theme.SURFACE_RING);
            Draw.caret(ctx, caretX + Metrics.CARET / 2, caretY + Metrics.CARET / 2,
                expanded ? Draw.Dir.DOWN : Draw.Dir.RIGHT, Theme.TEXT_LABEL);
        }

        boolean showBadge = note != null && !killed;
        int titleMax = Math.max(0, (int) Math.floor(titleSpace / (double) Fonts.snap(Fonts.TITLE)));
        Fonts.draw(ctx, Fonts.ellipsize(titleUpper, titleMax), textX, titleY,
            killed ? Theme.DISABLED_TEXT : Theme.INK_DEEP, Fonts.TITLE);
        if (beta && !killed) Fonts.draw(ctx, BETA_LABEL, betaX, betaY, Theme.OXBLOOD, Fonts.SMALL);
        int descColour = killed ? Theme.DISABLED_TEXT : Theme.TEXT_BODY;
        for (int i = 0; i < descLines.size(); i++) {
            Fonts.draw(ctx, descLines.get(i), textX, descY + i * Metrics.LINE_H, descColour);
        }
        if (showBadge) {
            // Oxblood "i" (info) badge; the note itself is a tooltip so every card header is uniform.
            Draw.bevel(ctx, badgeX, badgeY, BADGE, BADGE, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
            int stemX = badgeX + BADGE / 2;
            ctx.fill(stemX, badgeY + 2, stemX + 1, badgeY + 3, Theme.CREAM);              // dot
            ctx.fill(stemX, badgeY + 4, stemX + 1, badgeY + BADGE - 2, Theme.CREAM);      // stem
            if (mouseX >= badgeX - 1 && mouseX < badgeX + BADGE + 1 && mouseY >= badgeY - 1 && mouseY < badgeY + BADGE + 1) {
                hoveredNote = note;
            }
        }

        if (killed) {
            Draw.bevel(ctx, controlX, controlY, controlW, controlH,
                Theme.DISABLED, Theme.DISABLED_LIT, Theme.DISABLED_SHADE, Theme.DISABLED_RING);
            int textH = Fonts.height(Fonts.BODY);
            Fonts.drawCentered(ctx, DISABLED_LABEL, controlX + controlW / 2,
                controlY + (controlH - textH) / 2, Theme.DISABLED_TEXT, Fonts.BODY);
            if (mouseX >= controlX && mouseX < controlX + controlW && mouseY >= controlY && mouseY < controlY + controlH) {
                hoveredNote = DISABLED_NOTE;
            }
        } else if (toggle != null) {
            toggle.render(ctx, mouseX, mouseY);
        }

        if (drawerOpen()) drawer.render(ctx, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !contains(mx, my)) return false;
        // A non-left click reaches the drawer body only (so a listening KeyChip in there can
        // bind a mouse button); the header, caret and toggle stay left-click only.
        if (button != 0) return drawerOpen() && drawer.mouseClicked(mx, my, button);
        // Forwarded first (and unconditionally within the row) so focused text fields in this
        // row's own drawer see clicks that land outside them as "outside" clicks.
        if (drawerOpen() && drawer.mouseClicked(mx, my, button)) return true;
        if (killed()) return true;
        if (toggle != null && toggle.mouseClicked(mx, my, button)) return true;
        if (my < y + headerH && drawer != null) {
            expanded = !expanded;
            if (!expanded) drawer.unfocus();
            IslesClient.playMenuClickSound();
        }
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my) {
        return drawerOpen() && drawer.mouseDragged(mx, my);
    }

    @Override public void mouseReleased() {
        if (drawer != null) drawer.mouseReleased();
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        return drawerOpen() && contains(mx, my) && drawer.mouseScrolled(mx, my, amount);
    }

    @Override public boolean keyPressed(KeyInput in) {
        return drawerOpen() && drawer.keyPressed(in);
    }

    @Override public boolean charTyped(CharInput in) {
        return drawerOpen() && drawer.charTyped(in);
    }

    @Override public void unfocus() {
        if (drawer != null) drawer.unfocus();
    }
}
