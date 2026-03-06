package com.islesplus.features.rankcalculator;

import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import com.islesplus.entity.EntityScanResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Team;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RankCalculator {
    public static boolean rankCalculatorEnabled = false;

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

    // Grade thresholds (descending) and corresponding labels
    private static final double[] THRESHOLDS = { 0.900, 0.775, 0.650, 0.525, 0.400, 0.275 };
    private static final String[] GRADES     = { "S",   "A",   "B",   "C",   "D",   "E"   };

    private RankCalculator() {}

    public static void tick(MinecraftClient client, EntityScanResult scan) {
        if (client.world == null) return;

        // Track max players seen - never drops down
        if (WorldIdentification.world == PlayerWorld.RIFT) {
            playerCount = Math.max(playerCount, scan.players.size() + 1); // +1 for self
        }

        if (WorldIdentification.world != PlayerWorld.RIFT || !rankCalculatorEnabled) {
            totalTimeSecs = 0;
            return;
        }

        parseScoreboard(client);
        lastRank = calculateRank();
    }

    private static void parseScoreboard(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        for (ScoreHolder holder : scoreboard.getKnownScoreHolders()) {
            if (scoreboard.getScore(holder, sidebar) == null) continue;
            String text = getDisplayText(scoreboard, holder);
            Matcher m;
            if ((m = RIFT_TIME.matcher(text)).find()) {
                currentTimeSecs = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
                if (totalTimeSecs == 0) {
                    totalTimeSecs = resolveTotal(sidebar, currentTimeSecs);
                }
                currentPoints   = Integer.parseInt(m.group(3));
                totalPoints     = Integer.parseInt(m.group(4));
            } else {
                // Kills, chests, and boss may all appear on the same scoreboard line
                if ((m = KILLS.matcher(text)).find()) {
                    currentKills = Integer.parseInt(m.group(1));
                    totalKills   = Integer.parseInt(m.group(2));
                }
                if ((m = CHESTS.matcher(text)).find()) {
                    currentChests = Integer.parseInt(m.group(1));
                    totalChests   = Integer.parseInt(m.group(2));
                }
                if ((m = BOSS.matcher(text)).find()) {
                    currentBoss = Integer.parseInt(m.group(1));
                    totalBoss   = Integer.parseInt(m.group(2));
                }
            }
        }
    }

    /**
     * Returns the total rift duration in seconds.
     * Checks the sidebar title against the known-rift map first; falls back to
     * rounding the observed timer up to the nearest full minute so that players
     * entering within the same minute share an identical baseline.
     * e.g. unknown rift, join at 9:50 (590 s) → ceil(590/60)*60 = 600 → t = 590/600
     */
    private static int resolveTotal(ScoreboardObjective sidebar, int currentTimeSecs) {
        String title = sidebar.getDisplayName().getString().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : RiftRepository.getRiftDurations().entrySet()) {
            if (title.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return (int) Math.ceil(currentTimeSecs / 60.0) * 60;
    }

    private static String calculateRank() {
        if (totalTimeSecs == 0 || totalPoints == 0) return "--";
        double s = (double) currentPoints / totalPoints;
        double t = (double) currentTimeSecs / totalTimeSecs;

        double rawScore = (s * 4.0 + t * 1.0) / 5.0;
        double m = Math.min(1.0, 0.75 + (1.0 / 12.0) * playerCount);

        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (rawScore > THRESHOLDS[i] * m) return GRADES[i];
        }
        return "F";
    }

    public static void logState(MinecraftClient client) {
        if (client.player == null) return;

        double s        = totalPoints   > 0 ? (double) currentPoints   / totalPoints   : 0.0;
        double t        = totalTimeSecs > 0 ? (double) currentTimeSecs / totalTimeSecs : 0.0;
        double rawScore = (s * 4.0 + t * 1.0) / 5.0;
        double m        = Math.min(1.0, 0.75 + (1.0 / 12.0) * playerCount);

        send(client, String.format("[RankCalc] players=%d", playerCount));
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
     * When rank is S, returns the seconds remaining until score drops to A.
     * Returns -1 if not S, or if enough points that time alone can't demote.
     */
    public static int getSecondsUntilDemotion() {
        if (totalTimeSecs == 0 || totalPoints == 0) return -1;
        if (!"S".equals(lastRank)) return -1;

        double s = (double) currentPoints / totalPoints;
        double m = Math.min(1.0, 0.75 + (1.0 / 12.0) * playerCount);

        // S threshold: rawScore > THRESHOLDS[0] * m
        // rawScore = (s * 4.0 + t * 1.0) / 5.0
        // Solve for t where rawScore == threshold:
        //   t = 5.0 * threshold * m - s * 4.0
        double tThreshold = 5.0 * THRESHOLDS[0] * m - s * 4.0;

        if (tThreshold <= 0) return -1; // safe - can't drop even at time 0

        int timeSecsAtThreshold = (int) Math.ceil(tThreshold * totalTimeSecs);
        return Math.max(0, currentTimeSecs - timeSecsAtThreshold);
    }

    /**
     * When rank is not S, returns the additional points needed to reach S rank.
     * Returns -1 if already S, data not ready, or S is impossible at current time.
     */
    public static int getPointsUntilPromotion() {
        if (totalTimeSecs == 0 || totalPoints == 0) return -1;
        if ("S".equals(lastRank)) return -1;

        double t = (double) currentTimeSecs / totalTimeSecs;
        double m = Math.min(1.0, 0.75 + (1.0 / 12.0) * playerCount);

        // Need rawScore > THRESHOLDS[0] * m
        // (s * 4.0 + t) / 5.0 > 0.9 * m  =>  s > (4.5 * m - t) / 4.0
        double sThreshold = (4.5 * m - t) / 4.0;

        if (sThreshold >= 1.0) return -1; // impossible: even max points can't reach S

        int minPoints = (int) Math.floor(sThreshold * totalPoints) + 1;
        return Math.max(0, minPoints - currentPoints);
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
    }
}
