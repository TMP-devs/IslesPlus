package com.islesplus.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SliderMathTest {
    @Test void fractionClamps() { assertEquals(0f, SliderMath.fraction(5, 10, 100)); assertEquals(1f, SliderMath.fraction(500, 10, 100)); assertEquals(.5f, SliderMath.fraction(60, 10, 100), 1e-6); }
    @Test void volumeSnapsToFivePercent() { assertEquals(.85f, SliderMath.snap(.87f, 0f, 1f, .05f), 1e-6); }
    @Test void pitchRange() { assertEquals(1.25f, SliderMath.lerp(.5f, .5f, 2f), 1e-6); assertEquals(.5f, SliderMath.unlerp(1.25f, .5f, 2f), 1e-6); }
}
