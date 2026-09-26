package com.islesplus.features.rollpercent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemAgeTest {
    private static final long MIN = 60_000L, HOUR = 60 * MIN, DAY = 24 * HOUR;

    @Test void underAMinuteIsNew() {
        assertEquals("Age: <1m", ItemAge.format(59_000));
    }

    @Test void minutesAlone() {
        assertEquals("Age: 42m", ItemAge.format(42 * MIN + 30_000));
    }

    @Test void hoursThenMinutes() {
        assertEquals("Age: 5h 7m", ItemAge.format(5 * HOUR + 7 * MIN));
    }

    @Test void daysThenHours() {
        assertEquals("Age: 2d 3h", ItemAge.format(2 * DAY + 3 * HOUR + 59 * MIN));
    }

    @Test void yearsThenDays() {
        assertEquals("Age: 1y 12d", ItemAge.format(377 * DAY));
    }

    @Test void aZeroSecondUnitIsLeftOut() {
        assertEquals("Age: 4d", ItemAge.format(4 * DAY + 20 * MIN));
        assertEquals("Age: 3h", ItemAge.format(3 * HOUR));
    }

    /** The timestamp is the server's clock: a client clock slightly behind it must not show "-1m". */
    @Test void aTimestampInTheFutureIsNew() {
        assertEquals("Age: <1m", ItemAge.format(-90_000));
    }
}
