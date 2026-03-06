package com.islesplus.sound;

import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;

public final class SoundController {
    private static boolean modOnlySoundsEnabled = false;

    private SoundController() {
    }

    public static void setModOnlySoundsEnabled(boolean enabled) {
        modOnlySoundsEnabled = enabled;
    }

    public static boolean isModOnlySoundsEnabled() {
        return modOnlySoundsEnabled;
    }

    public static boolean shouldMuteIncomingSound(String soundId) {
        return modOnlySoundsEnabled && WorldIdentification.world != PlayerWorld.OTHER && !ModSounds.isAllowedSoundId(soundId);
    }

    public static boolean shouldAllowLocalSound(String soundId) {
        return WorldIdentification.world == PlayerWorld.OTHER || !modOnlySoundsEnabled || ModSounds.isAllowedSoundId(soundId);
    }
}
