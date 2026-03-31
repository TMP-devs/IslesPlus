package com.islesplus.features.bosstracker;

import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

public final class BossaryHook {

    private static final int SCAN_DELAY_TICKS = 5;

    private static State state = State.IDLE;
    private static int tickCounter = 0;

    private enum State { IDLE, WAITING_FOR_SCREEN, SCANNING }

    private BossaryHook() {}

    /** Call when /bossary is about to be sent so we know to watch for the screen. */
    public static void expectBossary() {
        state = State.WAITING_FOR_SCREEN;
        tickCounter = 0;
    }

    public static void onScreenOpen(HandledScreen<?> screen) {
        if (state == State.WAITING_FOR_SCREEN) {
            state = State.SCANNING;
            tickCounter = 0;
        }
    }

    public static void onScreenTick(HandledScreen<?> screen) {
        if (!BossTracker.bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return;

        // State-machine path: auto-opened /bossary with tick delay (mirrors NerdModeActivator)
        if (state == State.SCANNING) {
            tickCounter++;
            if (tickCounter < SCAN_DELAY_TICKS) return;
            state = State.IDLE;
            if (tryParseBossary(screen)) {
                if (BossTracker.pendingAutoClose) {
                    BossTracker.pendingAutoClose = false;
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) client.player.closeHandledScreen();
                }
            }
            return;
        }

        // Passive path: user manually opened /bossary, or retry after SCANNING
        // didn't find items in time — also handles pending auto-close
        if (state == State.IDLE) {
            if (tryParseBossary(screen)) {
                if (BossTracker.pendingAutoClose) {
                    BossTracker.pendingAutoClose = false;
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) client.player.closeHandledScreen();
                }
            }
        }
    }

    public static void reset() {
        state = State.IDLE;
        tickCounter = 0;
    }

    /**
     * Returns true if this screen looked like a bossary and was parsed.
     */
    private static boolean tryParseBossary(HandledScreen<?> screen) {
        if (!BossTracker.bossTrackerEnabled) return false;
        List<ItemStack> ghastTears = new ArrayList<>();
        boolean hasBossEntry = false;
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.GHAST_TEAR) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;
            boolean hasCooldown = false, hasNextSpawn = false, hasWarmup = false,
                    hasDamageMilestone = false, hasBeacon = false, hasPossibleDrops = false;
            for (net.minecraft.text.Text line : lore.lines()) {
                String l = line.getString();
                if (l.contains("Cooldown duration:"))  hasCooldown = true;
                if (l.contains("Next spawn in:"))      hasNextSpawn = true;
                if (l.contains("Warmup:"))             hasWarmup = true;
                if (l.contains("Damage milestone:"))   hasDamageMilestone = true;
                if (l.contains("Beacon location:"))    hasBeacon = true;
                if (l.contains("POSSIBLE DROPS:"))     hasPossibleDrops = true;
            }
            if (hasCooldown && hasNextSpawn && hasWarmup && hasDamageMilestone && hasBeacon && hasPossibleDrops) {
                hasBossEntry = true;
            }
            ghastTears.add(stack);
        }
        if (hasBossEntry && !ghastTears.isEmpty()) {
            BossTracker.parseBossaryItems(ghastTears);
            return true;
        }
        return false;
    }
}
