package com.islesplus.ui;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ThemeTest {
    @Test void paletteMatchesStyleGuide() {
        assertEquals(0xFF6E3B33, Theme.OXBLOOD);
        assertEquals(0xFFC8B58C, Theme.SURFACE);
        assertEquals(0xFF3F3931, Theme.WELL);
        assertEquals(0xB80D0B09, Theme.SCRIM); // 72% black-brown
    }
}
