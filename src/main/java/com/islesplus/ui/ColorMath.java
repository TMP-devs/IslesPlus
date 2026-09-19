package com.islesplus.ui;
import java.util.Locale;
public final class ColorMath {
    private ColorMath() {}
    public static int hslToRgb(float h, float s, float l) {
        h = ((h % 1f) + 1f) % 1f; s = clamp(s); l = clamp(l);
        float c = (1 - Math.abs(2 * l - 1)) * s, hp = h * 6f, x = c * (1 - Math.abs(hp % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; } else if (hp < 2) { r = x; g = c; } else if (hp < 3) { g = c; b = x; }
        else if (hp < 4) { g = x; b = c; } else if (hp < 5) { r = x; b = c; } else { r = c; b = x; }
        float m = l - c / 2;
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
    public static float[] rgbToHsl(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min, h = 0;
        if (d != 0) {
            if (max == r) h = ((g - b) / d) % 6; else if (max == g) h = (b - r) / d + 2; else h = (r - g) / d + 4;
            h = ((h * 60 + 360) % 360) / 360f;
        }
        float l = (max + min) / 2, s = d == 0 ? 0 : d / (1 - Math.abs(2 * l - 1));
        return new float[]{h, clamp(s), l};
    }
    /** The colour at {@code factor} of its brightness (0 = black, 1 = unchanged); returns RGB. */
    public static int darken(int rgb, float factor) {
        int r = Math.round(((rgb >> 16) & 0xFF) * factor), g = Math.round(((rgb >> 8) & 0xFF) * factor), b = Math.round((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    public static String toHex(int rgb) { return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF); }
    public static Integer parseHex(String s) {
        if (s == null) return null;
        String t = s.trim(); if (t.startsWith("#")) t = t.substring(1);
        if (!t.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(t, 16);
    }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
