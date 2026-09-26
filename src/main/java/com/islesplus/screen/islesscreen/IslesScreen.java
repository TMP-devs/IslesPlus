package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import com.islesplus.IslesPlusConfig;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.screen.hudedit.HudEditScreen;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.EmptyBox;
import com.islesplus.ui.widgets.NoteTooltip;
import com.islesplus.ui.widgets.TextField;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The /ip dashboard: a parchment scroll holding three tabs of {@link FeatureRow} cards in a
 * two-column grid, plus a stack of modal overlays (dropdown lists, dialogs) drawn on top.
 *
 * <p>Shell concerns only: frame, tabs, grid, scrolling, overlay hosting and input routing. The
 * cards themselves live in {@link Rows}; the parchment frame in {@link ScrollFrame}.
 */
public class IslesScreen extends Screen implements OverlayHost {
    private static final String[] TAB_LABELS = {"QoL", "Inventory", "Node Farming", "Rift", "Keybinds"};
    private static final Tab[] TABS = {Tab.QOL, Tab.INVENTORY, Tab.NODE_FARMING, Tab.RIFT, Tab.KEYBINDS};
    private static final String SIGNATURE = "by Chrrisk & Scrolls";
    private static final int TAB_PAD_X = 8, TAB_PAD_X_ACTIVE = 10, TAB_RIVET_GAP = 4, RIVET_W = 3;
    private static final int TABS_TO_GRID_GAP = 6, SCROLL_STEP = 20;
    /** Strip under the card grid that holds the makers' signature. */
    // Measured on a screenshot at size 20: the script's ink starts 10 px ABOVE the y it is drawn at
    // (tall capitals and swashes) and ends 13 px below it. The footer strip is sized from that, so
    // the whole signature sits under the card area with clear room above it.
    private static final int SIGN_RISE = 10, SIGN_DROP = 13, SIGN_CLEARANCE = 8, SIGN_BOTTOM_MARGIN = 4;
    private static final int SIGN_RIGHT_INSET = 5;
    private static final int FOOTER_H = SIGN_CLEARANCE + SIGN_RISE + SIGN_DROP + SIGN_BOTTOM_MARGIN;

    private final ScrollFrame frame = new ScrollFrame();
    private final Map<Tab, List<FeatureRow>> rows = new EnumMap<>(Tab.class);
    private final List<Widget> overlays = new ArrayList<>();

    private Tab activeTab = Tab.QOL;
    private int scroll;
    private double scrollRemainder;
    private boolean singleColumn;
    private final KonamiCode konami = new KonamiCode();
    private static final int SEARCH_MIN_W = 50, SEARCH_MAX_W = 120, TOP_BAR_GAP = 4;
    /** The tools row above the tabs: search and Edit HUD on the right. */
    private static final int TOOL_ROW_H = Metrics.BUTTON_H, TOOLS_TO_TABS_GAP = 5;
    /** The search box's text. While it is not blank the grid shows every matching card from
     * every tab, not just the open one. */
    private String query = "";
    private final TextField search = new TextField(() -> query, v -> { query = v; scroll = 0; }, "Search", 40);
    private final EmptyBox noMatches = new EmptyBox("No features match");
    /** Opens the HUD editor: where every on-screen element sits and how big it is. */
    private final Button editHud = new Button("Edit HUD", Button.Kind.PRIMARY,
        () -> { if (this.client != null) this.client.setScreen(new HudEditScreen(this)); });
    private int gridX, gridY, gridW, gridH, gridContentH;
    /** The cards the last layout pass placed. Drawn from this list, not a fresh activeRows(), so a
     * card a features refresh reveals mid-frame is never drawn before it has been laid out. */
    private List<FeatureRow> laidOut = List.of();

    public IslesScreen() {
        super(Text.literal("IslesPlus"));
        rows.put(Tab.QOL, Rows.qol(this));
        rows.put(Tab.INVENTORY, Rows.inventory(this));
        rows.put(Tab.NODE_FARMING, Rows.nodeFarming(this));
        rows.put(Tab.RIFT, Rows.rift(this));
        rows.put(Tab.KEYBINDS, Rows.keybinds());
    }

