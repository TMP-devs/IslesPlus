package com.islesplus.features.rankcalculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reference Desmos graph of the server's rating (score weight 4, time weight 1, solo, no
 * event). At zero time left its bands start at score fractions of roughly 0.285, 0.415, 0.545,
 * 0.675, 0.805 and 0.935, and its example run (219/274 points, 418 s left of 840 s) sits in the
 * A band just short of S.
 */
class DesmosReferenceTest {
    private static final double SOLO = RankGrades.multiplier(1, ActiveEvent.NONE);

    private static double raw(double scoreFraction, double timeFraction) {
        return (scoreFraction * 4.0 + timeFraction) / 5.0;
    }

    @Test void bandEdgesAtZeroTimeLeftMatchTheGraph() {
        double[] edges = {0.935, 0.805, 0.675, 0.545, 0.415, 0.285};   // S, A, B, C, D, E
        String[] bands = {"S", "A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < edges.length; i++) {
            assertEquals(bands[i], RankGrades.grade(raw(edges[i] + 0.005, 0), SOLO));
            assertEquals(bands[i + 1], RankGrades.grade(raw(edges[i] - 0.005, 0), SOLO));
        }
    }

    @Test void exampleRatingIsAnAWithoutTheEvent() {
        assertEquals("A", RankGrades.grade(raw(219.0 / 274.0, 418.0 / 840.0), SOLO));
    }

    @Test void exampleRatingIsAnSDuringRiftSpelunker() {
        assertEquals("S", RankGrades.grade(raw(219.0 / 274.0, 418.0 / 840.0),
            RankGrades.multiplier(1, ActiveEvent.RIFT_SPELUNKER)));
    }
}
