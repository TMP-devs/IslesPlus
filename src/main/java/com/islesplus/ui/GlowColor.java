package com.islesplus.ui;

public final class GlowColor {
    private GlowColor() {}
    public static int rgb(float h, float s, float l) {
        return ColorMath.hslToRgb(h, s, l);
    }
}
