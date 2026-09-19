package com.islesplus.features.rankcalculator;

import java.util.regex.Matcher;

/**
 * int parsing for scoreboard regex captures that doesn't crash the game.
 *
 * the server sometimes puts garbage on the sidebar for a tick, like a 15 digit "minutes"
 * value right after you enter a rift. our patterns only match digits so the only way this
 * fails is overflowing an int. instead of crashing the tick we hand back {@code null} and
 * the caller just skips that line and keeps last tick's numbers
 */
public final class ScoreboardNumbers {
    private ScoreboardNumbers() {}

    /**
     * parses every capture group of a match as an int.
     * returns them in order, or {@code null} if any of them don't fit
     */
    public static int[] parseGroups(Matcher m) {
        int count = m.groupCount();
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            try {
                out[i] = Integer.parseInt(m.group(i + 1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }
}
