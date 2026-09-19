package com.islesplus.screen.islesscreen;

import com.islesplus.sound.SoundConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditSoundDialogTest {

    @Test void shortNameStripsNamespaceAndNoteBlockPrefix() {
        assertEquals("pling", EditSoundDialog.shortName("minecraft:block.note_block.pling"));
    }

    @Test void shortNameStripsNamespaceAndEntityPrefix() {
        assertEquals("ender_dragon.growl", EditSoundDialog.shortName("minecraft:entity.ender_dragon.growl"));
    }

    @Test void shortNameStripsNamespaceAndBlockPrefix() {
        assertEquals("chest.open", EditSoundDialog.shortName("minecraft:block.chest.open"));
    }

    @Test void shortNameLeavesUnknownPrefixAlone() {
        assertEquals("modid:custom.cue", EditSoundDialog.shortName("modid:custom.cue"));
    }

    @Test void summaryFormatsShortNameVolumeAndPitch() {
        SoundConfig cfg = new SoundConfig("minecraft:block.note_block.pling", 0.9f, 1.0f);
        assertEquals("pling · 90% · pitch 1.00", EditSoundDialog.summary(cfg));
    }

    @Test void summaryRoundsVolumeAndUsesLocaleRootDecimalPoint() {
        SoundConfig cfg = new SoundConfig("minecraft:block.note_block.bass", 0.625f, 0.6f);
        assertEquals("bass · 63% · pitch 0.60", EditSoundDialog.summary(cfg));
    }
}
