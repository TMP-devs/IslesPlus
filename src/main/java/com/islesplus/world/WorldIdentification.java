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
    /** lowercase rift name we matched off the scoreboard, "" if we couldn't figure it out */
    public static String currentRiftName = "";
    private static boolean pendingCheck = false;
    private static int checkTicksRemaining = 0;
    private static boolean pendingWelcome = false;
    /** flips true once the scoreboard says "Dungeon" at any point this session */
    private static boolean seenDungeonServer = false;
    private static final int CHECK_TIMEOUT = 60; // retry for 3 seconds (60 ticks)

    /** matches "Isles01", "Isles13" etc. but not "skyblockisles.net" */
    private static final Pattern ISLES_WORLD = Pattern.compile("(?i)\\bIsles\\d+\\b");
    /** "Dungeon" or "Dungeons" in the server name = we're in a rift */
    private static final Pattern RIFT_WORLD = Pattern.compile("(?i)\\bDungeons?");
    /** strips § color codes */
    private static final Pattern STRIP_COLOR = Pattern.compile("§.");

    private WorldIdentification() {}

    /** call this on world join, kicks off figuring out what world we're in */
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
                // timed out. if we saw a dungeon server but couldn't match a rift name (new rift
                // not in our list yet) just turn dungeon stuff on anyway and use join time for
                // timing. covers brand new dungeons before anyone runs /ip refresh
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

        Map<String, Double> knownRifts = RiftRepository.getRiftMultipliers();
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

        // no rift data loaded (first launch / no internet), just go off the dungeon check
        if (foundDungeon && knownRifts.isEmpty() && disabledRifts.isEmpty()) {
            world = PlayerWorld.RIFT;
            currentRiftName = "";
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // rift is on the disabled list. dungeon features stay off but plushie_rifts still works
        if (foundDungeon && isDisabled) {
            world = PlayerWorld.DISABLED_RIFT;
            currentRiftName = matchedRiftName != null ? matchedRiftName : (matchedDisabledName != null ? matchedDisabledName : "");
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // known rift, everything on incl. the score multiplier
        if (foundDungeon && matchedRiftName != null) {
            world = PlayerWorld.RIFT;
            currentRiftName = matchedRiftName;
            pendingCheck = false;
            trySendWelcome(client);
            return;
        }

        // it's a dungeon but no name matched yet, keep trying. if we time out seenDungeonServer
        // kicks in and we go RIFT with an empty name

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
