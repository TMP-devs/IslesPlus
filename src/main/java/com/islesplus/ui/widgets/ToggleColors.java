package com.islesplus.ui.widgets;

import com.islesplus.ui.Theme;

/** Fill/lit/shade/ring colour tuples shared by the on/active and off/idle states of the
 * toggle, tile and chip widgets, so each widget doesn't repeat the same colour-selection block. */
final class ToggleColors {
    private ToggleColors() {}

    static final int FILL = 0, LIT = 1, SHADE = 2, RING = 3;

    /** Toggle "on" track, and tile/chip "active" background: OXBLOOD trio, INK ring. */
    static final int[] ON = {Theme.OXBLOOD, Theme.OXBLOOD_LIT, Theme.OXBLOOD_SHADE, Theme.INK};
    /** Toggle "off" track. */
    static final int[] TRACK_OFF = {Theme.OFF_TRACK, Theme.OFF_LIT, Theme.OFF_SHADE, Theme.INK};
    /** Tile/chip "idle" background. */
    static final int[] IDLE = {Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.RAISED_RING};

    /** Tile/chip checkbox indicator: {fill, ring}. */
    static final int[] BOX_ACTIVE = {Theme.CREAM, Theme.OXBLOOD_SHADE};
    static final int[] BOX_IDLE = {Theme.TICK_BOX_OFF, Theme.TICK_EDGE_OFF};
}
