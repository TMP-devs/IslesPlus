package com.islesplus.world;

public enum PlayerWorld {
    /** hub, rift lobby, whatever else we don't recognise */
    OTHER,
    /** main isles world (Isles01, Isles02...) */
    ISLE,
    /** inside a rift */
    RIFT,
    /** inside a rift that's on the disabled list, dungeon features off */
    DISABLED_RIFT
}
