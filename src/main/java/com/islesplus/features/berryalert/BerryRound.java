package com.islesplus.features.berryalert;

/**
 * One berry round, from the first berry to the last. No Minecraft types, so it is unit tested.
 * <p>
 * A round is a string of berries: five clicks pick one, and the server puts the next one up
 * straight away, so a berry vanishing mid-round is not the end of it. The round only ends once no
 * berry has been seen for {@link #ROUND_END_MS}.
 */
public final class BerryRound {
    static final long ROUND_END_MS = 1_000L;
    static final long TITLE_MS = 3_000L;

    private boolean active;
    private long lastSeenMs;
    private long startedMs;

    /** Called every tick with whether a berry is up. True only when this starts a new round. */
    public boolean update(boolean berryUp, long nowMs) {
        if (berryUp) {
            lastSeenMs = nowMs;
            if (active) return false;
            active = true;
            startedMs = nowMs;
            return true;
        }
        if (active && nowMs - lastSeenMs >= ROUND_END_MS) active = false;
        return false;
    }

    public boolean active() {
        return active;
    }

    /** "Berry spawned!" is a heads-up, not a clock: it shows for {@link #TITLE_MS} from the
     * round's first berry, and the berries that follow in the same round do not bring it back. */
    public boolean titleVisible(long nowMs) {
        return active && nowMs - startedMs < TITLE_MS;
    }

    public void reset() {
        active = false;
    }
}
