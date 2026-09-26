package com.islesplus.features.rankcalculator;

import com.islesplus.IslesClient;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.entity.EntityScanResult;
import com.islesplus.sync.FeatureFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Util;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RankCalculator {
    public static boolean rankCalculatorEnabled = true;
    public static boolean showPlayerCount = false;
    public static boolean showRankDropTimer = false;

    // Player count - tracks max seen, never decreases
    public static int playerCount = 1;

    // Scoreboard values
    private static int totalTimeSecs = 0;
    private static int currentTimeSecs = 0;
    private static int currentPoints = 0;
    private static int totalPoints = 0;
    public static int currentKills = 0;
    public static int totalKills = 0;
    public static int currentChests = 0;
    public static int totalChests = 0;
    public static int currentBoss = 0;
    public static int totalBoss = 0;

    // Output
    public static String lastRank = "--";

    // Scoreboard regex patterns
    private static final Pattern RIFT_TIME = Pattern.compile(
        "Rift Time: (\\d+)m (\\d+)s \\((\\d+)/(\\d+)\\)");
    private static final Pattern KILLS  = Pattern.compile("\u2620\\s*(\\d+)/(\\d+)"); // ☠ X/Y
    private static final Pattern CHESTS = Pattern.compile("\uD83C\uDF81\\s*(\\d+)/(\\d+)");
    private static final Pattern BOSS   = Pattern.compile("\uD83D\uDC51\\s*(\\d+)/(\\d+)");

    private static final double[] THRESHOLDS = RankGrades.THRESHOLDS;
    private static final String[] GRADES     = RankGrades.GRADES;

    // Server event, re-read for the whole rift: every tick for the first 5 seconds (the tab list can
    // fill in after the sidebar), then every 5 seconds, so an event that starts or ends mid-rift is
    // picked up. Rift Spelunker lowers the thresholds.
    public static ActiveEvent activeEvent = ActiveEvent.NONE;
    private static final int EVENT_SETTLE_TICKS = 100, EVENT_RECHECK_TICKS = 100;
    private static int settleTicksLeft = 0;
    private static int ticksUntilEventCheck = 0;
    private static boolean spelunkerAnnouncedThisRift = false;

    // Second source: the server's "CURRENT EVENT: ..." chat broadcast. Remembered across worlds
    // (it is usually seen in the hub before entering a rift) and used when the tab list names nothing.
    private static ActiveEvent announcedEvent = ActiveEvent.NONE;
    private static long announcedAtMs = 0L;

    private RankCalculator() {}

    private static boolean wasInRift = false;

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (client.world == null) return;

        // A second rift can start without a new server connection (which is what calls reset()):
        // entering one always re-arms the quick event read and the once-per-rift Spelunker notice.
        boolean inRift = WorldIdentification.world == PlayerWorld.RIFT;
        if (inRift && !wasInRift) {
            settleTicksLeft = EVENT_SETTLE_TICKS;
            ticksUntilEventCheck = 0;
            spelunkerAnnouncedThisRift = false;
        }
        wasInRift = inRift;

        // Track max players seen - never drops down
        if (WorldIdentification.world == PlayerWorld.RIFT) {
            playerCount = Math.max(playerCount, scan.players.size() + 1); // +1 for self
        }

        if (WorldIdentification.world != PlayerWorld.RIFT || !rankCalculatorEnabled) {
            totalTimeSecs = 0;
            return;
        }

        detectActiveEvent(client);
        parseScoreboard(client);
        lastRank = calculateRank();
    }

    /** Every system chat line comes through here so the event broadcast is never missed. */
    public static void onChatMessage(String text) {
        if (FeatureFlags.isKilled("rank_calculator")) return;
        ActiveEvent announced = ActiveEvent.fromAnnouncement(text);
        if (announced == null) return;
        announcedEvent = announced;
        announcedAtMs = Util.getMeasuringTimeMs();
        // Never applied directly: the next check re-resolves, and there the tab list still wins.
        ticksUntilEventCheck = 0;
    }

    private static void detectActiveEvent(MinecraftClient client) {
        if (ticksUntilEventCheck-- > 0) return;
        ActiveEvent fromTab = ActiveEvent.detect(TabListReader.lines(client));
        activeEvent = ActiveEvent.resolve(fromTab, announcedEvent, Util.getMeasuringTimeMs() - announcedAtMs);

        boolean settling = settleTicksLeft > 0 && fromTab == ActiveEvent.NONE;
        if (settleTicksLeft > 0) settleTicksLeft--;
        ticksUntilEventCheck = settling ? 0 : EVENT_RECHECK_TICKS;

        if (activeEvent == ActiveEvent.RIFT_SPELUNKER && !spelunkerAnnouncedThisRift) {
            spelunkerAnnouncedThisRift = true;
            IslesClient.sendAlwaysMessage(client, "Rift Spelunker active - grades need 10% less score.");
        }
    }

    private static void parseScoreboard(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        for (ScoreHolder holder : scoreboard.getKnownScoreHolders()) {
            if (scoreboard.getScore(holder, sidebar) == null) continue;
            String text = getDisplayText(scoreboard, holder);
            // Any capture that overflows an int (server sent a garbage value, e.g. an
            // uninitialized timer right after joining a rift) yields null and the line
            // is skipped, keeping the previous tick's values.
            Matcher m;
            int[] v;
            if ((m = RIFT_TIME.matcher(text)).find()) {
                if ((v = ScoreboardNumbers.parseGroups(m)) == null) continue;
                currentTimeSecs = v[0] * 60 + v[1];
                currentPoints   = v[2];
                totalPoints     = v[3];
                if (totalTimeSecs == 0 && totalPoints > 0) {
                    totalTimeSecs = resolveTotal(client);
                }
            } else {
                // Kills, chests, and boss may all appear on the same scoreboard line
                if ((m = KILLS.matcher(text)).find() && (v = ScoreboardNumbers.parseGroups(m)) != null) {
                    currentKills = v[0];
                    totalKills   = v[1];
                }
                if ((m = CHESTS.matcher(text)).find() && (v = ScoreboardNumbers.parseGroups(m)) != null) {
                    currentChests = v[0];
                    totalChests   = v[1];
                }
                if ((m = BOSS.matcher(text)).find() && (v = ScoreboardNumbers.parseGroups(m)) != null) {
                    currentBoss = v[0];
                    totalBoss   = v[1];
                }
            }
        }
    }

    private static int resolveTotal(MinecraftClient client) {
        String name = WorldIdentification.currentRiftName;
        if (!name.isEmpty()) {
            Double multiplier = RiftRepository.getRiftMultipliers().get(name);
            if (multiplier != null) {
                return (int) Math.round(totalPoints * multiplier);
            }
        }
        IslesClient.sendAlwaysMessage(client, "Unknown rift - score calculation may be slightly off.");
        return currentTimeSecs;
    }

    private static String calculateRank() {
        if (totalTimeSecs == 0 || totalPoints == 0) return "--";
        double s = (double) currentPoints / totalPoints;
        double t = (double) currentTimeSecs / totalTimeSecs;

        double rawScore = (s * 4.0 + t * 1.0) / 5.0;
        return RankGrades.grade(rawScore, RankGrades.multiplier(playerCount, activeEvent));
    }

    public static void logState(MinecraftClient client) {
        if (client.player == null) return;

        double s        = totalPoints   > 0 ? (double) currentPoints   / totalPoints   : 0.0;
        double t        = totalTimeSecs > 0 ? (double) currentTimeSecs / totalTimeSecs : 0.0;
        double rawScore = (s * 4.0 + t * 1.0) / 5.0;
        double m        = RankGrades.multiplier(playerCount, activeEvent);

        send(client, String.format("[RankCalc] players=%d event=%s", playerCount, activeEvent));
        send(client, String.format("[RankCalc] time=%d/%ds  pts=%d/%d  kills=%d/%d  chests=%d/%d  boss=%d/%d",
            currentTimeSecs, totalTimeSecs,
            currentPoints, totalPoints,
            currentKills, totalKills,
            currentChests, totalChests,
            currentBoss, totalBoss));
        send(client, String.format("[RankCalc] s=%.3f t=%.3f rawScore=%.3f M=%.3f -> rank=%s",
            s, t, rawScore, m, lastRank));
    }

    private static void send(MinecraftClient client, String message) {
        client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
    }

    private static String getDisplayText(Scoreboard scoreboard, ScoreHolder holder) {
        String name = holder.getNameForScoreboard();
        Team team = scoreboard.getScoreHolderTeam(name);
        String prefix = team != null ? team.getPrefix().getString() : "";
        String suffix = team != null ? team.getSuffix().getString() : "";
        return (prefix + name + suffix).trim();
    }

    /**
     * at S rank, how many seconds until we drop to A.
     * -1 if not S or if we have enough points that time alone can't demote us
     */
    public static int getSecondsUntilDemotion() {
        if (totalTimeSecs == 0 || totalPoints == 0) return -1;
        if (!"S".equals(lastRank)) return -1;

        double s = (double) currentPoints / totalPoints;
        double m = RankGrades.multiplier(playerCount, activeEvent);

        // S threshold: rawScore > THRESHOLDS[0] * m
        // rawScore = (s * 4.0 + t * 1.0) / 5.0
        // Solve for t where rawScore == threshold:
        //   t = 5.0 * threshold * m - s * 4.0
        double tThreshold = 5.0 * THRESHOLDS[0] * m - s * 4.0;

        if (tThreshold <= 0) return -1; // safe - can't drop even at time 0

        int timeSecsAtThreshold = (int) Math.ceil(tThreshold * totalTimeSecs);
        return Math.max(0, currentTimeSecs - timeSecsAtThreshold);
    }

    /** next rank up, null if already S or no data yet */
    public static String getNextRank() {
        if ("S".equals(lastRank) || "--".equals(lastRank)) return null;
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(lastRank)) return GRADES[i - 1]; // one step up
        }
        return GRADES[GRADES.length - 1]; // F -> E
    }

    /**
     * points needed to hit the next rank.
     * -1 if already S, no data, or it's mathematically not happening at the current time
     */
    public static int getPointsUntilPromotion() {
        if (totalTimeSecs == 0 || totalPoints == 0) return -1;
        if ("S".equals(lastRank) || "--".equals(lastRank)) return -1;

        // Find the threshold index for the next rank
        int thresholdIdx = -1;
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(lastRank)) { thresholdIdx = i - 1; break; }
        }
        if (thresholdIdx < 0) thresholdIdx = THRESHOLDS.length - 1; // F rank -> need to cross E threshold

        double t = (double) currentTimeSecs / totalTimeSecs;
        double m = RankGrades.multiplier(playerCount, activeEvent);

        // rawScore = (s * 4.0 + t) / 5.0 > THRESHOLDS[thresholdIdx] * m
        // => s > (5.0 * threshold * m - t) / 4.0
        double sThreshold = (5.0 * THRESHOLDS[thresholdIdx] * m - t) / 4.0;

        if (sThreshold >= 1.0) return -1; // impossible
        if (sThreshold <= 0) return 0;

        int minPoints = (int) Math.floor(sThreshold * totalPoints) + 1;
        return Math.max(0, minPoints - currentPoints);
    }

    /**
     * for anything below S: seconds until we drop a rank.
     * -1 if it doesn't apply, we're safe, or no data
     */
    public static int getSecondsUntilRankDrop() {
        if (totalTimeSecs == 0 || totalPoints == 0) return -1;
        if ("S".equals(lastRank) || "--".equals(lastRank) || "F".equals(lastRank)) return -1;

        int thresholdIdx = -1;
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(lastRank)) { thresholdIdx = i; break; }
        }
        if (thresholdIdx < 0) return -1;

        double s = (double) currentPoints / totalPoints;
        double m = RankGrades.multiplier(playerCount, activeEvent);

        // Solve for t where rawScore == THRESHOLDS[thresholdIdx] * m
        double tThreshold = 5.0 * THRESHOLDS[thresholdIdx] * m - s * 4.0;
        if (tThreshold <= 0) return -1; // enough points, can't drop

        int timeSecsAtThreshold = (int) Math.ceil(tThreshold * totalTimeSecs);
        return Math.max(0, currentTimeSecs - timeSecsAtThreshold);
    }

    public static int getRankColor() {
        return switch (lastRank) {
            case "S" -> 0xFFD4AF37;
            case "A" -> 0xFF2ECC71;
            case "B" -> 0xFF5BC0DE;
            case "C" -> 0xFFFFFFFF;
            case "D", "E", "F" -> 0xFFE74C3C;
            default  -> 0xFF888888;
        };
    }

    public static void reset() {
        playerCount = 1;
        totalTimeSecs = 0;
        currentTimeSecs = 0;
        currentPoints = 0;
        totalPoints = 0;
        currentKills = 0;
        totalKills = 0;
        currentChests = 0;
        totalChests = 0;
        currentBoss = 0;
        totalBoss = 0;
        lastRank = "--";
        activeEvent = ActiveEvent.NONE;
        settleTicksLeft = EVENT_SETTLE_TICKS;
        ticksUntilEventCheck = 0;
        spelunkerAnnouncedThisRift = false;
        wasInRift = false;
    }
}
