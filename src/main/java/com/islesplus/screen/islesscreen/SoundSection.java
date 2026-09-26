package com.islesplus.screen.islesscreen;

import com.islesplus.sound.SoundConfig;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.InfoChip;
import com.islesplus.ui.widgets.Label;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable drawer section for a row that carries a customizable {@link SoundConfig}: a "Sound"
 * label, a live summary chip, and an "Edit sound" button that opens {@link EditSoundDialog}.
 */
final class SoundSection {
    private static final int GAP = 4;

    private SoundSection() {}

    static Widget of(OverlayHost host, Supplier<SoundConfig> get, Consumer<SoundConfig> set, SoundConfig defaults) {
        return new Flow.WrapRow(GAP, GAP)
            .add(new Label("Sound", Theme.TEXT_LABEL, Fonts.BODY))
            .add(new InfoChip(() -> EditSoundDialog.summary(get.get())).large())
            .add(new Button("Edit sound", Button.Kind.PRIMARY, () -> EditSoundDialog.open(host, get, set, defaults)));
    }
}
