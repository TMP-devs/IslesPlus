package com.islesplus.world;

import com.islesplus.IslesClient;
import com.islesplus.features.rankcalculator.RiftRepository;
import com.islesplus.logging.IslesLog;
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
    private static final int CHECK_TIMEOUT = 60; // look every tick for 3 seconds (60 ticks)
    /** After that window: keep looking, once a second. While the world is still unknown, because
     * the sidebar can arrive late (a slow server, the server resource pack still loading) and
     * giving up for good left every Isles-only feature off until a relog. And while it is an Isle,
     * because a rift's sidebar can fill in line by line: a Dungeon line that shows up after an
     * Isle was assumed corrects it. */
    private static final int LATE_CHECK_EVERY = 20;
    private static int lateCheckCooldown = 0;
    private static int ticksSinceJoin = 0;
    /** What the last look at the sidebar saw, for the log line when nothing could be made of it. */
    private static String lastSidebarSeen = "";
    /** The connection the welcome was last shown on; weak, so an old connection is not kept alive. */
    private static java.lang.ref.WeakReference<Object> welcomedConnection = new java.lang.ref.WeakReference<>(null);

    // How an Isle is recognised. The server's NAME is deliberately not what decides it: names were
    // once "Isles" + digits only, then "IslesP02" appeared and every Isles-only feature went dark
    // there. Instead:
    //   1. are we connected to the Isles network? The address we joined says so; the sidebar title
    //      is a second opinion for players who joined through another domain or a direct IP.
    //   2. rifts are on the same network, and only the sidebar tells them apart: a "Dungeon" line
    //      means a rift - whenever it shows up, even after an Isle was assumed.
    //   3. the hub every connection starts in is on the network too, with the same title. Only the
    //      sidebar's server line tells it apart ("Hub01   v1.0.0" vs "Isles02   v1.0.0"), so that
    //      name is read - but only to rule the hub OUT.
    //   4. connected, sidebar up, not a Dungeon, not the hub: an Isle, whatever it is called.
    // The old name pattern is kept only for when step 1 cannot tell.

    /** Legacy: "Isles01", "IslesP02"... but not "skyblockisles.net" (no word boundary before its
     * "isles", and no digits after). */
    private static final Pattern ISLES_WORLD = Pattern.compile("(?i)\\bIsles[a-z]{0,3}\\d+\\b");

    static boolean isIslesServerLine(String line) { return ISLES_WORLD.matcher(line).find(); }

    /** Whether we are connected to the Isles network: the address joined, or failing that the
     * sidebar title. Anything that is not a letter or digit is dropped first, so ports, colour
     * codes, spacing and dots ("play.SKYBLOCK ISLES.net") make no difference. */
    static boolean isIslesNetwork(String serverAddress, String sidebarTitle) {
        return squash(serverAddress).contains("skyblockisles") || squash(sidebarTitle).contains("skyblockisles");
    }

    private static String squash(String s) {
        return s == null ? "" : STRIP_COLOR.matcher(s).replaceAll("").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** The sidebar's "<server name>   v<version>" line (the v is optional): group 1 is the name.
     * Seen in play: "Hub01   v1.0.0", "Isles02   v1.0.0", "IslesP02  v1.0.0". */
    private static final Pattern SERVER_LINE = Pattern.compile("^(\\S+)\\s+v?\\d+(?:\\.\\d+)+$");
    /** Servers on the network that are not the game: the hub every connection starts in, and the
     * usual names for the same kind of place. Anything NOT listed here counts as an Isle, so a new
     * game-server name works without a mod update; a new lobby name would only mean Isles features
     * show there until it is added. */
    private static final Pattern NON_GAME_SERVER = Pattern.compile("(?i)^(hub|lobby|limbo|queue|auth|afk)");

    /** The server name off the sidebar's server line, or null if no line is one. */
    static String serverNameOf(java.util.List<String> lines) {
        for (String line : lines) {
            java.util.regex.Matcher m = SERVER_LINE.matcher(line.trim());
            if (m.matches()) return m.group(1);
        }
        return null;
    }

    /**
     * Whether these sidebar lines are an Isle; the caller has already ruled out a Dungeon.
     * @return TRUE an Isle, FALSE definitely not (the hub), null cannot say yet
     * @param lenient after the first few seconds: on the network with a filled-in sidebar but no
     *                readable server line is accepted as an Isle, so a change to that line's format
     *                cannot switch the mod off
     */
    static Boolean isleVerdict(java.util.List<String> lines, boolean onNetwork, boolean lenient) {
        if (lines.isEmpty()) return null;
        String server = serverNameOf(lines);
        if (server != null) {
            if (NON_GAME_SERVER.matcher(server).find()) return Boolean.FALSE;
            if (onNetwork || isIslesServerLine(server)) return Boolean.TRUE;
            return null;   // some other network's server line
        }
        for (String line : lines) if (isIslesServerLine(line)) return Boolean.TRUE;
        return lenient && onNetwork ? Boolean.TRUE : null;
    }
    /** "Dungeon" or "Dungeons" in the server name = we're in a rift. No digits, so a renumbering or
     * a lettered name ("DungeonsG04") still reads as one. */
    private static final Pattern RIFT_WORLD = Pattern.compile("(?i)\\bDungeons?");

    /**
     * Whether these sidebar lines are a rift. The server line decides when there is one, so no
     * other row can vote; only when the sidebar has no readable server line does it fall back to
     * reading every row, which is how it worked before.
     */
    static boolean isRift(java.util.List<String> lines) {
        String server = serverNameOf(lines);
        if (server != null) return RIFT_WORLD.matcher(server).find();
        for (String line : lines) if (RIFT_WORLD.matcher(line).find()) return true;
        return false;
    }
    /** Party rows, "P » CreatorWodash". The only sidebar text a player picks, so it is never read:
     * a party member called "DungeonGuy" would otherwise read as a rift, and "IslesFan01" as a
     * server. */
    private static final Pattern PARTY_LINE = Pattern.compile("^P\\s*»");

    static boolean isPartyLine(String line) { return PARTY_LINE.matcher(line).find(); }
    /** Strips § colour codes. The trailing character is optional because every row on the live
     * sidebar ends in a bare §: with a plain "§." that § survived, and a line ending in it never
     * matched the $-anchored {@link #SERVER_LINE}, so no server name was ever read. */
    private static final Pattern STRIP_COLOR = Pattern.compile("§.?");

    static String stripColor(String s) { return STRIP_COLOR.matcher(s).replaceAll("").trim(); }

    private WorldIdentification() {}

    /** call this on world join, kicks off figuring out what world we're in */
    public static void onJoin() {
        lastSidebarSeen = "";
        pendingCheck = true;
        pendingWelcome = true;
        checkTicksRemaining = CHECK_TIMEOUT;
        seenDungeonServer = false;
        currentRiftName = "";
        lateCheckCooldown = 0;
        ticksSinceJoin = 0;
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) return;
        ticksSinceJoin++;

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
                if (world == PlayerWorld.OTHER) {
                    boolean sidebar = client.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null;
                    IslesLog.runtimeInfo("[Isles+] world not identified in the first 3 s (" + (sidebar ? "sidebar: " + lastSidebarSeen : "no sidebar yet") + "); still looking once a second");
                }
                trySendWelcome(client);
            }
        } else if (--lateCheckCooldown <= 0) {
            lateCheckCooldown = LATE_CHECK_EVERY;
            PlayerWorld before = world;
            tryDetectWorldType(client);
            // a Dungeon whose rift name is not in our list: same rule as at the end of the first window
            if (seenDungeonServer && world != PlayerWorld.RIFT && world != PlayerWorld.DISABLED_RIFT) {
                world = PlayerWorld.RIFT;
                currentRiftName = "";
            }
            if (world != before) {
                IslesLog.runtimeInfo("[Isles+] world identified late, " + ticksSinceJoin + " ticks after joining: " + buildDetectionMessage());
                IslesClient.sendStatusMessage(client, buildDetectionMessage());
            }
        }
    }

    private static void tryDetectWorldType(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        java.util.List<String> lines = new java.util.ArrayList<>();
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
            String line   = stripColor(prefix + name + suffix);
            if (isPartyLine(line)) continue;
            String lineLower = line.toLowerCase(Locale.ROOT);

            if (!line.isEmpty()) lines.add(line);

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

        String serverName = serverNameOf(lines);
        boolean foundDungeon = isRift(lines);
        if (foundDungeon) {
            seenDungeonServer = true;
        } else if (serverName != null) {
            // A server line that is not a Dungeon settles it: we have left the rift, so the sticky
            // flag must not drag us back into RIFT on the next look.
            seenDungeonServer = false;
        }

        String address = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "";
        boolean onNetwork = isIslesNetwork(address, sidebar.getDisplayName().getString());
        // pendingCheck is only false here on the once-a-second look after the first 3 seconds
        Boolean isle = foundDungeon ? Boolean.FALSE : isleVerdict(lines, onNetwork, !pendingCheck);
        boolean foundIsles = Boolean.TRUE.equals(isle);
        boolean notGameServer = !foundDungeon && Boolean.FALSE.equals(isle);
        lastSidebarSeen = "title \"" + sidebar.getDisplayName().getString() + "\", on network " + onNetwork + ", lines " + lines;

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

        // Leaving a rift is only believed on a read server line. A rift's sidebar half-filled, or
        // its line format changed, would otherwise let the lenient rule guess ISLE mid run.
        boolean inRift = world == PlayerWorld.RIFT || world == PlayerWorld.DISABLED_RIFT;
        if (inRift && serverName == null) return;

        if (foundIsles) {
            world = PlayerWorld.ISLE;
            currentRiftName = "";
            pendingCheck = false;
            trySendWelcome(client);
        } else if (notGameServer) {
            // the hub: settled, and it also undoes an Isle assumed from a half-filled sidebar
            world = PlayerWorld.OTHER;
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
        // The welcome + MOTD: once per connection to the server, on arrival. It used to be tied to
        // world OTHER, which only worked because the lobby every connection starts in happened to
        // read as OTHER. Hopping between the lobby, Isles and rifts keeps the same connection
        // object, so it is what says "this is still the same visit".
        Object connection = client.getNetworkHandler() != null ? client.getNetworkHandler().getConnection() : null;
        if (client.player != null && connection != null && welcomedConnection.get() != connection) {
            welcomedConnection = new java.lang.ref.WeakReference<>(connection);
            IslesClient.sendWelcomeMessage(client);
        }
        IslesClient.sendStatusMessage(client, buildDetectionMessage());
        IslesLog.runtimeInfo("[Isles+] " + buildDetectionMessage() + " | sidebar: " + lastSidebarSeen);
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
        lateCheckCooldown = 0;
        ticksSinceJoin = 0;
    }
}