    private void initUi() {
        Fonts.resetFallbackCheck();
        // Size the scroll so every card title (on any tab) fits on one line.
        int cardW = 0;
        for (List<FeatureRow> tab : rows.values()) for (FeatureRow row : tab) cardW = Math.max(cardW, row.widthForFullTitle());
        frame.layout(this.client, this.width, this.height, 2 * cardW + Metrics.GRID_GAP);
        gridX = frame.contentX;
        gridW = frame.contentW;
        // The scroll cannot grow past the window. When two columns would be too narrow for the
        // longest title (1080p at GUI scale 4, small windows), use one full-width column rather than
        // cut titles short.
        singleColumn = (gridW - Metrics.GRID_GAP) / 2 < cardW;
        gridY = tabBottom() + TABS_TO_GRID_GAP;
        gridH = Math.max(1, frame.contentY + frame.contentH - FOOTER_H - gridY);
    }

    /** The active tab's cards, minus any the json has "killed" (re-checked every call, so a
     * refreshed features json adds or removes cards while the screen is open). */
    private List<FeatureRow> activeRows() {
        List<FeatureRow> shown = new ArrayList<>();
        if (searching()) {
            // Searching ignores the tabs: every matching card, in tab order.
            for (Tab tab : TABS) for (FeatureRow row : rows.get(tab)) if (!row.hidden() && row.matchesSearch(query)) shown.add(row);
            return shown;
        }
        for (FeatureRow row : rows.get(activeTab)) if (!row.hidden()) shown.add(row);
        return shown;
    }

    private boolean searching() { return !query.isBlank(); }

    /** Whether tab i is drawn as the open one. None is while searching: the results span every tab. */
    private boolean tabActive(int i) { return !searching() && TABS[i] == activeTab; }

    private int tabBottom() { return frame.contentY + TOOL_ROW_H + TOOLS_TO_TABS_GAP + Metrics.TAB_H + 1; }

    /** The tools row above the tabs: the search box and Edit HUD on the right; the search box
     * takes what room is left, up to SEARCH_MAX_W. */
    private void layoutTopBar() {
        int top = frame.contentY;
        int right = frame.contentX + frame.contentW;
        int editW = editHud.prefWidth();
        editHud.layout(right - editW, top + (TOOL_ROW_H - Metrics.BUTTON_H) / 2, editW);
        int room = right - editW - TOP_BAR_GAP - frame.contentX;
        int searchW = Math.max(SEARCH_MIN_W, Math.min(SEARCH_MAX_W, room));
        search.layout(right - editW - TOP_BAR_GAP - searchW, top + (TOOL_ROW_H - Metrics.FIELD_H) / 2, searchW);
    }

    /** Tab label size: the title size, or body size when five tabs at the title size would not fit. */
    private float tabScale() {
        int total = 0;
        for (int i = 0; i < TABS.length; i++) {
            total += 2 * TAB_PAD_X_ACTIVE + 2 * RIVET_W + 2 * TAB_RIVET_GAP + Fonts.width(TAB_LABELS[i], Fonts.TITLE) + Metrics.TAB_GAP;
        }
        return total <= frame.contentW ? Fonts.TITLE : Fonts.BODY;
    }

    /** The row the tabs' bottom outline is on: where the shelf line runs. */
    private int shelfY() { return tabBottom(); }

    private int tabWidth(int i) {
        int pad = tabActive(i) ? TAB_PAD_X_ACTIVE : TAB_PAD_X;
        return 2 * pad + 2 * RIVET_W + 2 * TAB_RIVET_GAP + Fonts.width(TAB_LABELS[i], tabScale());
    }

    private int tabX(int i) {
        int x = frame.contentX;
        for (int j = 0; j < i; j++) x += tabWidth(j) + Metrics.TAB_GAP;
        return x;
    }

    /** The active tab is drawn (and hit-tested) 1 px taller, sharing the others' bottom edge. */
    private int tabHeight(int i) { return tabActive(i) ? Metrics.TAB_H + 1 : Metrics.TAB_H; }

    // ==============================
    // Layout
    // ==============================

    /** Lays out every card of the active tab, clamping the scroll offset if the content shrank. */
    private void layoutGrid() {
        layoutPass();
        int max = maxScroll();
        if (scroll > max || scroll < 0) {
            scroll = Math.max(0, Math.min(scroll, max));
            layoutPass();
        }
    }

