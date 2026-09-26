package com.islesplus.features.rollpercent;

import com.islesplus.ui.Theme;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tooltips are the ones from the in-game screenshots (icons shown as a private-use character). */
class RollPercentTest {
    /** The Ring of Fungoroth from the feature request: 0.775, 0.9667, 0.5. */
    @Test
    void labelsAreWholePercents() {
        assertEquals("[78%]", RollPercent.label(0.7750000000000001));
        assertEquals("[97%]", RollPercent.label(0.9666666666666667));
        assertEquals("[50%]", RollPercent.label(0.5));
        assertEquals("[100%]", RollPercent.label(1.0));
        assertEquals("[0%]", RollPercent.label(0.0));
    }

    @Test
    void outOfRangeRatiosAreClamped() {
        assertEquals("[100%]", RollPercent.label(1.3));
        assertEquals("[0%]", RollPercent.label(-0.2));
    }

    @Test
    void ringStatsFindTheirLinesByValue() {
        List<String> tooltip = List.of("Ring of Fungoroth", "Rare Ring", "",
            "+11.1% Crit Damage", "+5.9%  Damage", "+4.5%  Damage", "");
        assertArrayEquals(new int[] { 3, 4, 5 }, RollPercent.matchLines(tooltip, List.of(0.111, 0.059, 0.045)));
        // the item's own order does not have to be the tooltip's
        assertArrayEquals(new int[] { 5, 3, 4 }, RollPercent.matchLines(tooltip, List.of(0.045, 0.111, 0.059)));
    }

    /** Aetheris showed nothing: its damage line has no sign, so counting signed lines came up short. */
    @Test
    void aWeaponsUnsignedDamageLineIsFound() {
        List<String> tooltip = List.of("Aetheris", "Legendary Spear", "24.8 ", "",
            "+0.56 Energy Regen", "+13.2% Crit Chance", "",
            " Godpierce 16", "Throw your spear towards your", "target, dealing 250% damage.",
            "Consumes 1 Honing Stone!", "PASSIVE » Deteriorate",
            "Decrease Generic Defense of the", "target by 5% every hit, up to 25%.");
        assertArrayEquals(new int[] { 2, 4, 5 }, RollPercent.matchLines(tooltip, List.of(24.8, 0.56, 0.132)));
    }

    @Test
    void equalNumbersAreTakenInOrder() {
        List<String> tooltip = List.of("Bronze Hatchet", "Common Hatchet", "+4.0 Woodcutting Power", "+4.0 Woodcutting Efficiency");
        assertArrayEquals(new int[] { 2, 3 }, RollPercent.matchLines(tooltip, List.of(4.0, 4.0)));
    }

    /** An upgrade can change the number shown; what is left is paired by position, if it pairs up exactly. */
    @Test
    void unmatchedStatsFallBackToPosition() {
        List<String> tooltip = List.of("Some Blade", "+12.5% Crit Damage", "+7.0 Strength");
        assertArrayEquals(new int[] { 1, 2 }, RollPercent.matchLines(tooltip, List.of(0.111, 7.0)));
        // two stats nobody can place but only one free line: leave them alone
        assertArrayEquals(new int[] { -1, -1 }, RollPercent.matchLines(List.of("Some Blade", "+1 Luck"), List.of(0.27, 0.31)));
    }

    /** Sporeheart Orb, as the server sends it: the damage line is centred with spaces and an
     * invisible layout glyph, and shows 20.6 while the stat holds 11.2 (scaled by level). Its
     * number matches nothing, so it is placed as the one free stat line - unsigned, but with an icon. */
    @Test
    void aCentredScaledDamageLineIsFoundByElimination() {
        List<String> tooltip = List.of("Sporeheart Orb", "              Rare Focus", "",
            "                  20.6 ", "", "+0.73 Energy Regen", "",
            " Toxic Gas 20⚡", "Poison enemies around you,", "lowering their Defense by 25%.",
            "Consumes 1 Magic Rune!");
        assertArrayEquals(new int[] { 3, 5 }, RollPercent.matchLines(tooltip, List.of(11.2, 0.73)));
    }

    /** Shadespore Leggings put Max Health's roll on "+9.4% Damage": same digits, but one line is a
     * percentage and the other is not. A stored 9.4 is "+9.4", a stored 0.094 is "+9.4%". */
    @Test
    void aPercentLineAndAFlatLineWithTheSameDigitsAreNotConfused() {
        List<String> tooltip = List.of("Shadespore Leggings", "+8.7%  Damage", "+9.4%  Damage",
            "+9.4 Max Health", "+4.7% Crit Chance");
        assertArrayEquals(new int[] { 3, 1, 2, 4 }, RollPercent.matchLines(tooltip, List.of(9.4, 0.087, 0.094, 0.047)));
    }

    /** Ability text can open with a bare number; without an icon it is prose, not a stat line. */
    @Test
    void anUnsignedLineWithoutAnIconIsNeverAFallback() {
        List<String> tooltip = List.of("Some Staff", "20 blocks of range,", "+0.73 Energy Regen");
        assertArrayEquals(new int[] { -1, 2 }, RollPercent.matchLines(tooltip, List.of(11.2, 0.73)));
    }

    @Test
    void roundedOrTruncatedDisplayStillMatches() {
        assertArrayEquals(new int[] { 0 }, RollPercent.matchLines(List.of("+5.6% Damage"), List.of(0.0566)));
        assertArrayEquals(new int[] { 0 }, RollPercent.matchLines(List.of("+5.7% Damage"), List.of(0.0566)));
        assertArrayEquals(new int[] { 0 }, RollPercent.matchLines(List.of("-3 Speed"), List.of(-3.0)));
    }

    /** The Bronze Hatchet showed "[0%]": its stats are fixed, the server just stores 0 for them. */
    @Test
    void anItemWithOnlyZeroRatiosWasNeverRolled() {
        assertFalse(RollPercent.anyRolled(List.of(0.0)));
        assertFalse(RollPercent.anyRolled(Arrays.asList(0.0, 0.0, Double.NaN, null)));
        assertFalse(RollPercent.anyRolled(List.of()));
        assertTrue(RollPercent.anyRolled(List.of(0.0, 0.5)));
    }

    /** A roll with no line is listed by the stat's own name. */
    @Test
    void statNamesReadAsWords() {
        assertEquals("Stab Damage", RollPercent.statLabel("STAB_DAMAGE"));
        assertEquals("Critical Strike Damage", RollPercent.statLabel("CRITICAL_STRIKE_DAMAGE"));
        assertEquals("Health", RollPercent.statLabel("HEALTH"));
        assertEquals("Void Defense", RollPercent.statLabel("VOID__DEFENSE_"));
    }

    @Test
    void colourFollowsTheQuarter() {
        assertEquals(Theme.HUD_RED & 0xFFFFFF, RollPercent.colorFor(0.10));
        assertEquals(Theme.HUD_WARN & 0xFFFFFF, RollPercent.colorFor(0.25));
        assertEquals(Theme.HUD_GOLD & 0xFFFFFF, RollPercent.colorFor(0.5));
        assertEquals(Theme.HUD_LIME & 0xFFFFFF, RollPercent.colorFor(0.775));
        assertEquals(Theme.HUD_LIME & 0xFFFFFF, RollPercent.colorFor(0.99));
        assertEquals(Theme.HUD_VERDIGRIS & 0xFFFFFF, RollPercent.colorFor(1.0));
    }
}
