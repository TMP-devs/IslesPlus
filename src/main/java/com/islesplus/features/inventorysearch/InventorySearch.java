package com.islesplus.features.inventorysearch;

import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudLayout;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

/**
 * search bar on top of inventories. click it, type, matching items light up.
 * text stuff is in {@link SearchTextState}, calculator in {@link InventoryCalculator}
 *
 * syntax (comma separated, all terms have to match):
 *   text  - item name
 *   #text - lore / nbt
 */
public final class InventorySearch {
    public static boolean inventorySearchEnabled = true;

    static final int BAR_W = 140;
    static final int BAR_H = 16;
    static final int CHEVRON_W = 14;

    /** The bar and its calculator chevron, placed and scaled with the HUD editor. It lives in
     * inventory screens, so the HUD pass never draws it; {@link #onRender} does. */
    public static final HudElement ELEMENT = new HudElement("inventory_search", "Inventory Search",
        new HudPlacement(HudAnchor.START, HudAnchor.START, 4, 4)) {
        @Override public boolean enabled() { return inventorySearchEnabled; }
        @Override public boolean active(MinecraftClient client) { return false; }
        @Override public boolean inHud() { return false; }
        @Override public Size measure(boolean preview) { return new Size(BAR_W + CHEVRON_W, BAR_H); }
        @Override public String editorHint() { return "Shown in inventories"; }
        @Override public void draw(DrawContext ctx, Frame f) {
            boolean right = chevronRight(f.x(), HudAnchor.scaled(BAR_W + CHEVRON_W, f.scale()), f.screenW());
            drawBar(ctx, right, !f.preview() && SearchTextState.focused, f.preview());
        }
    };

    /** The chevron goes on the side of the bar that faces the middle of the screen. */
    private static boolean chevronRight(int boxX, int boxW, int screenW) {
        return boxX + boxW / 2 < screenW / 2;
    }

    private static int barX(boolean chevronRight) { return chevronRight ? 0 : CHEVRON_W; }

    private static int chevronX(boolean chevronRight) { return chevronRight ? BAR_W : 0; }

    /** The calculator opens over the bar when it would run off the bottom of the screen. */
    private static boolean calculatorAbove(HudLayout.Box box, int screenH) {
        return box.y() + (BAR_H + 2 + InventoryCalculator.getPanelHeight()) * box.scale() > screenH;
    }

