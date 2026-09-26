package com.islesplus.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public final class ModSounds {

    /** hand picked mc sounds you can choose from in the sound editor */
    public static final String[] SOUND_LIST = {
        "minecraft:block.note_block.pling",
        "minecraft:block.note_block.bell",
        "minecraft:block.note_block.bass",
        "minecraft:block.note_block.harp",
        "minecraft:block.note_block.chime",
        "minecraft:block.note_block.flute",
        "minecraft:block.note_block.banjo",
        "minecraft:block.note_block.guitar",
        "minecraft:block.note_block.xylophone",
        "minecraft:block.note_block.iron_xylophone",
        "minecraft:block.note_block.bit",
        "minecraft:block.note_block.didgeridoo",
        "minecraft:block.note_block.cow_bell",
        "minecraft:block.note_block.snare",
        "minecraft:block.note_block.hat",
        "minecraft:block.note_block.basedrum",
        "minecraft:entity.ender_dragon.growl",
        "minecraft:entity.player.levelup",
        "minecraft:entity.experience_orb.pickup",
        "minecraft:block.chest.open",
        "minecraft:ui.button.click",
        "minecraft:block.anvil.use",
        "minecraft:item.armor.equip_generic",
        "minecraft:block.amethyst_block.hit",
        "minecraft:entity.arrow.hit_player",
    };

    public enum Cue {
        SLOT_LOCK("minecraft:block.note_block.bell", 0.55F, 1.20F),
        INVENTORY_FULL("minecraft:block.chest.open", 0.62F, 0.95F),
        DROP_NOTIFY("minecraft:entity.ender_dragon.growl", 0.85F, 1.00F),
        NODE_ALERT("minecraft:block.note_block.bell", 2.00F, 1.00F),
        NODE_DEPLETED("minecraft:block.note_block.bass", 0.85F, 0.60F),
        ANNOUNCEMENT("minecraft:block.note_block.chime", 0.80F, 1.00F),
        MENU_CLICK("minecraft:ui.button.click", 0.20F, 1.25F),
        GROUND_ITEM_PING("minecraft:block.note_block.pling", 0.90F, 1.00F);

        private final String soundId;
        private final float defaultVolume;
        private final float defaultPitch;

        Cue(String soundId, float defaultVolume, float defaultPitch) {
            this.soundId = soundId;
            this.defaultVolume = defaultVolume;
            this.defaultPitch = defaultPitch;
        }
    }

    /** > 0 while one of our own sounds is being started. Starting a sound is synchronous on the
     * client thread (player.playSound -> world.playSoundClient -> SoundManager -> SoundSystem), so
     * the "mod only sounds" filter in SoundSystemMixin can tell our sounds from everything else,
     * even when the server plays the very same sound id. */
    private static int playingOwn = 0;

    public static boolean isPlayingOwn() { return playingOwn > 0; }

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
        playOwn(client, sound, volume, pitch);
    }

    public static void playConfig(MinecraftClient client, SoundConfig config) {
        if (client.player == null || config == null) return;
        try {
            Identifier id = Identifier.of(config.soundId);
            SoundEvent sound = Registries.SOUND_EVENT.get(id);
            if (sound == null) return;
            playOwn(client, sound, config.volume, config.pitch);
        } catch (Exception ignored) {}
    }

    /** Plays on the player, on the Players volume slider, marked as ours for the mod-only filter. */
    private static void playOwn(MinecraftClient client, SoundEvent sound, float volume, float pitch) {
        playingOwn++;
        try {
            client.player.playSound(sound, volume, pitch);
        } finally {
            playingOwn--;
        }
    }

    /** An alert, loud: played straight to the player (not from their position, so nothing
     * muffles it) at the config's volume and pitch. Marked as ours, so mod-only sounds keeps it. */
    public static void playAlert(MinecraftClient client, SoundConfig config) {
        if (client.player == null || config == null) return;
        try {
            SoundEvent sound = Registries.SOUND_EVENT.get(Identifier.of(config.soundId));
            if (sound == null) return;
            playingOwn++;
            try {
                client.getSoundManager().play(PositionedSoundInstance.ui(sound, config.pitch, config.volume));
            } finally {
                playingOwn--;
            }
        } catch (Exception ignored) {}
    }
}
