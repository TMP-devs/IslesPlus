package com.islesplus.features.inventorysearch;

import com.islesplus.sync.FeatureFlags;
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

/**
 * Inventory search bar - click to focus, then type to filter items.
 * Text state lives in {@link SearchTextState}, calculator in {@link InventoryCalculator}.
 *
 * Search syntax (comma-separated AND terms):
 *   text  - matches item display name
 *   #text - matches item lore and NBT data
 */
public final class InventorySearch {
    public enum SearchBarPosition {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public static boolean inventorySearchEnabled = false;
    public static SearchBarPosition barPosition = SearchBarPosition.TOP_LEFT;

    private static final int MARGIN = 4;
    static final int BAR_W = 140;
    static final int BAR_H = 16;
    static final int CHEVRON_W = 14;

    private static int barX(int screenW) {
        return switch (barPosition) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_CENTER, BOTTOM_CENTER -> screenW / 2 - BAR_W / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - BAR_W - MARGIN;
        };
    }

    private static int chevronX(int bx) {
        return switch (barPosition) {
            case TOP_LEFT, BOTTOM_LEFT -> bx + BAR_W;
            default -> bx - CHEVRON_W;
        };
    }

    private static int barY(int screenH) {
        return switch (barPosition) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenH - BAR_H - MARGIN;
        };
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
        double mx = click.x();
        double my = click.y();
        int bx = barX(screen.width);
        int by = barY(screen.height);

        // Chevron toggle
        int cx = chevronX(bx);
        if (mx >= cx && mx <= cx + CHEVRON_W && my >= by && my <= by + BAR_H) {
            InventoryCalculator.toggle();
            SearchTextState.focused = false;
            return false;
        }

        // Calculator panel clicks
        if (InventoryCalculator.open) {
            if (InventoryCalculator.handleClick(screen, mx, my, bx, by)) {
                SearchTextState.focused = false;
                InventoryCalculator.focused = true;
                return false;
            }
        }

        boolean hitBar = mx >= bx && mx <= bx + BAR_W && my >= by && my <= by + BAR_H;
        SearchTextState.focused = hitBar;
        if (hitBar) {
            InventoryCalculator.focused = false;
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
        MinecraftClient client = MinecraftClient.getInstance();
        boolean focused = SearchTextState.focused;

        int bx = barX(screen.width);
        int by = barY(screen.height);

        // Chevron
        int cx = chevronX(bx);
        ctx.fill(cx - 1, by - 1, cx + CHEVRON_W, by + BAR_H + 1, 0xFF555555);
        ctx.fill(cx, by, cx + CHEVRON_W, by + BAR_H, 0xFF1E1E1E);
        drawChevron(ctx, cx + CHEVRON_W / 2, by + BAR_H / 2, InventoryCalculator.open, 0xFFAAAAAA);

        // Border + background
        int borderColor = focused ? 0xFF3A8DDE : 0xFF555555;
        ctx.fill(bx - 1, by - 1, bx + BAR_W + 1, by,                 borderColor);
        ctx.fill(bx - 1, by + BAR_H, bx + BAR_W + 1, by + BAR_H + 1, borderColor);
        ctx.fill(bx - 1, by, bx, by + BAR_H,                          borderColor);
        ctx.fill(bx + BAR_W, by, bx + BAR_W + 1, by + BAR_H,          borderColor);
        ctx.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF1E1E1E);

        // Text / placeholder
        int textX = bx + 4, textY = by + 4, maxW = BAR_W - 8;
        if (SearchTextState.searchText.isEmpty() && !focused) {
            ctx.drawText(client.textRenderer, "Search...", textX, textY, 0xFF888888, false);
        } else {
            renderSearchText(ctx, client, focused, textX, textY, maxW, bx, by);
        }

        if (InventoryCalculator.open) {
            InventoryCalculator.render(ctx, client, bx, by, screen.width, screen.height);
        }
    }

    private static void renderSearchText(DrawContext ctx, MinecraftClient client, boolean focused,
                                          int textX, int textY, int maxW, int bx, int by) {
        int cursorPixel = client.textRenderer.getWidth(SearchTextState.searchText.substring(0, SearchTextState.cursorPos));
        int scrollOffset = cursorPixel > maxW ? cursorPixel - maxW : 0;

        if (focused && SearchTextState.hasSelection()) {
            int selStartPx = client.textRenderer.getWidth(SearchTextState.searchText.substring(0, SearchTextState.selMin())) - scrollOffset;
            int selEndPx = client.textRenderer.getWidth(SearchTextState.searchText.substring(0, SearchTextState.selMax())) - scrollOffset;
            int hlX1 = Math.max(0, selStartPx) + textX;
            int hlX2 = Math.min(maxW, selEndPx) + textX;
            if (hlX2 > hlX1) ctx.fill(hlX1, textY - 1, hlX2, textY + client.textRenderer.fontHeight, 0xFF3A6EA5);
        }

        ctx.enableScissor(textX, by, bx + BAR_W - 4, by + BAR_H);
        ctx.drawText(client.textRenderer, SearchTextState.searchText, textX - scrollOffset, textY, 0xFFFFFFFF, false);
        ctx.disableScissor();

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + cursorPixel - scrollOffset;
            ctx.fill(cursorX, textY - 1, cursorX + 1, textY + client.textRenderer.fontHeight, 0xFFFFFFFF);
        }
    }

    private static void drawChevron(DrawContext ctx, int cx, int cy, boolean down, int color) {
        if (down) {
            ctx.fill(cx - 3, cy - 1, cx + 4, cy, color);
            ctx.fill(cx - 2, cy, cx + 3, cy + 1, color);
            ctx.fill(cx - 1, cy + 1, cx + 2, cy + 2, color);
            ctx.fill(cx, cy + 2, cx + 1, cy + 3, color);
        } else {
            ctx.fill(cx - 1, cy - 3, cx, cy + 4, color);
            ctx.fill(cx, cy - 2, cx + 1, cy + 3, color);
            ctx.fill(cx + 1, cy - 1, cx + 2, cy + 2, color);
            ctx.fill(cx + 2, cy, cx + 3, cy + 1, color);
        }
    }
}
