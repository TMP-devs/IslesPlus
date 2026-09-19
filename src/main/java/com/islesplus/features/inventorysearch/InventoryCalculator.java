package com.islesplus.features.inventorysearch;

import com.islesplus.ui.Draw;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;

/**
 * the calculator dropdown you get from the little chevron next to the search bar.
 * this is just the UI, math is in {@link CalcExpression}
 */
public final class InventoryCalculator {

    // ---- State ----
    static boolean open = false;
    static boolean focused = false;

    // ---- State ----
    static boolean radiansMode = false;

    // ---- Button layout (4 columns to fit within search bar width) ----
    private static final String[][] BUTTONS = {
        {"sin(", "cos(", "tan(", "^"},
        {"sqrt(", "log(", "(", ")"},
        {"7", "8", "9", "/"},
        {"4", "5", "6", "*"},
        {"1", "2", "3", "-"},
        {"+/-", "0", ".", "+"},
        {"C", "CE", "ANS", "%"}
    };

    // ---- Button sizing ----
    private static final int BTN_W = 31;
    private static final int BTN_H = 16;
    private static final int BTN_PAD = 2;
    private static final int DISPLAY_H = 18;
    private static final int PANEL_PAD = 4;

    private InventoryCalculator() {}

    // ---- Dimension helpers ----

    static int panelX(int barX, int screenW) {
        int pw = getPanelWidth();
        int px = barX;
        if (px + pw > screenW - 2) px = screenW - pw - 2;
        if (px < 2) px = 2;
        return px;
    }

    static int panelY(int barY, int screenH) {
        int ph = getPanelHeight();
        boolean atBottom = barY + InventorySearch.BAR_H + ph > screenH;
        return atBottom ? barY - ph - 2 : barY + InventorySearch.BAR_H + 2;
    }

    static int getPanelWidth() {
        return InventorySearch.BAR_W;
    }

    static int getPanelHeight() {
        return PANEL_PAD * 2 + DISPLAY_H + BTN_PAD + BUTTONS.length * (BTN_H + BTN_PAD) + BTN_H + BTN_PAD;
    }

    // ---- Public API ----

    static void toggle() {
        open = !open;
        focused = open;
    }

    static void clear() {
        CalcExpression.clear();
        focused = false;
    }

    // ---- Rendering ----

    static void render(DrawContext ctx, int barX, int barY, int screenW, int screenH) {
        if (!open) return;

        int px = panelX(barX, screenW);
        int py = panelY(barY, screenH);
        int pw = getPanelWidth();
        int ph = getPanelHeight();

        // Panel frame
        Draw.bevel4(ctx, px, py, pw, ph, Theme.CALC_FRAME, Theme.CALC_FRAME_LIT, Theme.CALC_FRAME_SHADE,
                Theme.CALC_FRAME_LIT, Theme.CALC_FRAME_SHADE, Theme.INK);

        int innerX = px + PANEL_PAD;
        int curY = py + PANEL_PAD;

        // Expression display
        int displayW = pw - PANEL_PAD * 2;
        Draw.well(ctx, innerX, curY, displayW, DISPLAY_H, Theme.INK_DEEP);

        // DEG/RAD indicator — small chip, top-left of display, clickable.
        // Click rect in handleClick() is recomputed from this same Fonts.width(...) call.
        String modeLabel = radiansMode ? "RAD" : "DEG";
        int chipW = Fonts.width(modeLabel, Fonts.SMALL) + 4;
        int chipX = innerX + 2, chipY = curY + 1, chipH = 9;
        Draw.bevel(ctx, chipX, chipY, chipW, chipH, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK_DEEP);
        Fonts.draw(ctx, modeLabel, chipX + 2, chipY + 1, Theme.CREAM, Fonts.SMALL);

        String dText = CalcExpression.displayText.isEmpty() ? "0" : CalcExpression.displayText;
        String ghost = CalcExpression.getGhostParens();
        int textW = Fonts.width(dText);
        int ghostW = Fonts.width(ghost);
        int totalW = textW + ghostW;
        int displayTextX = innerX + displayW - 4 - totalW;
        int displayTextY = curY + (DISPLAY_H - Fonts.GLYPH_H) / 2;
        ctx.enableScissor(innerX + 2, curY, innerX + displayW - 2, curY + DISPLAY_H);
        Fonts.draw(ctx, dText, displayTextX, displayTextY, Theme.CREAM);
        if (!ghost.isEmpty()) {
            Fonts.draw(ctx, ghost, displayTextX + textW, displayTextY, Theme.CALC_EXPR);
        }
        ctx.disableScissor();
        curY += DISPLAY_H + BTN_PAD;

        // All button rows
        for (String[] row : BUTTONS) {
            drawButtonRow(ctx, row, innerX, curY);
            curY += BTN_H + BTN_PAD;
        }

        // Wide = button
        Draw.bevel(ctx, innerX, curY, displayW, BTN_H, Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK);
        int eqW = Fonts.width("=");
        Fonts.draw(ctx, "=", innerX + (displayW - eqW) / 2, curY + (BTN_H - Fonts.GLYPH_H) / 2, Theme.CREAM);
    }

    /** A DIGIT key is a single digit 0-9 or the decimal point; everything else in the grid is a FRAME key. */
    private static boolean isDigitKey(String label) {
        return (label.length() == 1 && Character.isDigit(label.charAt(0))) || label.equals(".");
    }

