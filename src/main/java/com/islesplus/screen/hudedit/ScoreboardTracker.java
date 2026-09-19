package com.islesplus.screen.hudedit;

/**
 * where the sidebar scoreboard was drawn last frame.
 * {@code InGameHudScoreboardMixin} updates this every frame
 */
public final class ScoreboardTracker {
    public static boolean valid  = false;
    public static int     x      = 0;
    public static int     y      = 0;
    public static int     width  = 0;
    public static int     height = 0;

    private ScoreboardTracker() {}

    public static void setBounds(int x, int y, int width, int height) {
        ScoreboardTracker.x      = x;
        ScoreboardTracker.y      = y;
        ScoreboardTracker.width  = width;
        ScoreboardTracker.height = height;
        ScoreboardTracker.valid  = true;
    }

    public static void clear() {
        valid = false;
    }
}
