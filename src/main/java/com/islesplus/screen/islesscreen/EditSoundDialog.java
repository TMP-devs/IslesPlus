package com.islesplus.screen.islesscreen;

import com.islesplus.IslesPlusConfig;
import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.Dialog;
import com.islesplus.ui.widgets.Dropdown;
import com.islesplus.ui.widgets.InfoChip;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.ValueSlider;
import net.minecraft.client.MinecraftClient;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Modal dialog for editing a {@link SoundConfig} in place: a sound picker dropdown, volume and
 * pitch sliders, and test/reset/done buttons. Mutates the live {@code SoundConfig} returned by
 * {@code get.get()} and calls {@code set.accept(cfg)} after every mutation; {@link IslesPlusConfig#save()}
 * runs on dropdown select, on slider release, on RESET and on close — never on every drag tick.
 */
public final class EditSoundDialog {
    private static final int ROW_GAP = 4;

    private EditSoundDialog() {}

    public static void open(OverlayHost host, Supplier<SoundConfig> get, Consumer<SoundConfig> set, SoundConfig defaults) {
        boolean[] tested = {false};
        Widget[] dialogRef = new Widget[1];

        Dropdown dropdown = new Dropdown(host, Dropdown.Style.DARK,
            () -> ModSounds.SOUND_LIST.length,
            i -> shortName(ModSounds.SOUND_LIST[i]),
            null,
            () -> findSoundIdx(get.get().soundId),
            i -> {
                SoundConfig cfg = get.get();
                cfg.soundId = ModSounds.SOUND_LIST[i];
                set.accept(cfg);
                IslesPlusConfig.save();
            });

        ValueSlider volumeSlider = new ValueSlider(
            () -> get.get().volume,
            v -> {
                SoundConfig cfg = get.get();
                cfg.volume = v;
                set.accept(cfg);
            },
            0f, 1f, 0.05f,
            IslesPlusConfig::save);

        ValueSlider pitchSlider = new ValueSlider(
            () -> get.get().pitch,
            v -> {
                SoundConfig cfg = get.get();
                cfg.pitch = v;
                set.accept(cfg);
            },
            0.5f, 2f, 0.05f,
            IslesPlusConfig::save)
            .fill(Theme.PITCH_FILL, Theme.PITCH_LIT, Theme.PITCH_SHADE);

        Button testButton = new Button("TEST SOUND", Button.Kind.SECONDARY, () -> {
            ModSounds.playConfig(MinecraftClient.getInstance(), get.get());
            tested[0] = true;
        }).fill();
        testButton.label(() -> tested[0] ? "TEST AGAIN" : "TEST SOUND");

        Button resetButton = new Button("RESET", Button.Kind.QUIET, () -> {
            SoundConfig cfg = get.get();
            cfg.copyFrom(defaults);
            set.accept(cfg);
            IslesPlusConfig.save();
        }).fill();

        Button doneButton = new Button("DONE", Button.Kind.PRIMARY, () -> {
            IslesPlusConfig.save();
            host.closeOverlay(dialogRef[0]);
        }).fill();

        Widget body = new Flow.Column(7)
            .add(new Label("SOUND", Theme.TEXT_META, Fonts.SMALL))
            .add(dropdown)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("VOLUME", Theme.TEXT_META, Fonts.SMALL).fixed(Fonts.labelColumn(Fonts.SMALL, "VOLUME", "PITCH")))
                .add(volumeSlider)
                .add(new InfoChip(() -> Math.round(get.get().volume * 100) + "%").fixed(Metrics.VALUE_CHIP_W)))
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(new Label("PITCH", Theme.TEXT_META, Fonts.SMALL).fixed(Fonts.labelColumn(Fonts.SMALL, "VOLUME", "PITCH")))
                .add(pitchSlider)
                .add(new InfoChip(() -> String.format(Locale.ROOT, "%.2f", get.get().pitch)).fixed(Metrics.VALUE_CHIP_W)))
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(testButton)
                .add(resetButton)
                .add(doneButton));

        Dialog dialog = new Dialog(host, "EDIT SOUND", body, IslesPlusConfig::save);
        dialogRef[0] = dialog;
        host.openOverlay(dialog);
    }

    /** Strips the namespace and the note_block/block/entity/ui/item category prefix, the way
     * the old sound editor overlay's shortName helper did. Package-visible: reused by later tasks. */
    static String shortName(String id) {
        if (id.startsWith("minecraft:")) id = id.substring("minecraft:".length());
        if (id.startsWith("block.note_block.")) return id.substring("block.note_block.".length());
        if (id.startsWith("block.")) return id.substring("block.".length());
        if (id.startsWith("entity.")) return id.substring("entity.".length());
        if (id.startsWith("ui.")) return id.substring("ui.".length());
        if (id.startsWith("item.")) return id.substring("item.".length());
        return id;
    }

    /** {@code shortName · n% · pitch %.2f}. Public: reused by later tasks, including from the
     * {@code screen.islesscreen.rows} sub-package. */
    public static String summary(SoundConfig c) {
        return shortName(c.soundId) + " · " + Math.round(c.volume * 100) + "% · pitch "
            + String.format(Locale.ROOT, "%.2f", c.pitch);
    }

    /** -1 when the id isn't in {@link ModSounds#SOUND_LIST} (the dropdown then shows nothing selected). */
    private static int findSoundIdx(String soundId) {
        for (int i = 0; i < ModSounds.SOUND_LIST.length; i++) {
            if (ModSounds.SOUND_LIST[i].equalsIgnoreCase(soundId)) return i;
        }
        return -1;
    }
}
