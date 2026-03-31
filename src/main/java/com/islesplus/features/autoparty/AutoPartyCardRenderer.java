package com.islesplus.features.autoparty;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class AutoPartyCardRenderer {

    private static final int PANEL_SOFT   = 0xEE262626;
    private static final int PANEL_HOVER  = 0xEE313131;
    private static final int BORDER       = 0xFF464646;
    private static final int TEXT_PRIMARY = 0xFFF0F0F0;
    private static final int TEXT_MUTED   = 0xFFB3B3B3;
    private static final int POSITIVE     = 0xFF38D286;
    private static final int NEGATIVE     = 0xFFE74C3C;
    private static final int BG_ITEM      = 0xEE1A1A1A;
    private static final int ACCENT       = 0xFF5AB8FF;

    public static int focusedIdx = -1;
    public static boolean editingGroupName = false;

    private AutoPartyCardRenderer() {}

    // ================================================================
    // Height
    // ================================================================

    public static int getExpandedHeight() {
        int h = 4;
        h += 14 + 4; // auto leave/disband toggle + gap

        if (!AutoParty.groups.isEmpty()) {
            h += 16 + 4; // group selector row + gap
        }

        List<String> members = getDisplayList();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) h += 4;
            h += friendRowHeight();
        }
        h += 4 + 14; // gap + Add Friend button
        if (!AutoParty.groups.isEmpty()) {
            h += 4 + 14; // gap + New Group button
        } else {
            h += 4 + 14; // gap + Create Group button
        }
        h += 4; // bottom pad
        return h;
    }

    private static int friendRowHeight() {
        return 4 + 14 + 4; // top pad + text field + bottom pad
    }

    private static List<String> getDisplayList() {
        if (!AutoParty.groups.isEmpty() && AutoParty.activeGroupIdx >= 0
                && AutoParty.activeGroupIdx < AutoParty.groups.size()) {
            return AutoParty.groups.get(AutoParty.activeGroupIdx).members;
        }
        return AutoParty.friends;
    }

    // ================================================================
    // Drawing
    // ================================================================

    public static void draw(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        int curY = y + 4;

        // Auto leave/disband toggle
        boolean toggleHov = isInside(mouseX, mouseY, x, curY, width, 14);
        if (toggleHov) ctx.fill(x, curY, x + width, curY + 14, PANEL_HOVER);
        drawMiniToggle(ctx, tr, x + 4, curY + 3, "Auto leave/disband", AutoParty.autoLeaveDisband);
        curY += 14 + 4;

        // Group selector (if groups exist)
        if (!AutoParty.groups.isEmpty()) {
            drawGroupSelector(ctx, tr, x, curY, width, mouseX, mouseY);
            curY += 16 + 4;
        }

        // Friend/member list
        List<String> members = getDisplayList();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) curY += 4;
            drawFriendRow(ctx, tr, i, x, curY, width, mouseX, mouseY, members);
            curY += friendRowHeight();
        }

        // Add Friend button
        curY += 4;
        drawCenteredButton(ctx, tr, x, curY, width, 80, "+ Add Friend", mouseX, mouseY);
        curY += 14;

        // Group management button
        curY += 4;
        if (AutoParty.groups.isEmpty()) {
            drawCenteredButton(ctx, tr, x, curY, width, 90, "+ Create Group", mouseX, mouseY);
        } else {
            drawCenteredButton(ctx, tr, x, curY, width, 90, "+ New Group", mouseX, mouseY);
        }
    }

    private static void drawGroupSelector(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        AutoParty.PartyGroup group = AutoParty.groups.get(AutoParty.activeGroupIdx);

        // Background bar
        ctx.fill(x, y, x + width, y + 16, BG_ITEM);
        ctx.fill(x, y, x + width, y + 1, BORDER);
        ctx.fill(x, y + 15, x + width, y + 16, BORDER);

        // Left arrow
        int arrowW = 16;
        boolean leftHov = isInside(mouseX, mouseY, x, y, arrowW, 16);
        if (leftHov) ctx.fill(x, y + 1, x + arrowW, y + 15, PANEL_HOVER);
        ctx.drawText(tr, Text.literal("<"), x + 5, y + 4, TEXT_MUTED, false);

        // Right arrow
        boolean rightHov = isInside(mouseX, mouseY, x + width - arrowW, y, arrowW, 16);
        if (rightHov) ctx.fill(x + width - arrowW, y + 1, x + width, y + 15, PANEL_HOVER);
        ctx.drawText(tr, Text.literal(">"), x + width - arrowW + 5, y + 4, TEXT_MUTED, false);

        // Group name (centered) - editable if editingGroupName
        String name = group.name;
        if (editingGroupName && focusedIdx == -2) {
            boolean showCursor = (Util.getMeasuringTimeMs() % 1000) < 500;
            name = name + (showCursor ? "|" : "");
        }
        int nameW = tr.getWidth(name);
        int nameX = x + (width - nameW) / 2;
        boolean nameHov = isInside(mouseX, mouseY, x + arrowW, y, width - arrowW * 2, 16);
        int nameColor = editingGroupName && focusedIdx == -2 ? ACCENT : (nameHov ? TEXT_PRIMARY : TEXT_MUTED);
        ctx.drawText(tr, Text.literal(name), nameX, y + 4, nameColor, false);

        // Delete group button (x) on far right of name area
        int delX = x + width - arrowW - 14;
        boolean delHov = isInside(mouseX, mouseY, delX, y + 2, 12, 12);
        ctx.drawText(tr, Text.literal("x"), delX + 2, y + 4, delHov ? NEGATIVE : TEXT_MUTED, false);
    }

    private static void drawFriendRow(DrawContext ctx, TextRenderer tr, int idx,
                                      int x, int y, int width, int mouseX, int mouseY,
                                      List<String> members) {
        int innerH = friendRowHeight();
        ctx.fill(x, y, x + width, y + innerH, BG_ITEM);
        ctx.fill(x, y, x + width, y + 1, BORDER);
        ctx.fill(x, y + innerH - 1, x + width, y + innerH, BORDER);

        int innerX = x + 4;
        int innerW = width - 8;
        int fieldY = y + 4;

        // Delete button on the right
        int delX = x + width - 14;
        ctx.drawText(tr, Text.literal("\u00d7"), delX + 1, fieldY + 1, NEGATIVE, false);

        // Text field takes remaining space
        int fieldW = innerW - 14;
        boolean focused = idx == focusedIdx && !editingGroupName;
        drawTextField(ctx, tr, innerX, fieldY, fieldW, members.get(idx), focused);
    }

    private static void drawCenteredButton(DrawContext ctx, TextRenderer tr, int x, int y, int width, int btnW, String label, int mouseX, int mouseY) {
        int btnX = x + (width - btnW) / 2;
        boolean btnHov = isInside(mouseX, mouseY, btnX, y, btnW, 14);
        ctx.fill(btnX, y, btnX + btnW, y + 14, btnHov ? PANEL_HOVER : PANEL_SOFT);
        ctx.fill(btnX, y, btnX + btnW, y + 1, BORDER);
        ctx.fill(btnX, y + 13, btnX + btnW, y + 14, BORDER);
        ctx.drawText(tr, Text.literal(label), btnX + (btnW - tr.getWidth(label)) / 2, y + 3, TEXT_MUTED, false);
    }

    // ================================================================
    // Clicks
    // ================================================================

    public static boolean onClick(int mouseX, int mouseY, int x, int y, int width) {
        int curY = y + 4;

        // Auto leave/disband toggle
        if (isInside(mouseX, mouseY, x, curY, width, 14)) {
            AutoParty.autoLeaveDisband = !AutoParty.autoLeaveDisband;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        curY += 14 + 4;

        // Group selector
        if (!AutoParty.groups.isEmpty()) {
            if (isInside(mouseX, mouseY, x, curY, width, 16)) {
                int arrowW = 16;
                // Left arrow
                if (isInside(mouseX, mouseY, x, curY, arrowW, 16)) {
                    AutoParty.activeGroupIdx = (AutoParty.activeGroupIdx - 1 + AutoParty.groups.size()) % AutoParty.groups.size();
                    focusedIdx = -1;
                    editingGroupName = false;
                    com.islesplus.IslesPlusConfig.save();
                    return true;
                }
                // Right arrow
                if (isInside(mouseX, mouseY, x + width - arrowW, curY, arrowW, 16)) {
                    AutoParty.activeGroupIdx = (AutoParty.activeGroupIdx + 1) % AutoParty.groups.size();
                    focusedIdx = -1;
                    editingGroupName = false;
                    com.islesplus.IslesPlusConfig.save();
                    return true;
                }
                // Delete group button
                int delX = x + width - arrowW - 14;
                if (isInside(mouseX, mouseY, delX, curY + 2, 12, 12)) {
                    AutoParty.groups.remove(AutoParty.activeGroupIdx);
                    if (AutoParty.groups.isEmpty()) {
                        AutoParty.activeGroupIdx = -1;
                    } else {
                        AutoParty.activeGroupIdx = Math.min(AutoParty.activeGroupIdx, AutoParty.groups.size() - 1);
                    }
                    focusedIdx = -1;
                    editingGroupName = false;
                    com.islesplus.IslesPlusConfig.save();
                    return true;
                }
                // Click on group name - edit it
                focusedIdx = -2; // special: editing group name
                editingGroupName = true;
                return true;
            }
            curY += 16 + 4;
        }

        // Friend/member rows
        List<String> members = getDisplayList();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) curY += 4;
            int rh = friendRowHeight();
            if (isInside(mouseX, mouseY, x, curY, width, rh)) {
                // Delete button
                int delX = x + width - 14;
                if (isInside(mouseX, mouseY, delX, curY + 4, 12, 12)) {
                    members.remove(i);
                    if (focusedIdx >= members.size()) focusedIdx = -1;
                    com.islesplus.IslesPlusConfig.save();
                    return true;
                }
                // Text field
                focusedIdx = i;
                editingGroupName = false;
                return true;
            }
            curY += rh;
        }

        // Add Friend button
        curY += 4;
        int addBtnW = 80;
        int addBtnX = x + (width - addBtnW) / 2;
        if (isInside(mouseX, mouseY, addBtnX, curY, addBtnW, 14)) {
            members.add("");
            focusedIdx = members.size() - 1;
            editingGroupName = false;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }
        curY += 14;

        // Group management button
        curY += 4;
        int grpBtnW = 90;
        int grpBtnX = x + (width - grpBtnW) / 2;
        if (isInside(mouseX, mouseY, grpBtnX, curY, grpBtnW, 14)) {
            if (AutoParty.groups.isEmpty()) {
                // Create first group from existing friends
                AutoParty.PartyGroup group = new AutoParty.PartyGroup("Group 1");
                group.members.addAll(AutoParty.friends);
                AutoParty.groups.add(group);
                AutoParty.activeGroupIdx = 0;
            } else {
                // Add new empty group
                AutoParty.PartyGroup group = new AutoParty.PartyGroup("Group " + (AutoParty.groups.size() + 1));
                AutoParty.groups.add(group);
                AutoParty.activeGroupIdx = AutoParty.groups.size() - 1;
            }
            focusedIdx = -1;
            editingGroupName = false;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }

        focusedIdx = -1;
        editingGroupName = false;
        return false;
    }

    // ================================================================
    // Keyboard
    // ================================================================

    public static boolean onChar(CharInput input) {
        if (!input.isValidChar()) return false;
        String ch = input.asString();

        // Editing group name
        if (editingGroupName && focusedIdx == -2 && !AutoParty.groups.isEmpty()) {
            AutoParty.PartyGroup group = AutoParty.groups.get(AutoParty.activeGroupIdx);
            group.name = group.name + ch;
            com.islesplus.IslesPlusConfig.save();
            return true;
        }

        // Editing friend name
        List<String> members = getDisplayList();
        if (focusedIdx < 0 || focusedIdx >= members.size()) return false;
        if (ch.equals(" ")) return true; // no spaces in usernames
        members.set(focusedIdx, members.get(focusedIdx) + ch);
        com.islesplus.IslesPlusConfig.save();
        return true;
    }

    public static boolean onKey(KeyInput input) {
        int key = input.key();

        // Editing group name
        if (editingGroupName && focusedIdx == -2 && !AutoParty.groups.isEmpty()) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                AutoParty.PartyGroup group = AutoParty.groups.get(AutoParty.activeGroupIdx);
                if (!group.name.isEmpty()) {
                    group.name = group.name.substring(0, group.name.length() - 1);
                    com.islesplus.IslesPlusConfig.save();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                editingGroupName = false;
                focusedIdx = -1;
                return true;
            }
            return false;
        }

        if (focusedIdx < 0) return false;
        List<String> members = getDisplayList();
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (focusedIdx < members.size()) {
                String cur = members.get(focusedIdx);
                if (!cur.isEmpty()) {
                    members.set(focusedIdx, cur.substring(0, cur.length() - 1));
                    com.islesplus.IslesPlusConfig.save();
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            focusedIdx = -1;
            return true;
        }
        return false;
    }

    public static void reset() {
        focusedIdx = -1;
        editingGroupName = false;
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static void drawMiniToggle(DrawContext ctx, TextRenderer tr, int x, int y, String label, boolean enabled) {
        int w = 24, h = 10;
        ctx.fill(x, y, x + w, y + h, enabled ? POSITIVE : 0xFF5B6678);
        int knobW = h - 4;
        int knobX = enabled ? x + w - knobW - 2 : x + 2;
        ctx.fill(knobX, y + 2, knobX + knobW, y + 2 + knobW, 0xFFFFFFFF);
        ctx.drawText(tr, Text.literal(label), x + w + 4, y + 1, enabled ? TEXT_PRIMARY : TEXT_MUTED, false);
    }

    private static void drawTextField(DrawContext ctx, TextRenderer tr,
                                      int x, int y, int w, String value, boolean focused) {
        ctx.fill(x, y, x + w, y + 12, BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 11, focused ? 0xFF333333 : PANEL_SOFT);
        boolean showCursor = focused && (Util.getMeasuringTimeMs() % 1000) < 500;
        String display = value + (showCursor ? "|" : "");
        int maxW = w - 6;
        while (!display.isEmpty() && tr.getWidth(display) > maxW) display = display.substring(1);
        String placeholder = value.isEmpty() && !focused ? "Username..." : "";
        if (!placeholder.isEmpty()) {
            ctx.drawText(tr, Text.literal(placeholder), x + 3, y + 2, TEXT_MUTED, false);
        } else {
            ctx.drawText(tr, Text.literal(display), x + 3, y + 2, TEXT_PRIMARY, false);
        }
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