    private void layoutPass() {
        int gap = Metrics.GRID_GAP;
        List<FeatureRow> list = activeRows();
        laidOut = list;
        int top = gridY - scroll;
        // the Keybinds page is one card: it gets the whole width
        if (singleColumn || (!searching() && activeTab == Tab.KEYBINDS)) {
            int y = top;
            for (FeatureRow row : list) {
                row.minHeight = 0;
                y += row.layout(gridX, y, Math.max(1, gridW)) + gap;
            }
            gridContentH = list.isEmpty() ? 0 : y - top - gap;
            return;
        }
        int cardW = Math.max(1, (gridW - gap) / 2);
        // Two independent columns: a pair of cards shares its COLLAPSED height so the closed grid
        // stays even, but an expanded card only lengthens its own column, its neighbour keeps its
        // size and only the cards below it in the same column shift down.
        int leftY = top, rightY = top;
        int rightX = gridX + cardW + gap;
        for (int i = 0; i < list.size(); i += 2) {
            FeatureRow a = list.get(i);
            FeatureRow b = i + 1 < list.size() ? list.get(i + 1) : null;
            int closedH = collapsedHeight(a, gridX, leftY, cardW);
            if (b != null) closedH = Math.max(closedH, collapsedHeight(b, rightX, rightY, cardW));
            a.minHeight = closedH;
            leftY += a.layout(gridX, leftY, cardW) + gap;
            if (b != null) {
                b.minHeight = closedH;
                rightY += b.layout(rightX, rightY, cardW) + gap;
            }
        }
        gridContentH = list.isEmpty() ? 0 : Math.max(leftY, rightY) - top - gap;
    }

    /** Height the card would have with its drawer closed (measured without disturbing its state). */
    private static int collapsedHeight(FeatureRow row, int x, int y, int width) {
        boolean wasExpanded = row.expanded;
        row.expanded = false;
        row.minHeight = 0;
        int h = row.layout(x, y, width);
        row.expanded = wasExpanded;
        return h;
    }

    /** A card's outer ring is drawn 1 px outside its bounds, so when the content overflows the
     * view, the scroll range runs a little past the last card: otherwise the bottom edge of the
     * last card is clipped away at the end of the scroll. */
    private static final int SCROLL_END_SLACK = 2;

    private int maxScroll() {
        int overflow = gridContentH - gridH;
        return overflow <= 0 ? 0 : overflow + SCROLL_END_SLACK;
    }

    /** Whether a click point is inside the card grid AND inside the scroll's revealed area. */
    private boolean inGrid(double mx, double my) {
        return mx >= gridX && mx < gridX + gridW && my >= gridY && my < gridY + gridH
            && my < frame.revealBottom();
    }

    // ==============================
    // Render
    // ==============================

    private void renderUi(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean overlaid = !overlays.isEmpty();
        int hx = overlaid ? -1 : mouseX;
        int hy = overlaid ? -1 : mouseY;

        ctx.fill(0, 0, this.width, this.height, Theme.SCRIM);
        frame.render(ctx);
        layoutGrid();
        FeatureRow.hoveredNote = null;

        int revealBottom = frame.revealBottom();
        if (revealBottom > frame.contentY) {
            // 1 px of slack all round so the cards' and tabs' outer rings are not clipped away.
            ctx.enableScissor(frame.contentX - 1, frame.contentY - 1, frame.contentX + frame.contentW + 1, revealBottom);
            drawTabs(ctx);
            layoutTopBar();
            editHud.render(ctx, hx, hy);
            search.render(ctx, hx, hy);
            if (revealBottom > gridY) {
                // Cards are clipped right under the shelf line, not at the top of the card area: at
                // rest there is a gap between the line and the first card, and a scrolled card
                // slides up through that gap and disappears under the line itself.
                int top = shelfY() + 1;
                ctx.enableScissor(gridX - 1, top, gridX + gridW + 1, Math.min(gridY + gridH, revealBottom));
                for (FeatureRow row : laidOut) row.render(ctx, hx, hy);
                if (searching() && laidOut.isEmpty()) {
                    noMatches.layout(gridX, gridY, gridW);
                    noMatches.render(ctx, hx, hy);
                }
                ctx.disableScissor();
                // The shelf: one ink rule across the whole content width, level with the bottom edge
                // of the tabs. Only while the list is scrolled down - it marks the edge cards are
                // disappearing under, so with nothing scrolled off there is nothing for it to mark.
                if (scroll > 0) {
                    ctx.fill(frame.contentX, shelfY(), frame.contentX + frame.contentW, shelfY() + 1, Theme.INK);
                }
            }
            drawSignature(ctx);   // after the cards, so it is always on top of them
            ctx.disableScissor();
        }

        if (!overlaid && FeatureRow.hoveredNote != null && mouseY < revealBottom
            && mouseY >= gridY && mouseY < gridY + gridH) {
            drawNoteTooltip(ctx, FeatureRow.hoveredNote, mouseX, mouseY);
        }

        for (Widget overlay : new ArrayList<>(overlays)) {
            ctx.createNewRootLayer();   // item icons below (cards, a lower dialog) must not show through
            overlay.layout(0, 0, this.width);
            overlay.render(ctx, mouseX, mouseY);
        }
    }

