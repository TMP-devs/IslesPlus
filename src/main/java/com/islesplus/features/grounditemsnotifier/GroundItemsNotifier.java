package com.islesplus.features.grounditemsnotifier;

import com.islesplus.entity.EntityScanResult;
import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

import java.util.*;

public final class GroundItemsNotifier {

    public static final SoundConfig DEFAULT_ITEM_SOUND =
        new SoundConfig("minecraft:block.note_block.pling", 0.90f, 1.00f);

    // ---- Per-item data ----

    public static class WatchedItem {
        public boolean enabled = true;
        public String customKeyword = "";
        public boolean lineTracker = true;
        public boolean screenNotifier = true;
        public boolean highlight = true;
        public boolean soundPing = false;
        public SoundConfig soundConfig = DEFAULT_ITEM_SOUND.copy();

        public String getMatchKeyword() {
            return customKeyword;
        }
    }

    // ---- State ----

    public static boolean groundItemsNotifierEnabled = false;
    public static float glowHue = 0.083f;
    public static final List<WatchedItem> watchedItems = new ArrayList<>();

    private static volatile Map<Integer, WatchedItem> matchingEntityMap = Map.of();
    private static Set<Integer> prevMatchingIds = Set.of();

    private GroundItemsNotifier() {}

    // ---- Tick ----

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (!groundItemsNotifierEnabled || FeatureFlags.isKilled("ground_items_notifier") || client.player == null || watchedItems.isEmpty()) {
            matchingEntityMap = Map.of();
            prevMatchingIds = Set.of();
            return;
        }

        Map<Integer, WatchedItem> next = new HashMap<>();
        for (Entity entity : scan.itemEntities) {
            if (!(entity instanceof ItemEntity ie)) continue;
            ItemStack stack = ie.getStack();
            if (stack.isEmpty()) continue;
            String combined = (stack.getItem().toString() + " " + stack.getComponents().toString())
                .toLowerCase(Locale.ROOT);
            for (WatchedItem item : watchedItems) {
                if (!item.enabled) continue;
                String kw = item.getMatchKeyword();
                if (kw == null || kw.isBlank()) continue;
                if (combined.contains(kw.toLowerCase(Locale.ROOT))) {
                    next.put(entity.getId(), item);
                    break; // first matching item config wins
                }
            }
        }
        matchingEntityMap = Map.copyOf(next);

        // Sound ping for newly appeared matching entities
        Set<Integer> newIds = new HashSet<>(next.keySet());
        newIds.removeAll(prevMatchingIds);
        for (int newId : newIds) {
            WatchedItem item = next.get(newId);
            if (item != null && item.soundPing) {
                ModSounds.playConfig(client, item.soundConfig);
                break; // one ping per tick max
            }
        }
        prevMatchingIds = Set.copyOf(next.keySet());
    }

    // ---- Queries used by renderers ----

    public static boolean shouldForceGlow(Entity entity) {
        if (!groundItemsNotifierEnabled || entity == null) return false;
        WatchedItem item = matchingEntityMap.get(entity.getId());
        return item != null && item.highlight;
    }

    public static boolean hasScreenNotifiers() {
        if (!groundItemsNotifierEnabled) return false;
        for (WatchedItem item : matchingEntityMap.values()) {
            if (item.screenNotifier) return true;
        }
        return false;
    }

    public static Map<Integer, WatchedItem> getMatchingEntityMap() {
        return matchingEntityMap;
    }

    public static void reset() {
        matchingEntityMap = Map.of();
        prevMatchingIds = Set.of();
    }
}
