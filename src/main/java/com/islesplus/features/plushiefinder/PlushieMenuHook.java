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
    /** "Plushy #5", "Plushie #5", "Plushy 5", all of it */
    private static final Pattern PLUSHIE_NUM =
        Pattern.compile("(?i)plush(?:y|ie?)\\s*#?(\\d+)");

    // reset when a new screen opens so we only sync once per menu, not every frame
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
     * runs every frame while a menu is open. items show up a frame or two after the screen
     * does, so the first frame we actually see plushies we sync ownership, then just draw
     * the overlay from there on
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

    /** does any slot have something named like "✔ Plushy #3" */
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
     * go through every slot, for each plushie item work out owned/not from the item type
     * (paper = owned, ghast tear = not) and push that into PlushieRepository
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

    /** the little "x / y found" text above the menu */
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

        // centered above the menu with a bit of a gap
        int ox = guiX + (guiW - tw) / 2;
        int oy = guiY - tr.fontHeight - 6;

        // don't let it go off the top of the screen on tall menus
        oy = Math.max(2, oy);

        ctx.fill(ox - 3, oy - 2, ox + tw + 3, oy + tr.fontHeight + 2, 0xBB141414);
        ctx.drawText(tr, Text.literal(text), ox, oy, 0xFFD4AF37, true);
    }
}
