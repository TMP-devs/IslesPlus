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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Inventory search bar - click to focus, then type to filter items.
 *
 * Search syntax (comma-separated AND terms):
 *   text  - matches item display name
 *   #text - matches item lore and NBT data
 *
 * Example: "iron,#sharp" shows items named "iron" that have "sharp" in lore/NBT.
 */
public final class InventorySearch {
    public enum SearchBarPosition {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public static boolean inventorySearchEnabled = false;
    public static SearchBarPosition barPosition = SearchBarPosition.TOP_LEFT;
    private static String searchText = "";
    private static boolean focused = false;

    /** Parsed from searchText whenever it changes. Each entry is trimmed and lowercased. */
    private static String[] parsedTerms = new String[0];

    private static final int MARGIN = 4;
    static final int BAR_W = 140;
    static final int BAR_H = 16;

    private static int barX(int screenW) {
        return switch (barPosition) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_CENTER, BOTTOM_CENTER -> screenW / 2 - BAR_W / 2;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - BAR_W - MARGIN;
        };
    }

    private static int barY(int screenH) {
        return switch (barPosition) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenH - BAR_H - MARGIN;
        };
    }

    private InventorySearch() {}

    private static void setSearchText(String text) {
        searchText = text;
        if (text.isEmpty()) {
            parsedTerms = new String[0];
        } else {
            String[] raw = text.split(",");
            for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim().toLowerCase();
            parsedTerms = raw;
        }
    }

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?>)) return;
            focused = false;

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
        boolean hitBar = mx >= bx && mx <= bx + BAR_W && my >= by && my <= by + BAR_H;
        focused = hitBar;
        return !hitBar;
    }

    private static boolean onKeyPress(Screen screen, KeyInput context) {
        if (!inventorySearchEnabled || !focused || FeatureFlags.isKilled("inventory_search") || WorldIdentification.world == PlayerWorld.OTHER) return true;

        int key = context.key();
        boolean ctrl = (context.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (ctrl) {
                setSearchText("");
            } else if (!searchText.isEmpty()) {
                setSearchText(searchText.substring(0, searchText.length() - 1));
            }
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!searchText.isEmpty()) {
                setSearchText("");
            } else {
                focused = false;
            }
            return false;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            focused = false;
            return false;
        }

        if (!ctrl && searchText.length() < 50) {
            boolean shift = (context.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            char c = keyToChar(key, shift);
            if (c != 0) {
                setSearchText(searchText + c);
                return false;
            }
        }

        return true;
    }

    private static char keyToChar(int key, boolean shift) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return (char) (shift ? key : key + 32);
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            if (!shift) return (char) key;
            return ")!@#$%^&*(".charAt(key - GLFW.GLFW_KEY_0);
        }
        if (shift) {
            return switch (key) {
                case GLFW.GLFW_KEY_MINUS        -> '_';
                case GLFW.GLFW_KEY_EQUAL        -> '+';
                case GLFW.GLFW_KEY_LEFT_BRACKET -> '{';
                case GLFW.GLFW_KEY_RIGHT_BRACKET-> '}';
                case GLFW.GLFW_KEY_BACKSLASH    -> '|';
                case GLFW.GLFW_KEY_SEMICOLON    -> ':';
                case GLFW.GLFW_KEY_APOSTROPHE   -> '"';
                case GLFW.GLFW_KEY_COMMA        -> '<';
                case GLFW.GLFW_KEY_PERIOD       -> '>';
                case GLFW.GLFW_KEY_SLASH        -> '?';
                case GLFW.GLFW_KEY_GRAVE_ACCENT -> '~';
                default -> 0;
            };
        } else {
            return switch (key) {
                case GLFW.GLFW_KEY_SPACE        -> ' ';
                case GLFW.GLFW_KEY_MINUS        -> '-';
                case GLFW.GLFW_KEY_EQUAL        -> '=';
                case GLFW.GLFW_KEY_LEFT_BRACKET -> '[';
                case GLFW.GLFW_KEY_RIGHT_BRACKET-> ']';
                case GLFW.GLFW_KEY_BACKSLASH    -> '\\';
                case GLFW.GLFW_KEY_SEMICOLON    -> ';';
                case GLFW.GLFW_KEY_APOSTROPHE   -> '\'';
                case GLFW.GLFW_KEY_COMMA        -> ',';
                case GLFW.GLFW_KEY_PERIOD       -> '.';
                case GLFW.GLFW_KEY_SLASH        -> '/';
                case GLFW.GLFW_KEY_GRAVE_ACCENT -> '`';
                default -> 0;
            };
        }
    }

    /**
     * Called from HandledScreenMixin.drawSlot (at TAIL).
     * Coordinates are GUI-relative; the draw context matrix is already translated to the GUI origin.
     */
    public static void drawSlotOverlay(DrawContext context, Slot slot) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search") || searchText.isEmpty() || WorldIdentification.world == PlayerWorld.OTHER) return;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;
        if (!matchesSearch(stack)) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xAA000000);
        }
    }

    private static void onRender(Screen screen, DrawContext ctx, int mx, int my, float delta) {
        if (!inventorySearchEnabled || FeatureFlags.isKilled("inventory_search")) return;
        if (WorldIdentification.world == PlayerWorld.OTHER) return;
        MinecraftClient client = MinecraftClient.getInstance();

        int bx = barX(screen.width);
        int by = barY(screen.height);

        // Border
        int borderColor = focused ? 0xFF3A8DDE : 0xFF555555;
        ctx.fill(bx - 1, by - 1, bx + BAR_W + 1, by,                    borderColor);
        ctx.fill(bx - 1, by + BAR_H, bx + BAR_W + 1, by + BAR_H + 1,    borderColor);
        ctx.fill(bx - 1, by,          bx,             by + BAR_H,         borderColor);
        ctx.fill(bx + BAR_W, by,      bx + BAR_W + 1, by + BAR_H,        borderColor);
        // Background
        ctx.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF1E1E1E);

        // Text / placeholder
        if (searchText.isEmpty() && !focused) {
            ctx.drawText(client.textRenderer, "Search...", bx + 4, by + 4, 0xFF888888, false);
        } else {
            boolean cursorOn = focused && (System.currentTimeMillis() % 1000) < 500;
            String full = searchText + (cursorOn ? "|" : "");
            int maxW = BAR_W - 6;
            String display = full;
            while (client.textRenderer.getWidth(display) > maxW && display.length() > 1) {
                display = display.substring(1);
            }
            ctx.drawText(client.textRenderer, display, bx + 4, by + 4, 0xFFFFFFFF, false);
        }

        // Hint - show above or below depending on position
        if (focused) {
            boolean atBottom = barPosition == SearchBarPosition.BOTTOM_LEFT
                    || barPosition == SearchBarPosition.BOTTOM_CENTER
                    || barPosition == SearchBarPosition.BOTTOM_RIGHT;
            int hintY = atBottom ? by - client.textRenderer.fontHeight - 3 : by + BAR_H + 3;
            ctx.drawText(client.textRenderer, "commas = AND  |  # = lore", bx, hintY, 0xFF888888, false);
        }
    }

    // -------------------------------------------------------------------------
    // Matching
    // -------------------------------------------------------------------------

    /**
     * Terms without '#' match against the item display name.
     * Terms with '#' match against lore lines and component/NBT data.
     * All terms must match (AND logic).
     */
    private static boolean matchesSearch(ItemStack stack) {
        String loreAndNbt = null;
        for (String term : parsedTerms) {
            if (term.isEmpty()) continue;
            if (term.startsWith("#")) {
                String query = term.substring(1);
                if (query.isEmpty()) continue;
                if (loreAndNbt == null) loreAndNbt = getItemLoreAndNbtText(stack);
                if (!loreAndNbt.contains(query)) return false;
            } else {
                if (!stack.getName().getString().toLowerCase().contains(term)) return false;
            }
        }
        return true;
    }

    private static String getItemLoreAndNbtText(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                sb.append(line.getString()).append(' ');
            }
        }
        try {
            sb.append(stack.getComponents().toString());
        } catch (Exception ignored) {}
        return sb.toString().toLowerCase();
    }

}
