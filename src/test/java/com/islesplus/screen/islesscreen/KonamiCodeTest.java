package com.islesplus.screen.islesscreen;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KonamiCodeTest {
    private static final int U = GLFW.GLFW_KEY_UP, D = GLFW.GLFW_KEY_DOWN, L = GLFW.GLFW_KEY_LEFT,
        R = GLFW.GLFW_KEY_RIGHT, B = GLFW.GLFW_KEY_B, A = GLFW.GLFW_KEY_A;

    private static boolean run(KonamiCode code, int... keys) {
        boolean done = false;
        for (int key : keys) done = code.feed(key);
        return done;
    }

    @Test void theFullSequenceCompletesOnTheLastKey() {
        KonamiCode code = new KonamiCode();
        assertFalse(run(code, U, U, D, D, L, R, L, R, B));
        assertTrue(code.feed(A));
    }

    @Test void aWrongKeyStartsOver() {
        assertFalse(run(new KonamiCode(), U, U, D, D, L, R, L, R, A, B));
        assertFalse(run(new KonamiCode(), U, U, D, L, D, L, R, L, R, B, A));
    }

    @Test void extraUpsAtTheStartStillWork() {
        assertTrue(run(new KonamiCode(), U, U, U, U, D, D, L, R, L, R, B, A));
    }

    @Test void itCanBeEnteredAgainAfterCompleting() {
        KonamiCode code = new KonamiCode();
        assertTrue(run(code, U, U, D, D, L, R, L, R, B, A));
        assertTrue(run(code, U, U, D, D, L, R, L, R, B, A));
    }

    @Test void aFailedRunFollowedByACleanOneWorks() {
        assertTrue(run(new KonamiCode(), U, U, D, B, U, U, D, D, L, R, L, R, B, A));
    }
}
