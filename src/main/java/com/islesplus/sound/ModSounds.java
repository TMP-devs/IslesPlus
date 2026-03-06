package com.islesplus.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ModSounds {
    public enum Cue {
        SLOT_LOCK("minecraft:block.note_block.bell", 0.55F, 1.20F),
        INVENTORY_FULL("minecraft:block.chest.open", 0.62F, 0.95F),
        DROP_NOTIFY("minecraft:entity.ender_dragon.growl", 0.85F, 1.00F),
        NODE_ALERT("minecraft:block.note_block.bell", 2.00F, 1.00F),
        NODE_DEPLETED("minecraft:block.note_block.bass", 0.85F, 0.60F),
        MENU_CLICK("minecraft:ui.button.click", 0.20F, 1.25F);

        private final String soundId;
        private final float defaultVolume;
        private final float defaultPitch;

        Cue(String soundId, float defaultVolume, float defaultPitch) {
            this.soundId = soundId;
            this.defaultVolume = defaultVolume;
            this.defaultPitch = defaultPitch;
        }
    }

    private static final Set<String> ALLOWED_SOUND_IDS = Set.of(Cue.values()).stream()
        .map(cue -> cue.soundId.toLowerCase(Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());

    private ModSounds() {
    }

    public static void play(MinecraftClient client, Cue cue) {
        play(client, cue, cue.defaultVolume, cue.defaultPitch);
    }

    public static void playScaled(MinecraftClient client, Cue cue, float volumeMultiplier) {
        play(client, cue, cue.defaultVolume * volumeMultiplier * 2.0f, cue.defaultPitch);
    }

    public static void play(MinecraftClient client, Cue cue, float volume, float pitch) {
        if (client.player == null) {
            return;
        }
        Identifier id = Identifier.of(cue.soundId);
        SoundEvent sound = Registries.SOUND_EVENT.get(id);
        if (sound == null) return;
        client.player.playSound(sound, volume, pitch);
    }

    public static boolean isAllowedSoundId(String soundId) {
        if (soundId == null) {
            return false;
        }
        return ALLOWED_SOUND_IDS.contains(soundId.toLowerCase(Locale.ROOT));
    }
}
