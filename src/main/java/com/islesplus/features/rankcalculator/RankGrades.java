package com.islesplus.features.rankcalculator;

/** Grade thresholds and the multiplier that scales them. No Minecraft classes, so it is unit-testable. */
final class RankGrades {
    // Grade thresholds (descending) and corresponding labels
    static final double[] THRESHOLDS = { 0.900, 0.775, 0.650, 0.525, 0.400, 0.275 };
    static final String[] GRADES     = { "S",   "A",   "B",   "C",   "D",   "E"   };

    private RankGrades() {}

    /** Threshold multiplier: the player-count discount, further scaled by the active event. */
    static double multiplier(int playerCount, ActiveEvent event) {
        return Math.min(1.0, 0.75 + (1.0 / 12.0) * playerCount) * event.thresholdScale();
    }

    static String grade(double rawScore, double m) {
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (rawScore > THRESHOLDS[i] * m) return GRADES[i];
        }
        return "F";
    }
}
