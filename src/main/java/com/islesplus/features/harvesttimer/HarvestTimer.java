package com.islesplus.features.harvesttimer;

import com.islesplus.features.nodealertmanager.NodeTracker;
import com.islesplus.entity.TrackedNode;
import com.islesplus.sync.FeatureFlags;
import net.minecraft.util.Util;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HarvestTimer {
    private HarvestTimer() {}

    private static final Pattern GATHERING_INTERVAL_PATTERN =
        Pattern.compile("NERD MODE . Gathering interval is (\\d+) ticks\\.");

    public static boolean harvestTimerEnabled = false;

    // Last parsed gathering interval in ticks (from chat)
    private static int lastGatheringInterval = -1;
    // The node UUID we're currently showing time for
    private static UUID currentNodeUuid = null;
    // Last node count we used for calculation
    private static int lastNodeCount = -1;
    // Total estimated seconds based on current interval and count
    private static double totalTimeSecs = -1;
    // Timestamp (ms) when totalTimeSecs was computed - countdown reference point
    private static long referenceTimeMs = 0;
    // Formatted time string, or null if nothing to show
    private static String formattedTime = null;
    // Text colour (RGB) for the current time: lime when the node is full, through gold, to red
    public static final int LIME = 0x8FD14F, GOLD = 0xF2BC3C, RED = 0xE05341;
    private static int timeColor = LIME;

    /** Harvest Timer mockup: lime while more than half the node's time is left, gold down to a
     * quarter, alert red for the last quarter. {@code fullSecs} is how long the node takes from
     * its fullest count. */
    public static int colorFor(double remainingSecs, double fullSecs) {
        if (fullSecs <= 0) return RED;
        double f = remainingSecs / fullSecs;
        return f > 0.5 ? LIME : f > 0.25 ? GOLD : RED;
    }

    public static void onMessage(String text) {
        if (!harvestTimerEnabled || FeatureFlags.isKilled("harvest_timer")) return;
        Matcher m = GATHERING_INTERVAL_PATTERN.matcher(text);
        if (!m.find()) return;
        try {
            int newInterval = Integer.parseInt(m.group(1));
            if (newInterval != lastGatheringInterval) {
                lastGatheringInterval = newInterval;
                // Recalculate with new interval if we have a count
                if (lastNodeCount >= 0) {
                    totalTimeSecs = (double) lastGatheringInterval * lastNodeCount / 20.0;
                    referenceTimeMs = Util.getMeasuringTimeMs();
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    public static void tick() {
        if (!harvestTimerEnabled || lastGatheringInterval < 0) {
            clearDisplay();
            return;
        }

        if (!NodeTracker.selfActivelyFarmingTrackedNode) {
            clearDisplay();
            totalTimeSecs = -1;
            return;
        }

        TrackedNode node = NodeTracker.trackedNode;
        if (node == null || node.previousCount < 0) {
            clearDisplay();
            return;
        }

        currentNodeUuid = node.textDisplayUuid;
        int count = node.previousCount;

        // When the count changes (node harvested) or first time, recalculate from scratch
        if (count != lastNodeCount) {
            lastNodeCount = count;
            totalTimeSecs = (double) lastGatheringInterval * count / 20.0;
            referenceTimeMs = Util.getMeasuringTimeMs();
        }

        // Count down live based on elapsed time
        long now = Util.getMeasuringTimeMs();
        double elapsed = (now - referenceTimeMs) / 1000.0;
        double remaining = Math.max(0, totalTimeSecs - elapsed);

        int remainingSecsInt = (int) Math.ceil(remaining);
        int minutes = remainingSecsInt / 60;
        int seconds = remainingSecsInt % 60;
        // "~": it is an estimate - other players hitting the node shorten it
        formattedTime = String.format("~%d:%02d", minutes, seconds);
        timeColor = colorFor(remaining, (double) lastGatheringInterval * Math.max(count, node.peakCount) / 20.0);
    }

    private static void clearDisplay() {
        formattedTime = null;
        currentNodeUuid = null;
        lastNodeCount = -1;
    }

    public static String getFormattedTime() {
        return formattedTime;
    }

    /** RGB for the current time, green at a full node down to red as it empties. */
    public static int getTimeColor() {
        return timeColor;
    }

    public static UUID getTrackedNodeUuid() {
        return currentNodeUuid;
    }

    public static void reset() {
        lastGatheringInterval = -1;
        currentNodeUuid = null;
        lastNodeCount = -1;
        totalTimeSecs = -1;
        referenceTimeMs = 0;
        formattedTime = null;
    }
}
