package com.islesplus.features.storagecount;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** The slot label for a storage stack ({@link StorageCount}); called from HandledScreenMixin. */
public final class StorageSlotLabel {
    /** Where vanilla's count ends (its x + 19 - 2 and y + 6 + 3, 8 tall). */
    private static final int RIGHT = 17, BOTTOM = 17, TEXT_H = 8;
    /** Vanilla's count digits are 6 px apart, so three of them are wider than the slot and run into
     * the next slot's count. A label wider than this steps down a size. */
    private static final int ROOM = 16;

    private StorageSlotLabel() {}

    /** The amount in the slot's bottom-right corner, in the game's own font and shadow, exactly
     * where vanilla draws a count - smaller when it would not fit the slot. */
    public static void draw(DrawContext ctx, String label, int slotX, int slotY) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int w = tr.getWidth(label);
        int gui = Math.max(1, (int) Math.round(client.getWindow().getScaleFactor()));
        float s = StorageCount.fitScale(w, gui, ROOM);
        if (s == 1f) {
            ctx.drawText(tr, label, slotX + RIGHT - w, slotY + BOTTOM - TEXT_H, 0xFFFFFFFF, true);
            return;
        }
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate(slotX + RIGHT - w * s, slotY + BOTTOM - TEXT_H * s);
            ctx.getMatrices().scale(s, s);
            ctx.drawText(tr, label, 0, 0, 0xFFFFFFFF, true);
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    /** The label to draw instead of the stack's own count, or {@code vanilla} to leave it be. */
    public static String of(ItemStack stack, String vanilla) {
        // vanilla passes its own label while you drag a stack across slots: that one stays
        if (vanilla != null || !StorageCount.storageCountEnabled || stack.isEmpty()
                || FeatureFlags.isKilled("storage_count")) return vanilla;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;
        List<String> lines = new ArrayList<>(lore.lines().size());
        for (Text line : lore.lines()) lines.add(line.getString());
        int amount = StorageCount.stored(lines);
        return amount < 0 ? null : StorageCount.label(amount);
    }
}
