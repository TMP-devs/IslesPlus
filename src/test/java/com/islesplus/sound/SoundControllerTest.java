package com.islesplus.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundControllerTest {
    @Test void filterOffAllowsEverything() {
        assertTrue(SoundController.allow(false, false, false));
    }

    @Test void filterOnMutesOtherSounds() {
        assertFalse(SoundController.allow(true, false, false));
    }

    @Test void filterOnKeepsOurSoundsAndMenuClicks() {
        assertTrue(SoundController.allow(true, true, false));
        assertTrue(SoundController.allow(true, false, true));
    }

    @Test void ownSoundFlagIsOffOutsideAPlay() {
        assertFalse(ModSounds.isPlayingOwn());
    }
}
