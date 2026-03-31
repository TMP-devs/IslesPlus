package com.islesplus.features.bosstracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

import java.util.*;
import java.util.regex.*;


public final class BossTracker {

    public static boolean bossTrackerEnabled = false;

    public enum BossHudPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public static BossHudPosition hudPosition = BossHudPosition.TOP_LEFT;
    public static boolean autoOpenBossary = false;
    public static final java.util.Set<String> hiddenBossNames = new java.util.HashSet<>();

    public enum BossState { UNKNOWN, COOLDOWN, READY, SPAWNING, SPAWNED }

    public static class TrackedBoss {
        public final String name;
        public long remainingMs;
        public BossState state;
        public long lastEntityMs = 0; // last time seen via world entity scan
        public long spawnedAtMs = 0;  // when boss entered SPAWNED state
        public TrackedBoss(String name, long remainingMs, BossState state) {
            this.name = name; this.remainingMs = remainingMs; this.state = state;
        }
    }

    public static final List<TrackedBoss> bosses = new ArrayList<>();
    public static long lastBossaryParseMs = 0;
    private static long lastEntityScanMs = 0;
    private static long lastStaleTickMs = 0;
    private static long lastSpawnTickMs = 0;
    private static long worldJoinMs = 0;
    private static boolean hintSent = false;
    // For multi-line chat detection
    private static String pendingDefeatBoss = null;
    private static long pendingDefeatExpiry = 0;
    private static boolean pendingSpawnEvent = false;
    private static long pendingSpawnEventExpiry = 0;
    public static boolean pendingAutoClose = false;
    public static boolean pendingReopenMenu = false;
    public static boolean pendingBossaryOpen = false;

    private static final long COOLDOWN_MS = 7_200_000L;
    private static final long SPAWNED_TIMEOUT_MS = 300_000L; // 5 minutes
    private static final long SPAWNED_FALLBACK_MS = 7_080_000L; // 1h 58m (2h minus ~2m spawn)

