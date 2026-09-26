package com.islesplus.features.rollpercent;

import com.islesplus.features.rollpercent.RollPercent.Slot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Where each stat's roll goes in a tooltip, on the lore of real items (from inspector dumps). */
class RollPlacementTest {
    private static final List<String> LEGGINGS = List.of(
        "              Shadespore Leggings",
        "             Rare Leggings",
        "",
        "             30.8    35.8 ",
        "          79.5 Base Defense",
        "                                             ",
        "+8.7%  Damage",
        "+9.4%  Damage",
        "+9.4 Max Health",
        "+4.7% Crit Chance");

    /** Defence and damage are shown scaled by level (5.5 reads 30.8, 0.067 reads 9.4%), so their
     * numbers match nothing: the icon, or the "Base Defense" label, says which stat a number is. */
    @Test void scaledStatsArePlacedByTheirIconOrLabel() {
        Slot[] slots = RollPercent.place(LEGGINGS,
            List.of("GENERIC_DEFENSE", "ILLUSION_DEFENSE", "HEAVY_DEFENSE", "THROWABLE_DAMAGE", "AURA_DAMAGE", "HEALTH", "CRITICAL_STRIKE_CHANCE"),
            List.of(14.2, 5.5, 6.4, 0.062, 0.067, 9.4, 0.047));
        assertArrayEquals(new Slot[] {
            new Slot(4, Slot.END),
            new Slot(3, LEGGINGS.get(3).indexOf('') + 1),   // two stats share this line:
            new Slot(3, LEGGINGS.get(3).indexOf('') + 1),   // each roll goes right after its icon
            new Slot(6, Slot.END),
            new Slot(7, Slot.END),
            new Slot(8, Slot.END),
            new Slot(9, Slot.END) }, slots);
    }

    @Test void aWeaponsCentredDamageLineIsPlacedByItsIcon() {
        List<String> orb = List.of("Sporeheart Orb", "              Rare Focus", "",
            "                  20.6 ", "", "+0.73 Energy Regen");
        assertArrayEquals(new Slot[] { new Slot(3, Slot.END), new Slot(5, Slot.END) },
            RollPercent.place(orb, List.of("AURA_DAMAGE", "ENERGY_REGENERATION"), List.of(11.2, 0.73)));
    }

    @Test void anIconForAStatTheItemDoesNotHaveIsIgnored() {
        List<String> lines = List.of("+9.4%  Damage", "+9.4 Max Health");
        assertArrayEquals(new Slot[] { new Slot(1, Slot.END) }, RollPercent.place(lines, List.of("HEALTH"), List.of(9.4)));
    }

    private static final String DIVIDER = " ".repeat(44);

    /** Storm's Legacy Boots (level 30): the illusion icon is on the defence row (Illusion Defense)
     * AND on a Damage line (Illusion Damage), and the item has both stats. */
    @Test void bootsWithTheSameIconForADefenseAndADamageStatShowEveryRoll() {
        List<String> boots = List.of(
            "               Storm's Legacy Boots",
            "           Legendary Boots",
            "",
            "             86.9    92.0 ",
            "          157.5 Base Defense",
            DIVIDER,
            "+12.4%  Damage",
            "+11.7%  Damage",
            "+6.3 Max Health",
            "+13.6% Crit Damage",
            "+0.4 Energy Regen",
            DIVIDER,
            "SET BONUS » Storm's Echo",
            "+15% damage per stack, up to 3.",
            "+50% damage per stack.",
            DIVIDER);
        // the rolled stats in the item's own order (VOID_DEFENSE has no ratio, so it is not here)
        Slot[] slots = RollPercent.place(boots,
            List.of("GENERIC_DEFENSE", "ILLUSION_DEFENSE", "BLUNT_DEFENSE", "STAB_DAMAGE", "ILLUSION_DAMAGE",
                "HEALTH", "CRITICAL_STRIKE_DAMAGE", "ENERGY_REGENERATION"),
            List.of(12.5, 6.9, 7.3, 0.069, 0.065, 6.3, 0.136, 0.4), true);
        String row = boots.get(3);
        assertArrayEquals(new Slot[] {
            new Slot(4, Slot.END),                          // 157.5 Base Defense
            new Slot(3, row.indexOf('') + 1),         // right after 86.9's icon
            new Slot(3, row.indexOf('') + 1),         // right after 92.0's icon
            new Slot(6, Slot.END),                          // +12.4% Damage (stab)
            new Slot(7, Slot.END),                          // +11.7% Damage (illusion)
            new Slot(8, Slot.END),
            new Slot(9, Slot.END),
            new Slot(10, Slot.END) }, slots);
    }

