package com.islesplus.features.rollpercent;

import java.util.Map;

/**
 * Which damage type each tooltip icon stands for. The server draws a stat's icon as a private-use
 * glyph (default font), and the same glyph means the same type on every item - unlike the number
 * beside it, which is scaled by the item's level and so matches nothing in the item's data.
 * <p>An icon names a TYPE, not a stat: the illusion icon marks Illusion Defense on an armour
 * piece's defence row and Illusion Damage on a "+x% Damage" line, and one item can have both. The
 * stat is {@code <TYPE>_DEFENSE} or {@code <TYPE>_DAMAGE}, picked from the line (see
 * {@link RollPercent#place}).
 * <p>Codepoints are from the Isles resource pack, {@code assets/minecraft/font/default.json}: each
 * maps to {@code custom/ui/dmg_indicator_<type>.png}. Types are spelled as in
 * {@code mythicmobs:stats} ("pierce" is PIERCING, "throwables" is THROWABLE).
 */
public final class StatGlyphs {
    /** Icon codepoint -> damage type, as in the item's stat names ("STAB" for STAB_DAMAGE). */
    public static final Map<Integer, String> TYPE_OF = Map.ofEntries(
        Map.entry(0xE013, "ARCANE"),
        Map.entry(0xE014, "AURA"),
        Map.entry(0xE015, "BLUNT"),
        Map.entry(0xE016, "HEAVY"),
        Map.entry(0xE017, "ILLUSION"),
        Map.entry(0xE018, "PIERCING"),
        Map.entry(0xE019, "SLASH"),
        Map.entry(0xE01A, "STAB"),
        Map.entry(0xE01B, "THROWABLE"),
        // the same nine, drawn for a negative value
        Map.entry(0xE01C, "ARCANE"),
        Map.entry(0xE01D, "AURA"),
        Map.entry(0xE01E, "BLUNT"),
        Map.entry(0xE01F, "HEAVY"),
        Map.entry(0xE020, "ILLUSION"),
        Map.entry(0xE021, "PIERCING"),
        Map.entry(0xE022, "SLASH"),
        Map.entry(0xE023, "STAB"),
        Map.entry(0xE024, "THROWABLE"),
        // elements. Only AETHER and VOID are confirmed stat names (seen as *_DEFENSE on items); a
        // wrong guess is harmless, because an icon is only used for a stat the item actually has
        Map.entry(0xE025, "FIRE"),
        Map.entry(0xE026, "WATER"),
        Map.entry(0xE027, "LIGHTNING"),
        Map.entry(0xE028, "EARTH"),
        Map.entry(0xE029, "NATURE"),
        Map.entry(0xE02A, "AIR"),
        Map.entry(0xE02B, "ICE"),
        Map.entry(0xE02C, "AETHER"),
        Map.entry(0xE02D, "VOID"),
        Map.entry(0xE02E, "GENERIC"));

    private StatGlyphs() {}
}
