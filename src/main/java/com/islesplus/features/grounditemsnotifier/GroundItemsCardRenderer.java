package com.islesplus.features.grounditemsnotifier;

import com.islesplus.screen.islesscreen.SoundEditorOverlay;
import com.islesplus.sound.SoundConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Handles all rendering and interaction for the Ground Items Notifier
 * expanded card section — item sub-cards, dropdown overlay, and text input.
 */
public final class GroundItemsCardRenderer {

    // Colors matching ScreenColors
    private static final int PANEL_SOFT   = 0xEE262626;
    private static final int PANEL_HOVER  = 0xEE313131;
    private static final int BORDER       = 0xFF464646;
    private static final int TEXT_PRIMARY = 0xFFF0F0F0;
    private static final int TEXT_MUTED   = 0xFFB3B3B3;
    private static final int POSITIVE     = 0xFF38D286;
    private static final int ACCENT       = 0xFF5AB8FF;
    private static final int NEGATIVE     = 0xFFE74C3C;
    private static final int BG_ITEM      = 0xEE1A1A1A;

    // ---- UI state (persists while screen is open) ----
    public static int focusedCustomIdx  = -1; // which WatchedItem's custom field has keyboard focus

    private GroundItemsCardRenderer() {}

    // ================================================================
    // Height calculations
    // ================================================================

    public static int itemSubCardHeight(GroundItemsNotifier.WatchedItem item) {
        int h = 4 + 16 + 4 + 14 + 4 + 16; // name row (toggle + delete) + keyword field + feature toggles row
        if (item.soundPing) h += 16; // "Edit Sound" button row
        h += 4; // bottom pad
        return h;
    }

