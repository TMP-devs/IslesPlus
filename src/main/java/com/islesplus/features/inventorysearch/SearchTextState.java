package com.islesplus.features.inventorysearch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Search bar text state, cursor management, keyboard handling, and item matching.
 */
final class SearchTextState {

    static String searchText = "";
    static boolean focused = false;
    static int cursorPos = 0;
    static int selectionStart = -1; // -1 = no selection

    /** Parsed from searchText whenever it changes. */
    static String[] parsedTerms = new String[0];

    private SearchTextState() {}

    static void setSearchText(String text) {
        searchText = text;
        cursorPos = Math.min(cursorPos, text.length());
        if (selectionStart != -1) selectionStart = Math.min(selectionStart, text.length());
        if (text.isEmpty()) {
            parsedTerms = new String[0];
            cursorPos = 0;
            selectionStart = -1;
        } else {
            String[] raw = text.split(",");
            for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim().toLowerCase();
            parsedTerms = raw;
        }
    }

    static boolean isCommandDown(int modifiers) {
        return net.minecraft.util.Util.getOperatingSystem() == net.minecraft.util.Util.OperatingSystem.OSX
                ? (modifiers & GLFW.GLFW_MOD_SUPER) != 0
                : (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    static int selMin() { return Math.min(selectionStart, cursorPos); }
    static int selMax() { return Math.max(selectionStart, cursorPos); }

    static void deleteSelection() {
        int lo = selMin(), hi = selMax();
        setSearchText(searchText.substring(0, lo) + searchText.substring(hi));
        cursorPos = lo;
        selectionStart = -1;
    }

    static boolean hasSelection() {
        return selectionStart != -1 && selectionStart != cursorPos;
    }

    static String getSelectedText() {
        if (!hasSelection()) return "";
        return searchText.substring(selMin(), selMax());
    }

    static void insertAtCursor(String text) {
        if (hasSelection()) deleteSelection();
        String filtered = text.length() + searchText.length() > 50
                ? text.substring(0, Math.max(0, 50 - searchText.length()))
                : text;
        setSearchText(searchText.substring(0, cursorPos) + filtered + searchText.substring(cursorPos));
        cursorPos += filtered.length();
        selectionStart = -1;
    }

    // ---- Keyboard handling ----

    static boolean handleKeyPress(KeyInput context) {
        int key = context.key();
        boolean cmd = isCommandDown(context.modifiers());
        boolean shift = (context.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (cmd) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> { selectionStart = 0; cursorPos = searchText.length(); return false; }
                case GLFW.GLFW_KEY_C -> {
                    if (hasSelection()) copyToClipboard();
                    return false;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSelection()) { copyToClipboard(); deleteSelection(); }
                    return false;
                }
                case GLFW.GLFW_KEY_V -> {
                    String clip = clipboardGet();
                    if (clip != null) insertAtCursor(clip.replace("\n", " ").replace("\r", ""));
                    return false;
                }
            }
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) { deleteSelection(); }
            else if (cmd) { setSearchText(searchText.substring(cursorPos)); cursorPos = 0; selectionStart = -1; }
            else if (cursorPos > 0) { cursorPos--; setSearchText(searchText.substring(0, cursorPos) + searchText.substring(cursorPos + 1)); }
            return false;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) deleteSelection();
            else if (cursorPos < searchText.length()) setSearchText(searchText.substring(0, cursorPos) + searchText.substring(cursorPos + 1));
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (hasSelection()) selectionStart = -1;
            else if (!searchText.isEmpty()) { setSearchText(""); cursorPos = 0; selectionStart = -1; }
            else focused = false;
            return false;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { focused = false; return false; }

        if (key == GLFW.GLFW_KEY_LEFT) { handleArrow(cmd, shift, true); return false; }
        if (key == GLFW.GLFW_KEY_RIGHT) { handleArrow(cmd, shift, false); return false; }

        if (key == GLFW.GLFW_KEY_HOME) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            cursorPos = 0;
            if (!shift) selectionStart = -1;
            return false;
        }
        if (key == GLFW.GLFW_KEY_END) {
            if (shift && selectionStart == -1) selectionStart = cursorPos;
            cursorPos = searchText.length();
            if (!shift) selectionStart = -1;
            return false;
        }

        if (!cmd) {
            char c = keyToChar(key, shift);
            if (c != 0) { insertAtCursor(String.valueOf(c)); return false; }
        }
        return true;
    }

    private static void handleArrow(boolean cmd, boolean shift, boolean left) {
        if (shift && selectionStart == -1) selectionStart = cursorPos;
        if (left) {
            if (cmd) cursorPos = 0;
            else if (!shift && hasSelection()) { cursorPos = selMin(); selectionStart = -1; }
            else if (cursorPos > 0) cursorPos--;
        } else {
            if (cmd) cursorPos = searchText.length();
            else if (!shift && hasSelection()) { cursorPos = selMax(); selectionStart = -1; }
            else if (cursorPos < searchText.length()) cursorPos++;
        }
        if (!shift) selectionStart = -1;
    }

    private static void copyToClipboard() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        GLFW.glfwSetClipboardString(window, getSelectedText());
    }

    private static String clipboardGet() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetClipboardString(window);
    }

    // ---- GLFW key-to-character mapping ----

    static char keyToChar(int key, boolean shift) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return (char) (shift ? key : key + 32);
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            if (!shift) return (char) key;
            return ")!@#$%^&*(".charAt(key - GLFW.GLFW_KEY_0);
        }
        if (shift) {
            return switch (key) {
                case GLFW.GLFW_KEY_MINUS -> '_'; case GLFW.GLFW_KEY_EQUAL -> '+';
                case GLFW.GLFW_KEY_LEFT_BRACKET -> '{'; case GLFW.GLFW_KEY_RIGHT_BRACKET -> '}';
                case GLFW.GLFW_KEY_BACKSLASH -> '|'; case GLFW.GLFW_KEY_SEMICOLON -> ':';
                case GLFW.GLFW_KEY_APOSTROPHE -> '"'; case GLFW.GLFW_KEY_COMMA -> '<';
                case GLFW.GLFW_KEY_PERIOD -> '>'; case GLFW.GLFW_KEY_SLASH -> '?';
                case GLFW.GLFW_KEY_GRAVE_ACCENT -> '~'; default -> 0;
            };
        }
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> ' '; case GLFW.GLFW_KEY_MINUS -> '-';
            case GLFW.GLFW_KEY_EQUAL -> '='; case GLFW.GLFW_KEY_LEFT_BRACKET -> '[';
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> ']'; case GLFW.GLFW_KEY_BACKSLASH -> '\\';
            case GLFW.GLFW_KEY_SEMICOLON -> ';'; case GLFW.GLFW_KEY_APOSTROPHE -> '\'';
            case GLFW.GLFW_KEY_COMMA -> ','; case GLFW.GLFW_KEY_PERIOD -> '.';
            case GLFW.GLFW_KEY_SLASH -> '/'; case GLFW.GLFW_KEY_GRAVE_ACCENT -> '`';
            default -> 0;
        };
    }

    // ---- Item matching ----

    static boolean matchesSearch(ItemStack stack) {
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
            for (Text line : lore.lines()) sb.append(line.getString()).append(' ');
        }
        try { sb.append(stack.getComponents().toString()); } catch (Exception ignored) {}
        return sb.toString().toLowerCase();
    }
}