    /** Warning tooltip for a card's "!" badge: the old note strip's look (tan, oxblood left edge)
     * as a floating panel beside the cursor, kept inside the window. */
    private void drawNoteTooltip(DrawContext ctx, String text, int mouseX, int mouseY) {
        ctx.createNewRootLayer();   // above any item icons on the cards
        NoteTooltip.draw(ctx, text, mouseX, mouseY, this.width, this.height);
    }

    private void drawTabs(DrawContext ctx) {
        int bottom = tabBottom();
        for (int i = 0; i < TABS.length; i++) {
            boolean active = tabActive(i);
            int pad = active ? TAB_PAD_X_ACTIVE : TAB_PAD_X;
            int tw = tabWidth(i), th = tabHeight(i);
            int tx = tabX(i), ty = bottom - th;

            if (active) {
                Draw.bevel4(ctx, tx, ty, tw, th, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE,
                    Theme.OXBLOOD_LIT_SIDE, Theme.OXBLOOD_SHADE_SIDE, Theme.INK);
            } else {
                Draw.bevel4(ctx, tx, ty, tw, th, Theme.TAB_OFF, Theme.TAB_OFF_LIT, Theme.TAB_OFF_SHADE,
                    Theme.TAB_OFF_LIT, Theme.TAB_OFF_SHADE, Theme.INK);
            }

            int rivet = active ? Theme.RIVET_ON : Theme.RIVET_OFF;
            int cy = ty + th / 2;
            Draw.rivet(ctx, tx + pad + 1, cy, rivet);
            Draw.rivet(ctx, tx + tw - pad - 2, cy, rivet);

            float scale = tabScale();
            int textH = Fonts.height(scale);
            Fonts.draw(ctx, TAB_LABELS[i], tx + pad + RIVET_W + TAB_RIVET_GAP, ty + (th - textH) / 2,
                active ? Theme.TAB_ON_TEXT : Theme.TAB_OFF_TEXT, scale);
        }

    }

    /** The makers' signature, signed in script at the bottom right of the sheet. */
    private void drawSignature(DrawContext ctx) {
        int bottom = frame.contentY + frame.contentH;
        int right = frame.contentX + frame.contentW;
        if (Fonts.scriptAvailable()) {
            // The final letter's swash overhangs the measured width, and the content area is clipped
            // at its right edge - so keep the signature a few pixels in from it.
            int sigW = Fonts.scriptWidth(SIGNATURE);
            int sigX = right - SIGN_RIGHT_INSET - sigW;
            int sigY = bottom - SIGN_BOTTOM_MARGIN - SIGN_DROP;
            // One even patch of plain parchment behind the whole signature: the sheet's lighter and
            // darker blotches would otherwise put a different colour behind each name.
            ctx.fill(sigX - 4, sigY - SIGN_RISE - 2, right, sigY + SIGN_DROP + 2, Theme.PARCHMENT);
            Fonts.drawScript(ctx, SIGNATURE, sigX, sigY, Theme.TEXT_TITLE);
        } else {
            Fonts.draw(ctx, SIGNATURE, right - Fonts.width(SIGNATURE), bottom - (FOOTER_H + Fonts.GLYPH_H) / 2, Theme.TEXT_TITLE);
        }
    }