    private static final Pattern READY_PATTERN = Pattern.compile("The (.+?) is ready to be spawned!");
    private static final Pattern SKULL_PATTERN = Pattern.compile("☠\\s+(.+?)\\s+☠");
    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d+)h");
    private static final Pattern MINS_PATTERN  = Pattern.compile("(\\d+)m");
    private static final Pattern SECS_PATTERN  = Pattern.compile("(\\d+)s");

    private BossTracker() {}

    public static void onWorldJoin() {
        worldJoinMs = Util.getMeasuringTimeMs();
        hintSent = false;
        lastEntityScanMs = 0;
        lastStaleTickMs = 0;
        lastSpawnTickMs = 0;
        lastBossaryParseMs = 0;
        pendingAutoClose = false;
        pendingReopenMenu = false;
        pendingBossaryOpen = false;
        bosses.clear();
    }

    public static void reset() {
        bosses.clear();
        lastBossaryParseMs = 0;
        lastEntityScanMs = 0;
        lastStaleTickMs = 0;
        lastSpawnTickMs = 0;
        worldJoinMs = 0;
        hintSent = false;
        pendingDefeatBoss = null;
        pendingSpawnEvent = false;
        pendingAutoClose = false;
        pendingReopenMenu = false;
        pendingBossaryOpen = false;
    }

    public static void tick(MinecraftClient client) {
        if (!bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return;
        if (WorldIdentification.world != PlayerWorld.ISLE) return;

        // Reopen /ip menu after bossary auto-close (deferred to avoid screen conflicts)
        if (pendingReopenMenu && !pendingAutoClose && client.currentScreen == null) {
            pendingReopenMenu = false;
            client.setScreen(new com.islesplus.screen.islesscreen.IslesScreen());
        }

        long now = Util.getMeasuringTimeMs();

        // 3 seconds after join: auto-open /bossary or send hint
        if (!hintSent && worldJoinMs > 0 && now - worldJoinMs > 3000) {
            hintSent = true;
            if (client.player != null) {
                if (autoOpenBossary) {
                    pendingBossaryOpen = true;
                } else {
                    com.islesplus.IslesClient.sendInfoMessage(client, "Open /bossary to start tracking world boss timers");
                }
            }
        }

        // Deferred /bossary open (e.g. after toggling feature on from settings screen)
        if (pendingBossaryOpen && client.player != null && client.currentScreen == null) {
            pendingBossaryOpen = false;
            pendingAutoClose = true;
            BossaryHook.expectBossary();
            client.player.networkHandler.sendChatCommand("bossary");
        }

        // COOLDOWN bosses tick down every 5 seconds
        if (lastStaleTickMs == 0) {
            lastStaleTickMs = now;
        } else if (now - lastStaleTickMs >= 5_000L) {
            for (TrackedBoss boss : bosses) {
                if (boss.state != BossState.COOLDOWN) continue;
                if (!isStale(boss)) continue; // entity scan is live, don't double-tick
                boss.remainingMs = Math.max(0, boss.remainingMs - 5_000L);
                if (boss.remainingMs == 0) boss.state = BossState.READY;
            }
            lastStaleTickMs = now;
        }

        // SPAWNING bosses tick down every 1 second
        if (lastSpawnTickMs == 0) {
            lastSpawnTickMs = now;
        } else if (now - lastSpawnTickMs >= 1_000L) {
            long elapsed = now - lastSpawnTickMs;
            for (TrackedBoss boss : bosses) {
                if (boss.state != BossState.SPAWNING) continue;
                boss.remainingMs = Math.max(0, boss.remainingMs - elapsed);
                if (boss.remainingMs == 0) {
                    boss.state = BossState.SPAWNED;
                    boss.spawnedAtMs = now;
                }
            }
            lastSpawnTickMs = now;
        }

        // SPAWNED bosses fall back to estimated cooldown after 5 minutes
        for (TrackedBoss boss : bosses) {
            if (boss.state != BossState.SPAWNED) continue;
            if (boss.spawnedAtMs > 0 && now - boss.spawnedAtMs >= SPAWNED_TIMEOUT_MS) {
                boss.state = BossState.COOLDOWN;
                boss.remainingMs = SPAWNED_FALLBACK_MS;
                boss.spawnedAtMs = 0;
            }
        }

        // Scan loaded world entities for live beacon timers
        scanEntities(client);

        // Expire pending multi-line matches
        if (pendingDefeatBoss != null && now > pendingDefeatExpiry) {
            pendingDefeatBoss = null;
        }
        if (pendingSpawnEvent && now > pendingSpawnEventExpiry) {
            pendingSpawnEvent = false;
        }
    }

    /** Called when a chat message is received. Pass the plain string. */
    public static void onChatMessage(String msg) {
        if (!bossTrackerEnabled || FeatureFlags.isKilled("boss_tracker")) return;

        String stripped = msg.strip();

        // "WORLD BOSS EVENT!" → 120s spawning timer
        if (stripped.contains("WORLD BOSS EVENT!")) {
            String name = extractBossNameFromDecorated(stripped);
            if (name != null) {
                setBossState(name, BossState.SPAWNING, 120_000L);
            } else {
                pendingSpawnEvent = true;
                pendingSpawnEventExpiry = Util.getMeasuringTimeMs() + 2000;
            }
            return;
        }

        // Pending spawn event — next decorated message with ☠ is the spawning boss
        if (pendingSpawnEvent) {
            String name = extractBossNameFromDecorated(stripped);
            if (name != null) {
                setBossState(name, BossState.SPAWNING, 120_000L);
                pendingSpawnEvent = false;
            }
            return;
        }

        // "WORLD BOSS DEFEATED!"
        if (stripped.contains("WORLD BOSS DEFEATED!")) {
            // Look for ☠ BossName ☠ in same message
            String name = extractBossNameFromDecorated(stripped);
            if (name != null) {
                defeatBoss(name);
            } else {
                // Watch next message for boss name
                pendingDefeatBoss = "__pending__";
                pendingDefeatExpiry = Util.getMeasuringTimeMs() + 2000;
            }
            return;
        }

        // Pending defeat — next decorated message with ☠ is the defeated boss
        if (pendingDefeatBoss != null && pendingDefeatBoss.equals("__pending__")) {
            String name = extractBossNameFromDecorated(stripped);
            if (name != null) {
                defeatBoss(name);
                pendingDefeatBoss = null;
            }
            return;
        }

        // "The [Boss] is ready to be spawned!"
        Matcher readyM = READY_PATTERN.matcher(stripped);
        if (readyM.find()) {
            setBossState(readyM.group(1).trim(), BossState.READY, 0);
            return;
        }

        // "about to spawn in 120 seconds" — extract name from same message
        if (stripped.contains("about to spawn in 120 seconds")) {
            String name = extractBossNameFromDecorated(stripped);
            if (name != null) {
                setBossState(name, BossState.SPAWNING, 120_000L);
            }
            return;
        }
    }

    /** Extract boss name from a message containing ☠ BossName ☠ */
    private static String extractBossNameFromDecorated(String msg) {
        Matcher m = SKULL_PATTERN.matcher(msg);
        while (m.find()) {
            String candidate = m.group(1).trim();
            if (!candidate.equalsIgnoreCase("WORLD BOSS")) return candidate;
        }
        return null;
    }

    private static void defeatBoss(String name) {
        TrackedBoss boss = findBoss(name);
        if (boss != null) {
            boss.remainingMs = COOLDOWN_MS;
            boss.state = BossState.COOLDOWN;
        }
    }

    private static void setBossState(String name, BossState state, long remainingMs) {
        TrackedBoss boss = findBoss(name);
        if (boss != null) {
            boss.state = state;
            boss.remainingMs = remainingMs;
        }
    }

    private static TrackedBoss findBoss(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (TrackedBoss b : bosses) {
            if (b.name.toLowerCase(Locale.ROOT).equals(lower)) {
                return b;
            }
        }
        return null;
    }

    /** Called when the /bossary inventory screen is detected. Parse all items. */
    public static void parseBossaryItems(List<ItemStack> items) {
        List<TrackedBoss> parsed = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString().strip();
            if (name.isEmpty() || name.equals("Previous Page") || name.equals("Next Page")) continue;
            // Must have POSSIBLE DROPS in lore to be a boss entry
            net.minecraft.component.type.LoreComponent lore = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (lore == null) continue;
            boolean hasPossibleDrops = false;
            long remainingMs = -1;
            for (net.minecraft.text.Text line : lore.lines()) {
                String l = line.getString();
                if (l.contains("POSSIBLE DROPS:")) hasPossibleDrops = true;
                if (l.contains("Next spawn in:")) {
                    remainingMs = parseTimeString(l);
                }
            }
            if (!hasPossibleDrops) continue;
            BossState state;
            if (remainingMs < 0) {
                state = BossState.UNKNOWN;
                remainingMs = 0;
            } else if (remainingMs == 0) {
                state = BossState.READY;
            } else {
                state = BossState.COOLDOWN;
            }
            TrackedBoss existing = findBoss(name);
            if (existing != null && state == BossState.UNKNOWN) {
                // "Available now!" — keep whatever richer state we already have from entity/chat
                parsed.add(existing);
            } else if (existing != null && (existing.state == BossState.SPAWNING || existing.state == BossState.SPAWNED || existing.state == BossState.READY)) {
                parsed.add(existing);
            } else {
                TrackedBoss updated = new TrackedBoss(name, remainingMs, state);
                if (existing != null) updated.lastEntityMs = existing.lastEntityMs;
                parsed.add(updated);
            }
        }
        if (!parsed.isEmpty()) {
            bosses.clear();
            bosses.addAll(parsed);
            lastBossaryParseMs = Util.getMeasuringTimeMs();
            lastStaleTickMs = Util.getMeasuringTimeMs();
        }
    }

    /** Parse "» Next spawn in: 1h 58m 10s" → milliseconds */
    private static long parseTimeString(String line) {
        long ms = 0;
        Matcher h = HOURS_PATTERN.matcher(line);
        Matcher m = MINS_PATTERN.matcher(line);
        Matcher s = SECS_PATTERN.matcher(line);
        if (h.find()) ms += Long.parseLong(h.group(1)) * 3600_000L;
        if (m.find()) ms += Long.parseLong(m.group(1)) * 60_000L;
        if (s.find()) ms += Long.parseLong(s.group(1)) * 1_000L;
        return ms;
    }

    /** Per-boss stale check: fresh only if entity scan is actively seeing the beacon. */
    public static boolean isStale(TrackedBoss boss) {
        long now = Util.getMeasuringTimeMs();
        if (boss.lastEntityMs > 0 && now - boss.lastEntityMs < 3_000L) return false;
        return true;
    }

    private static void scanEntities(MinecraftClient client) {
        if (client.world == null || client.player == null || bosses.isEmpty()) return;
        long now = Util.getMeasuringTimeMs();
        if (now - lastEntityScanMs < 1_000L) return;
        lastEntityScanMs = now;

        net.minecraft.util.math.Vec3d pos = new net.minecraft.util.math.Vec3d(
            client.player.getX(), client.player.getY(), client.player.getZ());
        net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(pos, pos).expand(128);
        List<net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity> displays =
            client.world.getEntitiesByType(
                net.minecraft.entity.EntityType.TEXT_DISPLAY, box,
                e -> e.getText().getString().contains("WORLD BOSS")
            );

        for (net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity display : displays) {
            String text = display.getText().getString();
            String[] lines = text.split("\n");
            if (lines.length < 3) continue;
            String bossName = lines[1].strip();
            String timeLine = lines[2].strip();
            TrackedBoss boss = findBoss(bossName);
            if (boss == null) continue;
            // SPAWNING and READY are chat-driven — entity scan must not override them
            if (boss.state == BossState.SPAWNING || boss.state == BossState.READY) continue;
            boss.lastEntityMs = now;
            if (timeLine.toLowerCase().contains("available")) {
                boss.state = BossState.UNKNOWN;
                boss.remainingMs = 0;
            } else {
                long ms = parseTimeString(timeLine);
                if (ms > 0) {
                    boss.remainingMs = ms;
                    boss.state = BossState.COOLDOWN;
                }
            }
        }
    }

    /** Format milliseconds as "1h 23m" or "45s" */
    public static String formatTime(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long h = s / 3600; s %= 3600;
        long m = s / 60;   s %= 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
