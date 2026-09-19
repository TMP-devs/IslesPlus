package com.islesplus.ui;
import net.minecraft.util.math.MathHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GlowColorTest {
    @Test void defaultsReproduceLegacyHsv() {
        for (float h : new float[]{0.917f, 0.128f, 0.092f, 0.617f, 0.333f}) {
            int legacy = MathHelper.hsvToRgb(h, 1f, 1f) & 0xFFFFFF, now = GlowColor.rgb(h, 1f, .5f);
            for (int sh = 0; sh <= 16; sh += 8)
                assertTrue(Math.abs(((legacy >> sh) & 255) - ((now >> sh) & 255)) <= 1);
        }
    }
}