    // ==============================
    // Overlay host
    // ==============================

    @Override public void openOverlay(Widget overlay) { overlays.add(overlay); }

    /** Removes the overlay and everything stacked above it, unfocusing each one on the way out.
     * The unfocus is what lets a closed overlay reset its owner's state, a Dropdown's list
     * clears the owning Dropdown's {@code open} flag (so its caret stops pointing up) and a
     * Dialog commits any focused field in its body, no matter how the overlay was dismissed:
     * by its own click, by Escape, by a Dialog below it closing, or by the screen closing. */
    @Override public void closeOverlay(Widget overlay) {
        int i = overlays.indexOf(overlay);
        if (i < 0) return;
        while (overlays.size() > i) overlays.remove(overlays.size() - 1).unfocus();
    }

    @Override public int screenWidth() { return this.width; }

    @Override public int screenHeight() { return this.height; }

    @Override public void closeScreen() { this.close(); }

    private Widget topOverlay() {
        return overlays.isEmpty() ? null : overlays.get(overlays.size() - 1);
    }

    // ==============================
    // Input
    // ==============================

    private boolean mouseClickedUi(Click click, boolean doubled) {
        Widget top = topOverlay();
        if (top != null) {
            top.mouseClicked(click.x(), click.y(), click.button());
            return true;
        }
        double mx = click.x(), my = click.y();
        layoutGrid();

        // A non-left click exists here only so a listening KeyChip can bind a mouse button: it
        // goes straight to the rows through the same grid/reveal gate as a left click, but with
        // no tab handling and neither the commit-before-apply pre-pass nor the post-consume
        // sweep, a right click must not commit a focused field, and must not cancel the chip
        // that is waiting for it.
        // Right click on the search box clears it (and leaves it focused to type again), like the
        // inventory search bar.
        if (click.button() == 1) {
            layoutTopBar();
            if (my < frame.revealBottom() && search.contains(mx, my)) {
                unfocusAll();
                query = "";
                scroll = 0;
                search.mouseClicked(mx, my, 0);
                return true;
            }
        }
        if (click.button() != 0) {
            if (inGrid(mx, my)) {
                for (FeatureRow row : activeRows()) {
                    if (row.mouseClicked(mx, my, click.button())) return true;
                }
            }
            return super.mouseClicked(click, doubled);
        }

        layoutTopBar();
        if (my < frame.revealBottom() && search.mouseClicked(mx, my, 0)) {
            for (List<FeatureRow> list : rows.values()) for (FeatureRow row : list) row.unfocus();
            return true;
        }
        search.unfocus();
        if (my < frame.revealBottom() && editHud.mouseClicked(mx, my, 0)) {
            unfocusAll();
            return true;
        }

        for (int i = 0; i < TABS.length; i++) {
            int tw = tabWidth(i), tx = tabX(i), ty = tabBottom() - tabHeight(i);
            if (mx >= tx && mx < tx + tw && my >= ty && my < tabBottom()) {
                unfocusAll();
                // Picking a tab ends a search: back to plain pages.
                if (TABS[i] != activeTab || searching()) {
                    activeTab = TABS[i];
                    query = "";
                    scroll = 0;
                    IslesClient.playMenuClickSound();
                }
                return true;
            }
        }

        // Commit before apply: a focused field in a row the click cannot belong to gives up
        // focus (and so commits its draft) before any control under the point acts on it.
        List<FeatureRow> list = activeRows();
        for (FeatureRow row : list) if (!row.contains(mx, my)) row.unfocus();

        FeatureRow consumer = null;
        if (inGrid(mx, my)) {
            for (FeatureRow row : list) {
                if (row.mouseClicked(mx, my, 0)) { consumer = row; break; }
            }
        }
        // Post-consume sweep: catches an overlapping row that contains the point but was not
        // the one that consumed the click, and unfocuses everything when nothing consumed it.
        for (FeatureRow row : list) if (row != consumer) row.unfocus();
        return consumer != null || super.mouseClicked(click, doubled);
    }

