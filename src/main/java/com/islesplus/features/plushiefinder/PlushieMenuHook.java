package com.islesplus.features.plushiefinder;

import com.islesplus.mixin.HandledScreenAccessor;
import com.islesplus.sync.FeatureFlags;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlushieMenuHook {
    /** "Plushy #5", "Plushie #5", "Plushy 5", all of it */
    private static final Pattern PLUSHIE_NUM =
        Pattern.compile("(?i)plush(?:y|ie?)\\s*#?(\\d+)");

    /** How often we re-read the slots. The server swaps page contents inside the *same* screen,
     * so there is no event to hang this off - we poll, cheaply. */
    private static final long SCAN_INTERVAL_MS = 100L;

    /** What the last scan saw, so we only touch the repository when the page actually changes
     * (paging to the next page, or a plushie flipping to found while the menu is open).
     * Empty/null means "the open screen is not showing plushies right now". */
    private static Map<Integer, Boolean> lastSeenPage = Collections.emptyMap();
    private static long lastScanAtMs = 0L;

    private PlushieMenuHook() {}

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?>)) return;
            forgetLastPage();
            ScreenEvents.afterRender(screen).register(
                (s, ctx, mx, my, dt) -> onRender(s, ctx, s.height));
        });
    }

    /** Drop what we think is on screen, so the next scan syncs from scratch. Called when a menu
     * opens and after the found list is reset. */
    public static void forgetLastPage() {
        lastSeenPage = Collections.emptyMap();
        lastScanAtMs = 0L;
    }

    // -------------------------------------------------------------------------

    /**
     * Runs every frame while a menu is open. Items show up a frame or two after the screen does,
     * and the plushie menu pages by rewriting the slots of the screen that is already open, so we
     * re-read the slots on a timer and sync whenever what they show has changed.
     */
    private static void onRender(net.minecraft.client.gui.screen.Screen screen, DrawContext ctx, int screenHeight) {
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (!PlushieFinder.plushieFinderEnabled || FeatureFlags.isKilled("plushie_finder")) return;

        long now = System.currentTimeMillis();
        if (now - lastScanAtMs >= SCAN_INTERVAL_MS) {
            lastScanAtMs = now;
            Map<Integer, Boolean> page = readPage(hs);
            if (page.isEmpty()) {
                // Same screen, but it is not showing plushies any more (Go Back, a sub-menu, ...).
                lastSeenPage = Collections.emptyMap();
            } else if (!page.equals(lastSeenPage)) {
                PlushieRepository.markSeen(page.keySet());
                PlushieRepository.setOwnedBatch(page);
                lastSeenPage = page;
            }
        }

        if (lastSeenPage.isEmpty()) return;
        drawOverlay(ctx, hs, screenHeight);
    }

    /**
     * Read every plushie the container is showing: number -> owned, worked out from the item type
     * (paper = owned, ghast tear = not). Player inventory slots are skipped so a plushie-named item
     * being carried can never flip a number the menu did not say anything about.
     */
    private static Map<Integer, Boolean> readPage(HandledScreen<?> screen) {
        ScreenHandler handler = ((HandledScreenAccessor) screen).getHandler();
        Map<Integer, Boolean> page = new HashMap<>();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) continue;
            ItemStack stack = slot.getStack();
            // Item type first: it is two reference compares, and it is what decides owned anyway.
            // Every other menu we poll therefore costs nothing but this check per slot - no name
            // building, no regex.
            boolean owned = stack.isOf(Items.PAPER);
            if (!owned && !stack.isOf(Items.GHAST_TEAR)) continue;

            Matcher m = PLUSHIE_NUM.matcher(stack.getName().getString());
            if (!m.find()) continue;

            int num;
            try { num = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException e) { continue; }

            page.put(num, owned);
        }
        return page;
    }

    /** The tally above the menu, plus a nudge line while pages of the list are still unread. */
    private static void drawOverlay(DrawContext ctx, HandledScreen<?> screen, int screenHeight) {
        List<PlushieEntry> plushies = PlushieRepository.getCachedPlushies();
        if (plushies.isEmpty()) return;

        // Ghosts count on neither side, or a stale "found" for a plushie the menu no longer lists
        // would put the tally above its own total.
        int found = 0;
        for (PlushieEntry p : plushies) {
            if (PlushieRepository.isOwned(p.num) && !PlushieRepository.isGhost(p.num)) found++;
        }
        int total = PlushieRepository.findableCount();
        int unread = PlushieRepository.unreadCount();

        String tally = "Plushie Finder  -  " + found + " / " + total + " found";
        // An unread page looks exactly like a page of plushies you have not found, so say so here,
        // where the player is already looking and one click from fixing it.
        String nudge = unread > 0
            ? "Open the other pages  -  " + (total - unread) + " / " + total + " read"
            : null;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int lineH = tr.fontHeight + 1;
        int tw = tr.getWidth(tally);
        if (nudge != null) tw = Math.max(tw, tr.getWidth(nudge));
        int blockH = nudge == null ? tr.fontHeight : tr.fontHeight + lineH;

        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int guiX = acc.getGuiX();
        int guiY = acc.getGuiY();
        int guiW = acc.getGuiWidth();

        // centered above the menu with a bit of a gap
        int ox = guiX + (guiW - tw) / 2;
        int oy = guiY - blockH - 6;

        // don't let it go off the top of the screen on tall menus
        oy = Math.max(2, oy);

        ctx.fill(ox - 3, oy - 2, ox + tw + 3, oy + blockH + 2, 0xBB141414);
        ctx.drawText(tr, Text.literal(tally), ox, oy, 0xFFD4AF37, true);
        if (nudge != null) {
            ctx.drawText(tr, Text.literal(nudge), ox, oy + lineH, 0xFFF2A640, true);
        }
    }
}
