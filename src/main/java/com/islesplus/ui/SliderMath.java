package com.islesplus.ui;
public final class SliderMath {
    private SliderMath() {}
    public static float fraction(double mouseX, int trackX, int trackW) {
        if (trackW <= 0) return 0f;
        float f = (float) ((mouseX - trackX) / trackW);
        return Math.max(0f, Math.min(1f, f));
    }
    public static float snap(float value, float min, float max, float step) {
        if (step <= 0f) return value;
        float snapped = Math.round((value - min) / step) * step + min;
        return Math.max(min, Math.min(max, snapped));
    }
    public static float lerp(float f, float min, float max) { return min + (max - min) * f; }
    public static float unlerp(float v, float min, float max) { return (v - min) / (max - min); }
}