    /** Aetherbloom Helmet (level 12, with a helmet skin: the roll data is intact). The aura icon
     * here is Aura DEFENSE, and the pierce icon is PIERCING_DAMAGE. */
    @Test void helmetDefenceRowAndDamageLinesAreAllPlaced() {
        List<String> helmet = List.of(
            "                 Aetherbloom Helmet",
            "           Legendary Helmet",
            "",
            "             66.7    90.6 ",
            "          28.2 Base Defense",
            DIVIDER,
            "+7.7%  Damage",
            "+8.8%  Damage",
            "+13.8 Max Health",
            "+13.7% Crit Damage",
            "+0.25 Energy Regen",
            DIVIDER,
            "SET BONUS » Alliance",
            "Gain +3% Crit Chance for",
            DIVIDER);
        Slot[] slots = RollPercent.place(helmet,
            List.of("GENERIC_DEFENSE", "ARCANE_DEFENSE", "AURA_DEFENSE", "STAB_DAMAGE", "PIERCING_DAMAGE",
                "HEALTH", "CRITICAL_STRIKE_DAMAGE", "ENERGY_REGENERATION"),
            List.of(4.6, 10.9, 14.8, 0.043, 0.049, 13.8, 0.137, 0.25), true);
        String row = helmet.get(3);
        assertArrayEquals(new Slot[] {
            new Slot(4, Slot.END),
            new Slot(3, row.indexOf('') + 1),
            new Slot(3, row.indexOf('') + 1),
            new Slot(6, Slot.END),
            new Slot(7, Slot.END),
            new Slot(8, Slot.END),
            new Slot(9, Slot.END),
            new Slot(10, Slot.END) }, slots);
    }

    /** Blackrib Boots (level 22): Piercing AND Heavy each exist as a defence and as a damage stat. */
    @Test void blackribBootsPlaceBothTypesAsDefenseAndAsDamage() {
        List<String> boots = List.of(
            "                 Blackrib Boots",
            "            Epic Boots",
            "",
            "             79.4    73.1 ",
            "          139.9 Base Defense",
            DIVIDER,
            "+11.2%  Damage",
            "+9.4%  Damage",
            "+11.3 Max Health",
            "+7.0% Crit Chance",
            DIVIDER);
        Slot[] slots = RollPercent.place(boots,
            List.of("GENERIC_DEFENSE", "PIERCING_DEFENSE", "HEAVY_DEFENSE", "PIERCING_DAMAGE", "HEAVY_DAMAGE",
                "HEALTH", "CRITICAL_STRIKE_CHANCE"),
            List.of(11.1, 6.3, 5.8, 0.062, 0.052, 11.3, 0.07), true);
        String row = boots.get(3);
        assertArrayEquals(new Slot[] {
            new Slot(4, Slot.END),
            new Slot(3, row.indexOf('') + 1),
            new Slot(3, row.indexOf('') + 1),
            new Slot(6, Slot.END),
            new Slot(7, Slot.END),
            new Slot(8, Slot.END),
            new Slot(9, Slot.END) }, slots);
    }