    private static void drawButtonRow(DrawContext ctx, String[] labels, int startX, int y) {
        int x = startX;
        for (String label : labels) {
            boolean digit = isDigitKey(label);
            int fill = digit ? Theme.CALC_NUM : Theme.CALC_FRAME;
            int lit = digit ? Theme.CALC_NUM_LIT : Theme.CALC_FRAME_LIT;
            int shade = digit ? Theme.CALC_NUM_SHADE : Theme.CALC_FRAME_SHADE;
            Draw.bevel(ctx, x, y, BTN_W, BTN_H, fill, lit, shade, Theme.INK);
            int tw = Fonts.width(label);
            int tx = x + (BTN_W - tw) / 2;
            int ty = y + (BTN_H - Fonts.GLYPH_H) / 2;
            Fonts.draw(ctx, label, tx, ty, Theme.TEXT_LABEL);
            x += BTN_W + BTN_PAD;
        }
    }

    // ---- Click handling ----

    static boolean handleClick(Screen screen, double mx, double my, int barX, int barY) {
        if (!open) return false;

        int px = panelX(barX, screen.width);
        int py = panelY(barY, screen.height);
        int pw = getPanelWidth();
        int ph = getPanelHeight();

        if (mx < px || mx > px + pw || my < py || my > py + ph) return false;

        int innerX = px + PANEL_PAD;

        // DEG/RAD indicator click (top-left of display) — same chip bounds as drawn in render()
        String modeLabel = radiansMode ? "RAD" : "DEG";
        int chipW = Fonts.width(modeLabel, Fonts.SMALL) + 4;
        int chipH = 9;
        if (mx >= innerX + 2 && mx <= innerX + 2 + chipW && my >= py + PANEL_PAD + 1 && my <= py + PANEL_PAD + 1 + chipH) {
            radiansMode = !radiansMode;
            return true;
        }

        int curY = py + PANEL_PAD + DISPLAY_H + BTN_PAD;

        for (String[] row : BUTTONS) {
            String hit = hitTestRow(row, innerX, curY, mx, my);
            if (hit != null) { onButtonClick(hit); return true; }
            curY += BTN_H + BTN_PAD;
        }

        // Wide = button
        int displayW = pw - PANEL_PAD * 2;
        if (mx >= innerX && mx <= innerX + displayW && my >= curY && my <= curY + BTN_H) {
            CalcExpression.evaluate();
            return true;
        }

        return true;
    }

    private static String hitTestRow(String[] labels, int startX, int y, double mx, double my) {
        if (my < y || my > y + BTN_H) return null;
        int x = startX;
        for (String label : labels) {
            if (mx >= x && mx <= x + BTN_W) return label;
            x += BTN_W + BTN_PAD;
        }
        return null;
    }

    private static void onButtonClick(String label) {
        switch (label) {
            case "=" -> CalcExpression.evaluate();
            case "C" -> CalcExpression.clear();
            case "CE" -> CalcExpression.clearEntry();
            case "+/-" -> CalcExpression.toggleSign();
            case "ANS" -> CalcExpression.useAns();
            default -> CalcExpression.appendToExpression(label);
        }
    }

    // ---- Keyboard input ----

    static boolean handleKeyPress(KeyInput context) {
        if (!open) return true;

        int key = context.key();
        boolean shift = (context.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            open = false;
            focused = false;
            return false;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            CalcExpression.evaluate();
            return false;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            CalcExpression.backspace();
            return false;
        }

        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9 && !shift) {
            CalcExpression.appendToExpression(String.valueOf((char) key));
            return false;
        }

        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            CalcExpression.appendToExpression(String.valueOf(key - GLFW.GLFW_KEY_KP_0));
            return false;
        }

        if (key == GLFW.GLFW_KEY_KP_ADD) { CalcExpression.appendToExpression("+"); return false; }
        if (key == GLFW.GLFW_KEY_KP_SUBTRACT) { CalcExpression.appendToExpression("-"); return false; }
        if (key == GLFW.GLFW_KEY_KP_MULTIPLY) { CalcExpression.appendToExpression("*"); return false; }
        if (key == GLFW.GLFW_KEY_KP_DIVIDE) { CalcExpression.appendToExpression("/"); return false; }
        if (key == GLFW.GLFW_KEY_KP_DECIMAL) { CalcExpression.appendToExpression("."); return false; }

        if (shift) {
            switch (key) {
                case GLFW.GLFW_KEY_EQUAL -> { CalcExpression.appendToExpression("+"); return false; }
                case GLFW.GLFW_KEY_8 -> { CalcExpression.appendToExpression("*"); return false; }
                case GLFW.GLFW_KEY_9 -> { CalcExpression.appendToExpression("("); return false; }
                case GLFW.GLFW_KEY_0 -> { CalcExpression.appendToExpression(")"); return false; }
                case GLFW.GLFW_KEY_6 -> { CalcExpression.appendToExpression("^"); return false; }
                case GLFW.GLFW_KEY_5 -> { CalcExpression.appendToExpression("%"); return false; }
            }
        }

        if (!shift) {
            if (key == GLFW.GLFW_KEY_MINUS) { CalcExpression.appendToExpression("-"); return false; }
            if (key == GLFW.GLFW_KEY_SLASH) { CalcExpression.appendToExpression("/"); return false; }
            if (key == GLFW.GLFW_KEY_PERIOD) { CalcExpression.appendToExpression("."); return false; }
        }

        if (!shift) {
            switch (key) {
                case GLFW.GLFW_KEY_S -> { CalcExpression.appendToExpression("sin("); return false; }
                case GLFW.GLFW_KEY_C -> { CalcExpression.appendToExpression("cos("); return false; }
                case GLFW.GLFW_KEY_T -> { CalcExpression.appendToExpression("tan("); return false; }
                case GLFW.GLFW_KEY_Q -> { CalcExpression.appendToExpression("sqrt("); return false; }
                case GLFW.GLFW_KEY_L -> { CalcExpression.appendToExpression("log("); return false; }
            }
        }

        return false;
    }
}