    /** Total height of the expanded body section (all item cards + add button). */
    public static int getExpandedHeight() {
        List<GroundItemsNotifier.WatchedItem> items = GroundItemsNotifier.watchedItems;
        int h = 0;
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) h += 4; // gap between item cards
            h += itemSubCardHeight(items.get(i));
        }
        h += 4 + 16 + 4; // gap + add button + bottom pad
        return h;
    }

    // ================================================================
    // Drawing
    // ================================================================

    /**
     * Draw the full expanded body at (x, y, width).
     * Called from IslesScreen after the header separator.
     */
    public static void draw(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        List<GroundItemsNotifier.WatchedItem> items = GroundItemsNotifier.watchedItems;
        int curY = y;

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) curY += 4;
            GroundItemsNotifier.WatchedItem item = items.get(i);
            drawItemSubCard(ctx, tr, item, i, x, curY, width, mouseX, mouseY);
            curY += itemSubCardHeight(item);
        }

        // Add button
        curY += 4;
        int btnW = 80;
        int btnX = x + (width - btnW) / 2;
        boolean btnHover = isInside(mouseX, mouseY, btnX, curY, btnW, 14);
        ctx.fill(btnX, curY, btnX + btnW, curY + 14, btnHover ? PANEL_HOVER : PANEL_SOFT);
        ctx.fill(btnX, curY, btnX + btnW, curY + 1, BORDER);
        ctx.fill(btnX, curY + 13, btnX + btnW, curY + 14, BORDER);
        String addLabel = "+ Add Item";
        ctx.drawText(tr, Text.literal(addLabel), btnX + (btnW - tr.getWidth(addLabel)) / 2, curY + 3, TEXT_MUTED, false);
    }

    private static void drawItemSubCard(DrawContext ctx, TextRenderer tr,
                                        GroundItemsNotifier.WatchedItem item, int idx,
                                        int x, int y, int width, int mouseX, int mouseY) {
        int h = itemSubCardHeight(item);
        int innerX = x + 8;
        int innerW = width - 16;

        // Sub-card background + border
        ctx.fill(x, y, x + width, y + h, BG_ITEM);
        ctx.fill(x, y, x + width, y + 1, BORDER);
        ctx.fill(x, y + h - 1, x + width, y + h, BORDER);

        // -- Row 1: [enabled toggle] [× delete] --
        int row1Y = y + 4;

        drawToggle(ctx, innerX, row1Y + 2, 34, 12, item.enabled);

        int delX = innerX + innerW - 12;
        ctx.drawText(tr, Text.literal("×"), delX + 2, row1Y + 3, NEGATIVE, false);

        int curY = row1Y + 16 + 4;

        // -- Row 2: keyword text field --
        boolean focused = idx == focusedCustomIdx;
        drawTextField(ctx, tr, innerX, curY, innerW, item.customKeyword, focused);
        curY += 14 + 4;

        // -- Row 3: feature toggles [Line] [Notif] [Glow] [Sound] --
        curY += 2;
        int featW = innerW / 4;
        drawMiniToggle(ctx, tr, innerX,              curY, "Line",  item.lineTracker);
        drawMiniToggle(ctx, tr, innerX + featW,      curY, "Screen warn", item.screenNotifier);
        drawMiniToggle(ctx, tr, innerX + featW * 2,  curY, "Glow",  item.highlight);
        drawMiniToggle(ctx, tr, innerX + featW * 3,  curY, "Sound", item.soundPing);
        curY += 12;

        // -- Row 4 (conditional): "Edit Sound" button --
        if (item.soundPing) {
            curY += 2;
            int editBtnW = 80;
            int editBtnX = innerX + (innerW - editBtnW) / 2;
            boolean editHov = isInside(mouseX, mouseY, editBtnX, curY, editBtnW, 12);
            ctx.fill(editBtnX, curY, editBtnX + editBtnW, curY + 12, editHov ? PANEL_HOVER : PANEL_SOFT);
            ctx.fill(editBtnX, curY, editBtnX + editBtnW, curY + 1, BORDER);
            String editLabel = "Edit Sound";
            ctx.drawText(tr, Text.literal(editLabel),
                editBtnX + (editBtnW - tr.getWidth(editLabel)) / 2, curY + 2, TEXT_PRIMARY, false);
        }
    }

    // ================================================================
    // Click handling
    // ================================================================

    /**
     * Handle a mouse click inside the expanded body area.
     * Coordinates match those passed to draw().
     */
    public static boolean onClick(int mouseX, int mouseY, int x, int y, int width) {
        List<GroundItemsNotifier.WatchedItem> items = GroundItemsNotifier.watchedItems;
        int curY = y;

        for (int i = 0; i < items.size(); i++) {
            if (i > 0) curY += 4;
            GroundItemsNotifier.WatchedItem item = items.get(i);
            int h = itemSubCardHeight(item);

            if (isInside(mouseX, mouseY, x, curY, width, h)) {
                focusedCustomIdx = -1;
                return onItemCardClick(mouseX, mouseY, item, i, x, curY, width);
            }
            curY += h;
        }

        // Add button
        curY += 4;
        int btnW = 80;
        int btnX = x + (width - btnW) / 2;
        if (isInside(mouseX, mouseY, btnX, curY, btnW, 14)) {
            GroundItemsNotifier.watchedItems.add(new GroundItemsNotifier.WatchedItem());
            com.islesplus.IslesPlusConfig.save();
            return true;
        }

        focusedCustomIdx = -1;
        return false;
    }

    private static boolean onItemCardClick(int mouseX, int mouseY,
                                           GroundItemsNotifier.WatchedItem item, int idx,
                                           int x, int y, int width) {
        int innerX = x + 8;
        int innerW = width - 16;
        int row1Y = y + 4;

        // Enabled toggle
        if (isInside(mouseX, mouseY, innerX, row1Y + 2, 34, 12)) {
            item.enabled = !item.enabled;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }

        // Delete button
        int delX = innerX + innerW - 12;
        if (isInside(mouseX, mouseY, delX, row1Y + 2, 12, 12)) {
            GroundItemsNotifier.watchedItems.remove(idx);
            if (focusedCustomIdx == idx) focusedCustomIdx = -1;
            else if (focusedCustomIdx > idx) focusedCustomIdx--;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }

        int curY = row1Y + 16 + 4;

        // Keyword field
        if (isInside(mouseX, mouseY, innerX, curY, innerW, 12)) {
            focusedCustomIdx = idx;
            return true;
        }
        curY += 14 + 4;

        // Feature toggles row
        int featW = innerW / 4;
        if (isInside(mouseX, mouseY, innerX, curY, featW, 12)) {
            item.lineTracker = !item.lineTracker;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        if (isInside(mouseX, mouseY, innerX + featW, curY, featW, 12)) {
            item.screenNotifier = !item.screenNotifier;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        if (isInside(mouseX, mouseY, innerX + featW * 2, curY, featW, 12)) {
            item.highlight = !item.highlight;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        if (isInside(mouseX, mouseY, innerX + featW * 3, curY, featW, 12)) {
            item.soundPing = !item.soundPing;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        curY += 12;

        // "Edit Sound" button row
        if (item.soundPing) {
            curY += 2;
            int editBtnW = 80;
            int editBtnX = innerX + (innerW - editBtnW) / 2;
            if (isInside(mouseX, mouseY, editBtnX, curY, editBtnW, 12)) {
                SoundEditorOverlay.open(
                    () -> item.soundConfig,
                    v  -> item.soundConfig = v,
                    GroundItemsNotifier.DEFAULT_ITEM_SOUND,
                    com.islesplus.IslesPlusConfig::save
                );
                return true;
            }
        }

        return true; // consumed (any click inside sub-card)
    }

    // ================================================================
    // Keyboard input
    // ================================================================

    public static boolean onChar(CharInput input) {
        if (focusedCustomIdx < 0 || focusedCustomIdx >= GroundItemsNotifier.watchedItems.size()) return false;
        if (!input.isValidChar()) return false;
        GroundItemsNotifier.WatchedItem item = GroundItemsNotifier.watchedItems.get(focusedCustomIdx);
        item.customKeyword += input.asString();
        com.islesplus.IslesPlusConfig.save();
        return true;
    }

    public static boolean onKey(KeyInput input) {
        if (focusedCustomIdx < 0) return false;
        int key = input.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (focusedCustomIdx < GroundItemsNotifier.watchedItems.size()) {
                GroundItemsNotifier.WatchedItem item = GroundItemsNotifier.watchedItems.get(focusedCustomIdx);
                if (!item.customKeyword.isEmpty()) {
                    item.customKeyword = item.customKeyword.substring(0, item.customKeyword.length() - 1);
                    com.islesplus.IslesPlusConfig.save();
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            focusedCustomIdx = -1;
            return true;
        }
        return false;
    }

    // ================================================================
    // Small drawing helpers
    // ================================================================

    private static void drawToggle(DrawContext ctx, int x, int y, int w, int h, boolean enabled) {
        ctx.fill(x, y, x + w, y + h, enabled ? POSITIVE : 0xFF5B6678);
        int knobSize = h - 4;
        int knobX = enabled ? x + w - knobSize - 2 : x + 2;
        ctx.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, 0xFFFFFFFF);
    }

    private static void drawMiniToggle(DrawContext ctx, TextRenderer tr, int x, int y, String label, boolean enabled) {
        // 8×8 checkbox-style indicator
        ctx.fill(x, y + 2, x + 8, y + 10, BORDER);
        ctx.fill(x + 1, y + 3, x + 7, y + 9, PANEL_SOFT);
        if (enabled) ctx.fill(x + 2, y + 4, x + 6, y + 8, POSITIVE);
        ctx.drawText(tr, Text.literal(label), x + 10, y + 2, enabled ? TEXT_PRIMARY : TEXT_MUTED, false);
    }

    private static void drawTextField(DrawContext ctx, TextRenderer tr,
                                      int x, int y, int w, String value, boolean focused) {
        ctx.fill(x, y, x + w, y + 12, BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 11, focused ? 0xFF333333 : PANEL_SOFT);
        boolean showCursor = focused && (Util.getMeasuringTimeMs() % 1000) < 500;
        String display = value + (showCursor ? "|" : "");
        int maxW = w - 6;
        while (!display.isEmpty() && tr.getWidth(display) > maxW) display = display.substring(1);
        ctx.drawText(tr, Text.literal(display), x + 3, y + 2, TEXT_PRIMARY, false);
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Called when the screen closes or the card collapses. */
    public static void reset() {
        focusedCustomIdx = -1;
    }
}
