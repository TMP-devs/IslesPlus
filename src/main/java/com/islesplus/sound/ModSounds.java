package com.islesplus.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class ModSounds {

    /** Curated list of Minecraft sound IDs available in the sound editor. */
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

    /**
     * Sounds the mod will actively play. Rebuilt whenever configs load or save.
     * Always includes the non-configurable cues (slot lock, menu click).
     */
    private static final Set<String> ALLOWED_SOUND_IDS = new java.util.HashSet<>();
    static {
        ALLOWED_SOUND_IDS.add(Cue.SLOT_LOCK.soundId.toLowerCase(Locale.ROOT));
        ALLOWED_SOUND_IDS.add(Cue.MENU_CLICK.soundId.toLowerCase(Locale.ROOT));
    }

    /**
     * Rebuild the allowlist from whatever sounds are currently configured on active features.
     * Call this after loading or saving config.
     */
    public static void rebuildActiveSounds(Iterable<String> activeSoundIds) {
        ALLOWED_SOUND_IDS.clear();
        ALLOWED_SOUND_IDS.add(Cue.SLOT_LOCK.soundId.toLowerCase(Locale.ROOT));
        ALLOWED_SOUND_IDS.add(Cue.MENU_CLICK.soundId.toLowerCase(Locale.ROOT));
        for (String id : activeSoundIds) {
            if (id != null) ALLOWED_SOUND_IDS.add(id.toLowerCase(Locale.ROOT));
        }
    }

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

    public static void playConfig(MinecraftClient client, SoundConfig config) {
        if (client.player == null || config == null) return;
        try {
            Identifier id = Identifier.of(config.soundId);
            SoundEvent sound = Registries.SOUND_EVENT.get(id);
            if (sound == null) return;
            client.player.playSound(sound, config.volume, config.pitch);
        } catch (Exception ignored) {}
    }

    public static boolean isAllowedSoundId(String soundId) {
        if (soundId == null) {
            return false;
        }
        return ALLOWED_SOUND_IDS.contains(soundId.toLowerCase(Locale.ROOT));
    }
}
