package com.islesplus.screen.islesscreen;

import com.islesplus.IslesClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

final class KeybindsCardRenderer {

    private static final int TEXT_PRIMARY = 0xFFF0F0F0;
    private static final int TEXT_MUTED   = 0xFFB3B3B3;
    private static final int TEXT_UNBOUND = 0xFF666666;
    private static final int POSITIVE     = 0xFF38D286;
    private static final int BORDER       = 0xFF464646;

    private static final int ROW_H = 14;

    private KeybindsCardRenderer() {}

    private static final String[][] ENTRIES = {
        { "Lock Slot",              null }, // filled dynamically
        { "Confirm Full Inventory", null },
        { "Open Backpack (/bp)",    null },
        { "Open Trash (/trash)",    null },
        { "Open Resource Vault",    null },
        { "Auto Party",             null },
        { "Party Warp (/p warp)",   null },
    };

    private static KeyBinding[] bindings() {
        return new KeyBinding[]{
            IslesClient.LOCK_SLOT_KEY,
            IslesClient.CONFIRM_INVENTORY_FULL_KEY,
            IslesClient.BACKPACK_KEY,
            IslesClient.TRASH_KEY,
            IslesClient.RESOURCE_VAULT_KEY,
            IslesClient.AUTO_PARTY_KEY,
            IslesClient.PARTY_WARP_KEY,
        };
    }

    public static int getExpandedHeight() {
        return ENTRIES.length * ROW_H + 4 + ROW_H + 4; // rows + gap + hint row + bottom pad
    }

    public static void draw(DrawContext ctx, TextRenderer tr, int x, int y, int width, int mouseX, int mouseY) {
        KeyBinding[] keys = bindings();
        int curY = y + 2;

        for (int i = 0; i < ENTRIES.length; i++) {
            String label = ENTRIES[i][0];
            KeyBinding kb = keys[i];
            boolean unbound = kb.isUnbound();
            String bindStr = unbound ? "unbound" : kb.getBoundKeyLocalizedText().getString();
            int bindColor = unbound ? TEXT_UNBOUND : POSITIVE;

            ctx.drawText(tr, Text.literal(label), x + 4, curY + 2, TEXT_MUTED, false);
            int bindW = tr.getWidth(bindStr);
            ctx.drawText(tr, Text.literal(bindStr), x + width - 4 - bindW, curY + 2, bindColor, false);
            curY += ROW_H;
        }

        // Separator + hint
        curY += 2;
        ctx.fill(x + 4, curY, x + width - 4, curY + 1, BORDER);
        curY += 4;
        String hint = "Configure: Options \u25BA Controls \u25BA Isles+";
        ctx.drawText(tr, Text.literal(hint), x + 4, curY + 1, TEXT_UNBOUND, false);
    }
}
