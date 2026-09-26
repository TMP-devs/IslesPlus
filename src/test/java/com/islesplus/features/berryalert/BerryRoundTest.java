package com.islesplus.features.berryalert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BerryRoundTest {

    // ---- rounds --------------------------------------------------------------------------------

    @Test void firstBerryStartsARound() {
        BerryRound round = new BerryRound();
        assertTrue(round.update(true, 1_000));
        assertTrue(round.active());
    }

    @Test void aBerryRespawningMidRoundDoesNotStartAnother() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        // every 5 clicks the berry is replaced: a moment with none, then a new one
        assertFalse(round.update(false, 1_300));
        assertFalse(round.update(true, 1_500));
        assertTrue(round.active());
    }

    @Test void roundEndsOnceNoBerryHasBeenSeenForASecond() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        round.update(false, 1_900);
        assertTrue(round.active());
        round.update(false, 2_000);
        assertFalse(round.active());
    }

    @Test void aBerryAfterTheRoundEndedStartsANewOne() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        round.update(false, 2_500);
        assertTrue(round.update(true, 9_000));
    }

    @Test void noBerryNoRound() {
        BerryRound round = new BerryRound();
        assertFalse(round.update(false, 1_000));
        assertFalse(round.active());
    }

    @Test void resetEndsTheRound() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        round.reset();
        assertFalse(round.active());
    }

    // ---- the title ---------------------------------------------------------------------------

    @Test void titleShowsForThreeSecondsFromTheFirstBerry() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        assertTrue(round.titleVisible(1_000));
        assertTrue(round.titleVisible(3_999));
        assertFalse(round.titleVisible(4_000));
    }

    @Test void aBerryRespawningMidRoundDoesNotBringTheTitleBack() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        round.update(true, 3_400);    // clicking away at the first berry
        round.update(false, 3_500);   // picked: a moment with none...
        round.update(true, 3_800);    // ...and the next berry, same round
        assertFalse(round.titleVisible(4_500));
    }

    @Test void aNewRoundShowsTheTitleAgain() {
        BerryRound round = new BerryRound();
        round.update(true, 1_000);
        round.update(false, 5_000);   // round over
        round.update(true, 9_000);
        assertTrue(round.titleVisible(9_500));
    }

    @Test void noRoundNoTitle() {
        assertFalse(new BerryRound().titleVisible(1_000));
    }
}
