package com.islesplus.world;

public enum PlayerWorld {
    /** Hub, rift lobby, or any unrecognised world. */
    OTHER,
    /** Isles overworld instance (Isles01, Isles02, …). */
    ISLE,
    /** Active rift run. */
    RIFT,
    /** Active rift run where dungeon features are explicitly disabled. */
    DISABLED_RIFT
}
