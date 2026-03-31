package com.islesplus.features.plushiefinder;

import com.islesplus.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlushieMenuHook {
    /** Matches "Plushy #5", "Plushie #5", "Plushy 5", etc. */
    private static final Pattern PLUSHIE_NUM =
        Pattern.compile("(?i)plush(?:y|ie?)\\s*#?(\\d+)");

    // Reset each time a new HandledScreen opens; prevents re-syncing every frame.
    private static boolean syncedThisScreen = false;

    private PlushieMenuHook() {}

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?>)) return;
            syncedThisScreen = false;
            ScreenEvents.afterRender(screen).register(
                (s, ctx, mx, my, dt) -> onRender(s, ctx, s.height));
        });
    }

    // -------------------------------------------------------------------------

    /**
     * Called every frame while a HandledScreen is open.
     * On the first frame we detect plushie items (items arrive after screen open),
     * we sync ownership and then draw the overlay on every frame after that.
     */
    private static void onRender(net.minecraft.client.gui.screen.Screen screen, DrawContext ctx, int screenHeight) {
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (!PlushieFinder.plushieFinderEnabled) return;
        if (!hasPlushieItems(hs)) return;

        if (!syncedThisScreen) {
            syncFromScreen(hs);
            syncedThisScreen = true;
        }

        drawOverlay(ctx, hs, screenHeight);
    }

    /**
     * Returns true if any slot in the screen contains an item whose name
     * matches the plushie number pattern (e.g. "✔ Plushy #3").
     */
    private static boolean hasPlushieItems(HandledScreen<?> screen) {
        ScreenHandler handler = ((HandledScreenAccessor) screen).getHandler();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (PLUSHIE_NUM.matcher(stack.getName().getString()).find()) return true;
        }
        return false;
    }

    /**
     * Scan every non-empty slot. For each item whose name matches the plushie
     * number pattern, determine owned status from item type and update PlushieRepository.
     */
    private static void syncFromScreen(HandledScreen<?> screen) {
        ScreenHandler handler = ((HandledScreenAccessor) screen).getHandler();
        Map<Integer, Boolean> updates = new HashMap<>();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            Matcher m = PLUSHIE_NUM.matcher(stack.getName().getString());
            if (!m.find()) continue;

            int num;
            try { num = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { continue; }

            if (stack.isOf(Items.PAPER)) {
                updates.put(num, true);
            } else if (stack.isOf(Items.GHAST_TEAR)) {
                updates.put(num, false);
            }
        }
        PlushieRepository.setOwnedBatch(updates);
    }

    /** Small overlay drawn on the left-center of the screen. */
    private static void drawOverlay(DrawContext ctx, HandledScreen<?> screen, int screenHeight) {
        List<PlushieEntry> plushies = PlushieRepository.getCachedPlushies();
        if (plushies.isEmpty()) return;

        int found = 0;
        for (PlushieEntry p : plushies) {
            if (PlushieRepository.isOwned(p.num)) found++;
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String text = "Plushie Finder  -  " + found + " / " + plushies.size() + " found";
        int tw = tr.getWidth(text);
        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int guiX = acc.getGuiX();
        int guiY = acc.getGuiY();
        int guiW = acc.getGuiWidth();

        // Center above the open handled UI, with a small top gap.
        int ox = guiX + (guiW - tw) / 2;
        int oy = guiY - tr.fontHeight - 6;

        // Keep visible even if a UI is very close to the top edge.
        oy = Math.max(2, oy);

        ctx.fill(ox - 3, oy - 2, ox + tw + 3, oy + tr.fontHeight + 2, 0xBB141414);
        ctx.drawText(tr, Text.literal(text), ox, oy, 0xFFD4AF37, true);
    }
}
