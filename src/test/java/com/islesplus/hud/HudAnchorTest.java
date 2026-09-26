package com.islesplus.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudAnchorTest {

    @Test
    void resolvesEachAnchor() {
        assertEquals(10, HudAnchor.resolve(HudAnchor.START, 10, 400, 100));
        assertEquals(150 + 5, HudAnchor.resolve(HudAnchor.CENTER, 5, 400, 100));
        assertEquals(400 - 100 - 10, HudAnchor.resolve(HudAnchor.END, 10, 400, 100));
    }

    @Test
    void anchorIsTheThirdTheCentreIsIn() {
        assertEquals(HudAnchor.START, HudAnchor.anchorFor(0, 900, 100));      // centre 50
        assertEquals(HudAnchor.CENTER, HudAnchor.anchorFor(400, 900, 100));   // centre 450
        assertEquals(HudAnchor.END, HudAnchor.anchorFor(790, 900, 100));      // centre 840
    }

    @Test
    void offsetRoundTripsThroughResolve() {
        for (int anchor = HudAnchor.START; anchor <= HudAnchor.END; anchor++) {
            for (int pos : new int[]{0, 13, 250, 377}) {
                int off = HudAnchor.offsetFor(anchor, pos, 427, 50);
                assertEquals(pos, HudAnchor.resolve(anchor, off, 427, 50));
            }
        }
    }

    @Test
    void cornerKeepsItsMarginOnABiggerScreen() {
        int off = HudAnchor.offsetFor(HudAnchor.END, 480 - 60 - 10, 480, 60);
        assertEquals(10, off);
        assertEquals(960 - 60 - 10, HudAnchor.resolve(HudAnchor.END, off, 960, 60));
    }

    @Test
    void clampKeepsOnScreen() {
        assertEquals(0, HudAnchor.clamp(-5, 300, 50));
        assertEquals(250, HudAnchor.clamp(290, 300, 50));
        assertEquals(0, HudAnchor.clamp(10, 300, 400));   // too big: pinned to the start edge
    }

    @Test
    void scaledRoundsUp() {
        assertEquals(51, HudAnchor.scaled(34, 1.5f));
        assertEquals(35, HudAnchor.scaled(69, 0.5f));
    }

    @Test
    void scaleIsClampedAndRounded() {
        assertEquals(HudPlacement.MIN_SCALE, HudPlacement.clampScale(0.1f));
        assertEquals(HudPlacement.MAX_SCALE, HudPlacement.clampScale(9f));
        assertEquals(1.23f, HudPlacement.clampScale(1.2345f));
    }

    @Test
    void placementJsonRoundTrips() {
        HudPlacement p = new HudPlacement(HudAnchor.END, HudAnchor.CENTER, 12, -7);
        p.scale = 1.5f;
        HudPlacement back = HudPlacement.fromJson(p.toJson());
        assertEquals(true, p.sameAs(back));
    }

    @Test
    void opacityRoundTripsAndIsCopied() {
        HudPlacement p = new HudPlacement(HudAnchor.START, HudAnchor.START, 10, 10);
        p.opacity = 0.35f;
        assertEquals(0.35f, HudPlacement.fromJson(p.toJson()).opacity);
        assertEquals(0.35f, p.copy().opacity);
        assertEquals(0f, HudPlacement.clampOpacity(-1f));
    }
}
