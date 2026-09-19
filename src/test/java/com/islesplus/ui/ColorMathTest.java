package com.islesplus.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ColorMathTest {
    @Test void pureHues() {
        assertEquals(0xFF0000, ColorMath.hslToRgb(0f, 1f, .5f));
        assertEquals(0x00FF00, ColorMath.hslToRgb(1/3f, 1f, .5f));
        assertEquals(0x0000FF, ColorMath.hslToRgb(2/3f, 1f, .5f));
    }
    @Test void lightnessExtremes() {
        assertEquals(0x000000, ColorMath.hslToRgb(.4f, 1f, 0f));
        assertEquals(0xFFFFFF, ColorMath.hslToRgb(.4f, 1f, 1f));
    }
    @Test void mockupSample() { // Button Finder mockup: hue 320, sat 85, light 55 -> #ee2bad
        assertEquals("#EE2BAD", ColorMath.toHex(ColorMath.hslToRgb(320/360f, .85f, .55f)));
    }
    @Test void roundTrip() {
        for (int rgb : new int[]{0x6E3B33, 0xC8B58C, 0x123456, 0xFEDCBA}) {
            float[] hsl = ColorMath.rgbToHsl(rgb);
            int back = ColorMath.hslToRgb(hsl[0], hsl[1], hsl[2]);
            for (int sh = 0; sh <= 16; sh += 8)
                assertTrue(Math.abs(((rgb >> sh) & 255) - ((back >> sh) & 255)) <= 1, Integer.toHexString(rgb));
        }
    }
    @Test void greyHasZeroSaturation() { assertEquals(0f, ColorMath.rgbToHsl(0x808080)[1], 1e-6); }
    @Test void hexParsing() {
        assertEquals(0xEE2BAD, ColorMath.parseHex("#ee2bad"));
        assertEquals(0xEE2BAD, ColorMath.parseHex(" EE2BAD "));
        assertNull(ColorMath.parseHex("#ee2ba"));
        assertNull(ColorMath.parseHex("#gg0000"));
        assertNull(ColorMath.parseHex(null));
    }
}