    private InventorySearch() {}

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?>)) return;
            SearchTextState.focused = false;
            SearchTextState.cursorPos = SearchTextState.searchText.length();
            SearchTextState.selectionStart = -1;
            InventoryCalculator.open = false;
            InventoryCalculator.clear();

            ScreenMouseEvents.allowMouseClick(screen).register(InventorySearch::onMouseClick);
            ScreenKeyboardEvents.allowKeyPress(screen).register(InventorySearch::onKeyPress);
            ScreenEvents.afterRender(screen).register(InventorySearch::onRender);
        });
    }

    private static boolean onMouseClick(Screen screen, Click click) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search") || WorldIdentification.world == PlayerWorld.OTHER) return true;
        HudLayout.Box box = HudLayout.place(ELEMENT, screen.width, screen.height, false);
        boolean right = chevronRight(box.x(), box.w(), screen.width);
        // Into the bar's own (unscaled) pixels
        double mx = (click.x() - box.x()) / box.scale();
        double my = (click.y() - box.y()) / box.scale();
        int bx = barX(right);
        int by = 0;

        // Chevron toggle
        int cx = chevronX(right);
        if (mx >= cx && mx <= cx + CHEVRON_W && my >= by && my <= by + BAR_H) {
            InventoryCalculator.toggle();
            SearchTextState.focused = false;
            return false;
        }

        // Calculator panel clicks
        if (InventoryCalculator.open) {
            if (InventoryCalculator.handleClick(mx, my, bx, by, calculatorAbove(box, screen.height))) {
                SearchTextState.focused = false;
                InventoryCalculator.focused = true;
                return false;
            }
        }

        boolean hitBar = mx >= bx && mx <= bx + BAR_W && my >= by && my <= by + BAR_H;
        SearchTextState.focused = hitBar;
        if (hitBar) {
            InventoryCalculator.focused = false;
            // right-click in the box clears what was typed (and leaves it focused to type again)
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) SearchTextState.setSearchText("");
            SearchTextState.cursorPos = SearchTextState.searchText.length();
            SearchTextState.selectionStart = -1;
        } else {
            InventoryCalculator.focused = false;
        }
        return !hitBar;
    }

    private static boolean onKeyPress(Screen screen, KeyInput context) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search") || WorldIdentification.world == PlayerWorld.OTHER) return true;

        if (InventoryCalculator.open && InventoryCalculator.focused) {
            return InventoryCalculator.handleKeyPress(context);
        }

        if (!SearchTextState.focused) return true;
        return SearchTextState.handleKeyPress(context);
    }

    /** Called from HandledScreenMixin.drawSlot (at TAIL). */
    public static void drawSlotOverlay(DrawContext context, Slot slot) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search")
                || SearchTextState.searchText.isEmpty() || WorldIdentification.world == PlayerWorld.OTHER) return;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;
        if (!SearchTextState.matchesSearch(stack)) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xAA000000);
        }
    }

    private static void onRender(Screen screen, DrawContext ctx, int mx, int my, float delta) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search")) return;
        if (WorldIdentification.world == PlayerWorld.OTHER) return;
        HudLayout.Box box = HudLayout.place(ELEMENT, screen.width, screen.height, false);
        boolean right = chevronRight(box.x(), box.w(), screen.width);
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) box.x(), (float) box.y());
            ctx.getMatrices().scale(box.scale(), box.scale());
            drawBar(ctx, right, SearchTextState.focused, false);
            if (InventoryCalculator.open) {
                InventoryCalculator.render(ctx, barX(right), 0, calculatorAbove(box, screen.height));
            }
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    /** Chevron + bar at (0, 0). A preview (the HUD editor) shows the empty bar. */
    private static void drawBar(DrawContext ctx, boolean chevronRight, boolean focused, boolean preview) {
        MinecraftClient client = MinecraftClient.getInstance();
        int bx = barX(chevronRight), by = 0;

        // Chevron
        int cx = chevronX(chevronRight);
        Draw.bevel(ctx, cx, by, CHEVRON_W, BAR_H, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
        Draw.caret(ctx, cx + CHEVRON_W / 2, by + BAR_H / 2,
                !preview && InventoryCalculator.open ? Draw.Dir.DOWN : Draw.Dir.RIGHT, Theme.CREAM);

        // Bar
        Draw.bevel(ctx, bx, by, BAR_W, BAR_H, Theme.CALC_NUM, Theme.CALC_NUM_LIT, Theme.CALC_NUM_SHADE,
                focused ? Theme.OXBLOOD : Theme.INK);

        // Text / placeholder
        int textX = bx + 4, textY = by + 4, maxW = BAR_W - 8;
        if (preview || (SearchTextState.searchText.isEmpty() && !focused)) {
            Fonts.draw(ctx, "Search...", textX, textY, Theme.TEXT_META);
        } else {
            renderSearchText(ctx, client, focused, textX, textY, maxW, bx, by);
        }
    }

    private static void renderSearchText(DrawContext ctx, MinecraftClient client, boolean focused,
                                          int textX, int textY, int maxW, int bx, int by) {
        String text = SearchTextState.searchText;
        int cursorPixel = Fonts.width(text.substring(0, SearchTextState.cursorPos));
        int scrollOffset = cursorPixel > maxW ? cursorPixel - maxW : 0;

        ctx.enableScissor(textX, by, bx + BAR_W - 4, by + BAR_H);
        if (focused && SearchTextState.hasSelection()) {
            int selMin = SearchTextState.selMin();
            int selMax = SearchTextState.selMax();
            String pre = text.substring(0, selMin);
            String sel = text.substring(selMin, selMax);
            String post = text.substring(selMax);

            int selStartPx = Fonts.width(pre) - scrollOffset;
            int selEndPx = Fonts.width(pre + sel) - scrollOffset;
            int hlX1 = Math.max(0, selStartPx) + textX;
            int hlX2 = Math.min(maxW, selEndPx) + textX;
            if (hlX2 > hlX1) ctx.fill(hlX1, textY - 1, hlX2, textY + client.textRenderer.fontHeight, Theme.OXBLOOD);

            int preX = textX - scrollOffset;
            Fonts.draw(ctx, pre, preX, textY, Theme.INK_DEEP);
            int selX = preX + Fonts.width(pre);
            Fonts.draw(ctx, sel, selX, textY, Theme.CREAM);
            int postX = selX + Fonts.width(sel);
            Fonts.draw(ctx, post, postX, textY, Theme.INK_DEEP);
        } else {
            Fonts.draw(ctx, text, textX - scrollOffset, textY, Theme.INK_DEEP);
        }
        ctx.disableScissor();

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + cursorPixel - scrollOffset;
            ctx.fill(cursorX, textY - 1, cursorX + 1, textY + client.textRenderer.fontHeight, Theme.INK_DEEP);
        }
    }
}
