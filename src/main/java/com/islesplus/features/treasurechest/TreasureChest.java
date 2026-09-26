package com.islesplus.features.treasurechest;

/**
 * What a sunken treasure chest is and how its waypoint reads. No Minecraft types, so it is unit
 * tested.
 * <p>
 * A chest is a modelengine rig (found with {@code /ip scan}, 2026-09-24): {@code chest_top} and
 * {@code chest_bot} item displays under {@code modelengine:prop_crate_<rarity>/}, an interaction
 * to click, and a label over it. The model says the rarity; the label says what kind of chest it
 * is ({@link #nameFromLabel}). Common and rare were scanned; the other tiers are the same pattern
 * and are read the same way, and a tier nobody has named here still gets a waypoint.
 */
public final class TreasureChest {
    private static final String NAMESPACE = "modelengine";
    private static final String PREFIX = "prop_crate_";
    private static final String TOP = "/chest_top";
    /** Up close the chest is in plain sight, and the tag would only sit in the way. */
    static final double HIDE_WITHIN = 5.0;

    static final int GREEN = 0xFF55E05A, LIGHT_BLUE = 0xFF6FD3F2, BLUE = 0xFF4F8DF5,
        PURPLE = 0xFFB86BF0, GOLD = 0xFFF2BC3C, WHITE = 0xFFF6ECD4;

    private TreasureChest() {}

    /** "rare" for {@code modelengine:prop_crate_rare/chest_top}; null for anything that is not
     * the top of a chest (the bottom is the same chest, so it would be a second waypoint). */
    public static String rarityOf(String namespace, String path) {
        if (!NAMESPACE.equals(namespace) || !path.startsWith(PREFIX) || !path.endsWith(TOP)) return null;
        String rarity = path.substring(PREFIX.length(), path.length() - TOP.length());
        return rarity.isEmpty() || rarity.contains("/") ? null : rarity;
    }

    /**
     * What a chest's waypoint is called, from the label standing over it - or null for a chest
     * that gets none. The same models are dressed up as several things (scan 2026-09-24 05:23):
     * <ul>
     * <li>"Sunken Tortuga Chest / Requires a sunken key." - a sunken chest, and not only
     *     Tortuga's: its own first line. The only kind that gets a waypoint.</li>
     * <li>"TREASURE / to loot!" - common chests all over the isle (one by the cooking
     *     station, scan 19:22): none.</li>
     * <li>"LOCKED / 2/5 Kills" - a kill-counter chest: none.</li>
     * <li>no label at all (a smaller one with fire on it) - none.</li>
     * </ul>
     */
    public static String nameFromLabel(String label) {
        String[] lines = label.strip().split("\n");
        String first = lines[0].strip();
        boolean sunkenName = first.startsWith("Sunken ") && first.endsWith(" Chest");
        boolean sunkenKey = label.toLowerCase(java.util.Locale.ROOT).contains("requires a sunken key");
        return sunkenName || sunkenKey ? first : null;
    }

    public static String distance(double metres) {
        return Math.round(metres) + "m";
    }

    public static int colorOf(String rarity) {
        return switch (rarity) {
            case "common" -> GREEN;
            case "uncommon" -> LIGHT_BLUE;
            case "rare" -> BLUE;
            case "epic" -> PURPLE;
            case "legendary" -> GOLD;
            default -> WHITE;
        };
    }

    /** The tag's backing: the rarity colour, darkened and see-through, so cream text reads on it. */
    public static int backgroundOf(String rarity) {
        int c = colorOf(rarity);
        int r = ((c >> 16) & 0xFF) * 35 / 100, g = ((c >> 8) & 0xFF) * 35 / 100, b = (c & 0xFF) * 35 / 100;
        return 0xCC000000 | (r << 16) | (g << 8) | b;
    }

    public static boolean showTag(double metres) {
        return metres > HIDE_WITHIN;
    }

    /** Tortuga's sunken chests are under the sea; above it (Y 32) their waypoints are only clutter. */
    static final double TORTUGA_SEA_Y = 32.0;

    /** Whether a chest's waypoint shows to a player at height {@code playerY}. */
    public static boolean shownFromHeight(String name, double playerY) {
        return !name.contains("Tortuga") || playerY <= TORTUGA_SEA_Y;
    }
}
