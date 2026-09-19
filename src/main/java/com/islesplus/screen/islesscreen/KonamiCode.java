package com.islesplus.screen.islesscreen;

import org.lwjgl.glfw.GLFW;

/** Up Up Down Down Left Right Left Right B A. Feed it key codes; it says when the run completes. */
final class KonamiCode {
    private static final int[] SEQUENCE = {
        GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
        GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
        GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A
    };

    private int progress = 0;

    /** True exactly when {@code key} completes the sequence (progress then starts over). */
    boolean feed(int key) {
        if (key == SEQUENCE[progress]) {
            progress++;
        } else if (key == GLFW.GLFW_KEY_UP) {
            // A wrong key that is itself an Up: "up up up down..." must still work, so an extra Up
            // after the opening pair keeps the pair; anywhere else it starts a new run.
            progress = progress == 2 ? 2 : 1;
        } else {
            progress = 0;
        }
        if (progress < SEQUENCE.length) return false;
        progress = 0;
        return true;
    }

    void reset() { progress = 0; }
}