    private boolean mouseDraggedUi(Click click, double deltaX, double deltaY) {
        Widget top = topOverlay();
        if (top != null) {
            top.mouseDragged(click.x(), click.y());
            return true;
        }
        for (FeatureRow row : activeRows()) {
            if (row.mouseDragged(click.x(), click.y())) return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private boolean mouseReleasedUi(Click click) {
        Widget top = topOverlay();
        if (top != null) {
            top.mouseReleased();
            return true;
        }
        for (FeatureRow row : activeRows()) row.mouseReleased();
        editHud.mouseReleased();
        return super.mouseReleased(click);
    }

    private boolean mouseScrolledUi(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Widget top = topOverlay();
        if (top != null) {
            top.mouseScrolled(mouseX, mouseY, verticalAmount);
            return true;
        }
        for (FeatureRow row : activeRows()) {
            if (row.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
        }
        // Precision trackpads send many tiny deltas; truncating each to a whole pixel drops them all,
        // so the fraction is carried over to the next event.
        scrollRemainder += verticalAmount * SCROLL_STEP;
        int whole = (int) scrollRemainder;
        scrollRemainder -= whole;
        scroll = Math.max(0, Math.min(scroll - whole, maxScroll()));
        return true;
    }

    private boolean keyPressedUi(KeyInput input) {
        Widget top = topOverlay();
        if (top != null) {
            if (!top.keyPressed(input) && input.key() == GLFW.GLFW_KEY_ESCAPE) closeOverlay(top);
            return true;
        }
        if (search.focused && search.keyPressed(input)) { konami.reset(); return true; }
        for (FeatureRow row : activeRows()) {
            if (row.keyPressed(input)) { konami.reset(); return true; }   // typing in a field is not the code
        }
        // Not while typing (a focused field does not consume arrows or letters, so its keys would
        // count), and not with Ctrl/Alt/Shift held (Ctrl+A is "select all", not an A).
        if (TextField.anyFocused() || input.modifiers() != 0) {
            konami.reset();
        } else if (konami.feed(input.key())) {
            IslesClient.playMenuClickSound();
            SecretPage.open(this, frame.contentW);
            return true;
        }
        return super.keyPressed(input);
    }

    private boolean charTypedUi(CharInput input) {
        Widget top = topOverlay();
        if (top != null) {
            top.charTyped(input);
            return true;
        }
        if (search.focused && search.charTyped(input)) return true;
        for (FeatureRow row : activeRows()) {
            if (row.charTyped(input)) return true;
        }
        return super.charTyped(input);
    }

    private void unfocusAll() {
        search.unfocus();
        for (List<FeatureRow> list : rows.values()) {
            for (FeatureRow row : list) row.unfocus();
        }
    }

    /** Also reached when something else replaces the screen (the server opening a menu, a
     * disconnect): anything still being typed is committed and saved, exactly as on close(). */
    @Override
    public void removed() {
        unfocusAll();
        for (Widget overlay : new ArrayList<>(overlays)) overlay.unfocus();
        IslesPlusConfig.save();
        super.removed();
    }

    @Override
    public void close() {
        unfocusAll();
        // Copy first: an overlay's unfocus() commits a draft, and a commit is free to reach back
        // into the host. Same reason the render loop iterates a copy.
        for (Widget overlay : new ArrayList<>(overlays)) overlay.unfocus();
        overlays.clear();
        if (this.client != null) this.client.setScreen(null);
    }

    // /ip UI: its text is in the Isles+ faces (Fonts.islesUi), everything else's in the game's font.
    @Override protected void init() { Fonts.islesUi(this::initUi); }
    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) { Fonts.islesUi(() -> renderUi(ctx, mouseX, mouseY, delta)); }
    @Override public boolean mouseClicked(Click click, boolean doubled) { return Fonts.islesUi(() -> mouseClickedUi(click, doubled)); }
    @Override public boolean mouseDragged(Click click, double deltaX, double deltaY) { return Fonts.islesUi(() -> mouseDraggedUi(click, deltaX, deltaY)); }
    @Override public boolean mouseReleased(Click click) { return Fonts.islesUi(() -> mouseReleasedUi(click)); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { return Fonts.islesUi(() -> mouseScrolledUi(mouseX, mouseY, horizontalAmount, verticalAmount)); }
    @Override public boolean keyPressed(KeyInput input) { return Fonts.islesUi(() -> keyPressedUi(input)); }
    @Override public boolean charTyped(CharInput input) { return Fonts.islesUi(() -> charTypedUi(input)); }
}
