package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesPlusConfig;
import com.islesplus.ui.ColorMath;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.BrightnessRail;
import com.islesplus.ui.widgets.HexField;
import com.islesplus.ui.widgets.HueRail;
import com.islesplus.ui.widgets.Label;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable drawer body for a row that carries a customizable glow colour (hue/saturation/
 * lightness triple): a "Color" label, hue rail and live swatch on one line, and a "Bright"
 * label, brightness rail and hex field on the next.
 *
 * <p>Dragging the hue or brightness rail leaves saturation untouched, and the brightness rail
 * previews its strip at the stored saturation, so a grey colour shows a grey ramp. Committing a typed hex
 * sets saturation and lightness unconditionally, but only overwrites hue when the parsed colour
 * isn't a grey (saturation 0), otherwise typing a grey hex would jump the hue knob to red.
 * Every change persists via {@link IslesPlusConfig#save()} on rail release or hex commit, never
 * on intermediate drag ticks.
 */
public final class GlowColorDrawer {
    private static final int GAP = 6;

    private GlowColorDrawer() {}

    public static Widget of(Supplier<Float> hue, Consumer<Float> setHue,
                             Supplier<Float> sat, Consumer<Float> setSat,
                             Supplier<Float> light, Consumer<Float> setLight) {
        Supplier<Integer> getRgb = () -> ColorMath.hslToRgb(hue.get(), sat.get(), light.get());
        Consumer<Integer> setRgb = rgb -> {
            float[] hsl = ColorMath.rgbToHsl(rgb);
            if (hsl[1] > 0f) setHue.accept(hsl[0]);
            setSat.accept(hsl[1]);
            setLight.accept(hsl[2]);
            IslesPlusConfig.save();
        };

        HueRail hueRail = new HueRail(hue, setHue, IslesPlusConfig::save);
        BrightnessRail brightnessRail = new BrightnessRail(hue, sat, light, setLight, IslesPlusConfig::save);
        Widget swatch = new Swatch(getRgb);

        return new Flow.Column(GAP)
            .add(new Flow.WrapRow(GAP, GAP)
                .add(new Label("Color", Theme.TEXT_LABEL, Fonts.SMALL).fixed(Fonts.labelColumn(Fonts.SMALL, "Color", "Bright")))
                .add(hueRail)
                .add(swatch))
            .add(new Flow.WrapRow(GAP, GAP)
                .add(new Label("Bright", Theme.TEXT_LABEL, Fonts.SMALL).fixed(Fonts.labelColumn(Fonts.SMALL, "Color", "Bright")))
                .add(brightnessRail)
                .add(new HexField(getRgb, setRgb)));
    }

    /** Tiny swatch showing the current colour, ringed in {@link Theme#INK_DEEP}. */
    private static final class Swatch extends Widget {
        private final Supplier<Integer> getRgb;

        Swatch(Supplier<Integer> getRgb) {
            this.getRgb = getRgb;
        }

        /** Same width as the hex field below it, so the hue and brightness rails end up equally long. */
        @Override public int prefWidth() { return HexField.fieldWidth(); }

        @Override public int layout(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.w = HexField.fieldWidth();
            this.h = Metrics.RAIL_H;
            return this.h;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY) {
            int rgb = getRgb.get() | 0xFF000000;
            ctx.fill(x, y, x + w, y + h, rgb);
            Draw.ring(ctx, x, y, w, h, Theme.INK_DEEP);
        }
    }
}
