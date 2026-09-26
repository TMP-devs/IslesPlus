package com.islesplus.features.quickactions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickActionsLayoutTest {
    private static final int GX = 100, GY = 50;

    @Test void buttonsSitAboveTheOffHandSlotEvenlySpaced() {
        assertEquals(GX + 77, QuickActions.buttonX(GX));
        assertEquals(GY + 9, QuickActions.buttonY(GY, 2));    // button 3, top: 1 px under the helmet slot's top
        assertEquals(GY + 25, QuickActions.buttonY(GY, 1));
        assertEquals(GY + 41, QuickActions.buttonY(GY, 0));   // button 1, bottom
    }

    @Test void hitTestMatchesTheCells() {
        int x = GX + 77;
        assertEquals(0, QuickActions.buttonAt(GX, GY, x + 8, GY + 41 + 8));
        assertEquals(1, QuickActions.buttonAt(GX, GY, x, GY + 25));
        assertEquals(1, QuickActions.buttonAt(GX, GY, x + 15, GY + 40));   // last row of button 2's cell
        assertEquals(0, QuickActions.buttonAt(GX, GY, x + 15, GY + 41));   // first row of button 1's
        assertEquals(2, QuickActions.buttonAt(GX, GY, x - 1, GY + 9));
    }

    @Test void outsideTheColumnIsNothing() {
        int x = GX + 77;
        assertEquals(-1, QuickActions.buttonAt(GX, GY, x - 2, GY + 30));
        assertEquals(-1, QuickActions.buttonAt(GX, GY, x + 17, GY + 30));
        assertEquals(-1, QuickActions.buttonAt(GX, GY, x + 8, GY + 57));   // below button 1
        assertEquals(-1, QuickActions.buttonAt(GX, GY, x + 8, GY + 8));    // above button 3
    }
}