    /** With one of a row's two icons unknown, the known one's roll must stay right after its own
     * icon - never at the end of the row, beside the other stat's number. */
    @Test void aRollNeverLandsBesideAnotherStatsNumber() {
        List<String> lines = List.of("             86.9    92.0 ");   // second icon unknown
        assertArrayEquals(new Slot[] { new Slot(0, lines.get(0).indexOf('') + 1) },
            RollPercent.place(lines, List.of("ILLUSION_DEFENSE"), List.of(6.9), true));
    }

    /** Defense or damage from the word on the line; for an unlabelled number with both stats on the
     * item, from the kind of item - and when that is unknown, nothing rather than a guess. */
    @Test void defenseOrDamageIsPickedFromTheLineOrTheItem() {
        List<String> names = List.of("ILLUSION_DEFENSE", "ILLUSION_DAMAGE");
        List<Double> values = List.of(6.9, 0.065);
        List<String> damageLine = List.of("+11.7%  Damage");
        assertArrayEquals(new Slot[] { null, new Slot(0, Slot.END) }, RollPercent.place(damageLine, names, values, true));
        List<String> bareNumber = List.of("86.9 ");
        assertArrayEquals(new Slot[] { new Slot(0, Slot.END), null }, RollPercent.place(bareNumber, names, values, true));
        assertArrayEquals(new Slot[] { null, new Slot(0, Slot.END) }, RollPercent.place(bareNumber, names, values, false));
        assertArrayEquals(new Slot[] { null, null }, RollPercent.place(bareNumber, names, values, null));
        // a "Damage" line never takes a defence stat, even when that is the only one the item has
        assertArrayEquals(new Slot[] { null }, RollPercent.place(damageLine, List.of("ILLUSION_DEFENSE"), List.of(6.9), true));
    }

    @Test void negativeIconsMeanTheSameType() {
        List<String> lines = List.of("-4.0%  Damage");
        assertArrayEquals(new Slot[] { new Slot(0, Slot.END) },
            RollPercent.place(lines, List.of("STAB_DAMAGE"), List.of(-0.04), true));
    }

    /** A weapon's unlabelled damage line (a spear's "23.3 (icon)") is its damage stat. */
    @Test void aWeaponsBareDamageNumberIsItsDamageStat() {
        List<String> spear = List.of("Aetheris", "Legendary Spear", "", "                  23.3 ", DIVIDER,
            "+0.47 Energy Regen", "+14.4% Crit Chance", DIVIDER, " Godpierce 16⚡", "target, dealing 250% damage.");
        assertArrayEquals(new Slot[] { new Slot(3, Slot.END), new Slot(5, Slot.END), new Slot(6, Slot.END) },
            RollPercent.place(spear, List.of("PIERCING_DAMAGE", "ENERGY_REGENERATION", "CRITICAL_STRIKE_CHANCE"),
                List.of(12.9, 0.47, 0.144), false));
    }

    /** The last-resort pairing by position must not reach into a set bonus: one leftover stat and
     * one leftover "+" line used to pair up even when that line was set-bonus prose. */
    @Test void positionalFallbackStaysInTheStatSection() {
        List<String> lines = List.of("Some Helmet", DIVIDER, "+9.4 Max Health", DIVIDER,
            "SET BONUS » Something", "+15% damage per stack", DIVIDER);
        Slot[] slots = RollPercent.place(lines, List.of("HEALTH", "LUCK"), List.of(9.4, 0.31));
        assertEquals(new Slot(2, Slot.END), slots[0]);
        assertEquals(null, slots[1]);
    }

    @Test void aStatWithNoLineIsNotPlaced() {
        // two stats, one stat line: the number finds Health, and nothing is left over for the other
        Slot[] slots = RollPercent.place(List.of("Some Charm", "+9.4 Max Health"), List.of("HEALTH", "LUCK"), List.of(9.4, 0.31));
        assertEquals(new Slot(1, Slot.END), slots[0]);
        assertEquals(null, slots[1]);
    }
}
