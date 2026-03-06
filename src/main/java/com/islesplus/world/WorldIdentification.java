package com.islesplus.world;

import com.islesplus.IslesClient;
import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.sync.RefreshPoller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Team;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class WorldIdentification {
    public static volatile PlayerWorld world = PlayerWorld.OTHER;
    /** Lowercase rift name matched from the scoreboard, empty for unknown/unmatched rifts. */
    public static String currentRiftName = "";
    private static boolean pendingCheck = false;
    private static int checkTicksRemaining = 0;
    private static boolean pendingWelcome = false;
    /** True once we have seen the "Dungeon" server type on the scoreboard this session. */
    private static boolean seenDungeonServer = false;
    private static final int CHECK_TIMEOUT = 60; // retry for 3 seconds (60 ticks)

    /** Matches server instance lines like "Isles01", "Isles13", etc. (not "skyblockisles.net"). */
    private static final Pattern ISLES_WORLD = Pattern.compile("(?i)\\bIsles\\d+\\b");
    /** Matches rift server name containing "Dungeon" or "Dungeons". */
    private static final Pattern RIFT_WORLD = Pattern.compile("(?i)\\bDungeons?");
    /** Strips Minecraft color/format codes (§ + any char). */
    private static final Pattern STRIP_COLOR = Pattern.compile("§.");

    private WorldIdentification() {}

    /** Call on world join to begin world-type detection. */
    public static void onJoin() {
        pendingCheck = true;
        pendingWelcome = true;
        checkTicksRemaining = CHECK_TIMEOUT;
        seenDungeonServer = false;
        currentRiftName = "";
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) return;

        if (pendingCheck) {
            if (checkTicksRemaining > 0) {
                checkTicksRemaining--;
                tryDetectWorldType(client);
            } else {
                pendingCheck = false;
                // Timed out: if we saw a dungeon server but no rift name matched (unknown rift,
                // not yet in our list), default to enabling dungeon features with minute-rounding
                // fallback for timing. This handles new dungeons before a /ip refresh.
                if (seenDungeonServer && world != PlayerWorld.RIFT && world != PlayerWorld.DISABLED_RIFT) {
                    world = PlayerWorld.RIFT;
                    currentRiftName = "";
                }
                trySendWelcome(client);
            }
        }
    }

    private static void tryDetectWorldType(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        boolean foundDungeon = false;
        boolean foundIsles = false;
        String matchedRiftName = null;
        String matchedDisabledName = null;
        boolean isDisabled = false;

        Map<String, Integer> knownRifts = RiftRepository.getRiftDurations();
        Set<String> disabledRifts = RiftRepository.getDisabledRifts();

        for (ScoreHolder holder : scoreboard.getKnownScoreHolders()) {
            if (scoreboard.getScore(holder, sidebar) == null) continue;
            String name   = holder.getNameForScoreboard();
            Team   team   = scoreboard.getScoreHolderTeam(name);
            String prefix = team != null ? team.getPrefix().getString() : "";
            String suffix = team != null ? team.getSuffix().getString() : "";
            String line   = STRIP_COLOR.matcher(prefix + name + suffix).replaceAll("").trim();
            String lineLower = line.toLowerCase(Locale.ROOT);

            if (RIFT_WORLD.matcher(line).find()) foundDungeon = true;
            if (ISLES_WORLD.matcher(line).find()) foundIsles = true;

            if (matchedRiftName == null) {
                for (String riftName : knownRifts.keySet()) {
                    if (lineLower.contains(riftName)) {
                        matchedRiftName = riftName;
                        break;
                    }
                }
            }

            if (!isDisabled) {
                for (String disabled : disabledRifts) {
                    if (lineLower.contains(disabled)) {
                        isDisabled = true;
                        matchedDisabledName = disabled;
                        break;
                    }
                }
            }
        }

        if (foundDungeon) seenDungeonServer = true;

        // No data loaded yet (first launch, no network) - fall back to dungeon-only detection.
        if (foundDungeon && knownRifts.isEmpty() && disabledRifts.isEmpty()) {
            world = PlayerWorld.RIFT;
            currentRiftName = "";
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // Explicitly disabled rift - dungeon features off, but plushie_rifts can still activate.
        if (foundDungeon && isDisabled) {
            world = PlayerWorld.DISABLED_RIFT;
            currentRiftName = matchedRiftName != null ? matchedRiftName : (matchedDisabledName != null ? matchedDisabledName : "");
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // Known rift matched - full dungeon features with preset duration.
        if (foundDungeon && matchedRiftName != null) {
            world = PlayerWorld.RIFT;
            currentRiftName = matchedRiftName;
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // foundDungeon but no name matched yet - keep retrying. On timeout, seenDungeonServer
        // will trigger RIFT with empty currentRiftName (unknown rift fallback).

        if (foundIsles) {
            world = PlayerWorld.ISLE;
            currentRiftName = "";
            pendingCheck = false;
            trySendWelcome(client);
        }
    }

    private static void trySendWelcome(MinecraftClient client) {
        if (!pendingWelcome) return;
        pendingWelcome = false;
        if (world == PlayerWorld.ISLE || world == PlayerWorld.OTHER) {
            RefreshPoller.start();
        }
        if (world == PlayerWorld.OTHER && client.player != null) {
            IslesClient.sendWelcomeMessage(client);
        }
        IslesClient.sendStatusMessage(client, buildDetectionMessage());
    }

    private static String buildDetectionMessage() {
        return switch (world) {
            case ISLE -> "Detected: ISLE";
            case RIFT -> currentRiftName.isEmpty()
                ? "Detected: RIFT (unknown rift)"
                : "Detected: RIFT (" + currentRiftName + ")";
            case DISABLED_RIFT -> {
                boolean hasPlushie = RiftRepository.getPlushieRifts().contains(currentRiftName);
                String name = currentRiftName.isEmpty() ? "unknown rift" : currentRiftName;
                yield "Detected: DISABLED_RIFT (" + name + ") - " + (hasPlushie ? "plushie only" : "all features off");
            }
            case OTHER -> "Detected: OTHER";
        };
    }

    public static void reset() {
        world = PlayerWorld.OTHER;
        currentRiftName = "";
        pendingCheck = false;
        pendingWelcome = false;
        checkTicksRemaining = 0;
        seenDungeonServer = false;
    }
}
