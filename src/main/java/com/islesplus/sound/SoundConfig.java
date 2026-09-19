package com.islesplus.sound;

/** one configurable sound: which one, how loud, what pitch */
public final class SoundConfig {
    public String soundId;
    public float volume;
    public float pitch;

    public SoundConfig(String soundId, float volume, float pitch) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SoundConfig copy() {
        return new SoundConfig(soundId, volume, pitch);
    }

    public void copyFrom(SoundConfig other) {
        this.soundId = other.soundId;
        this.volume  = other.volume;
        this.pitch   = other.pitch;
    }
}
