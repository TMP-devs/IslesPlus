package com.islesplus.features.rollpercent;

import com.islesplus.logging.IslesLog;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Theme;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows how well each stat on an item rolled, next to the stat in its tooltip:
 * {@code +11.1% Crit Damage [78%]}.
 * <p>The server keeps the roll in the item's custom data, under
 * {@code PublicBukkitValues}:
 * <pre>
 * "mythicmobs:stats":       { "k:s":3, "k:0":"CRITICAL_STRIKE_DAMAGE", "v:0":{ "k:s":1, "k:0":"ADDITIVE", "v:0":0.111d }, ... }
 * "mythicmobs:stat_ratios": { "k:s":3, "k:0":"CRITICAL_STRIKE_DAMAGE", "v:0":{ "k:s":1, "k:0":"ADDITIVE", "v:0":0.775d }, ... }
 * </pre>
 * Both are maps written as numbered pairs ({@code k:N} -> {@code v:N}, {@code k:s} = size), one
 * level for the stat and one for its modifier type. {@code stats} is every stat with its value;
 * {@code stat_ratios} holds 0..1 for each stat that was ROLLED, under the same names.
 * <p>A stat is tied to its tooltip line by the NUMBER the line shows: {@code stats} says crit damage
 * is 0.111, so the line reading "+11.1%" is crit damage. That copes with lines that have no sign
 * (a weapon's "24.8" damage line), with fixed stats mixed in, and with any order. Stats whose
 * number cannot be found (an upgrade changed what is displayed) fall back to position among the
 * signed lines that are left. A stat that matches no line gets nothing: a wrong percentage is
 * worse than none.
 * <p>An item whose ratios are all 0 was never rolled - its stats are fixed, like a Bronze Hatchet -
 * and shows no percentages at all.
 * <p>A rolled stat that no line can be tied to (a level-scaled "+12.4% Damage" whose icon is not
 * known) is not dropped: it is listed by name at the bottom of the tooltip, "Stab Damage [95%]",
 * read straight from the item's data like the IslesNBT inspector does. So every roll always shows.
 */
public final class RollPercent {
    public static boolean rollPercentEnabled = true;

    private static final String BUKKIT_VALUES = "PublicBukkitValues";
    private static final String STATS = "mythicmobs:stats";
    private static final String RATIOS = "mythicmobs:stat_ratios";
    /** Next to PublicBukkitValues in the custom data: "HELMET", "BOOTS", ... */
    private static final String ITEM_TYPE = "ITEM_TYPE";
    private static final Set<String> ARMOUR = Set.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
    /** Rolls by tooltip text, for stand-in items that arrive without their data. See {@link RollCache}. */
    private static final RollCache CACHE = new RollCache(4096);
    /** Resolved on use, not in a static field: the loader does not exist in unit tests. */
    private static Path cachePath() {
        return FabricLoader.getInstance().getConfigDir().resolve("islesplus").resolve("roll_cache.json");
    }
    /** A line that opens with a number: "+11.1% Crit Damage", "-3 Speed", "24.8 (sword icon)".
     * The server centres a weapon's damage line with spaces and invisible private-use layout
     * glyphs, so those may come first. Groups: 1 sign, 2 number, 3 its decimals. */
    private static final Pattern LEADING_NUMBER = Pattern.compile("^[\\s\\uE000-\\uF8FF]*([+-]?)(\\d+(?:\\.(\\d+))?)");

    private RollPercent() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!rollPercentEnabled || FeatureFlags.isKilled("roll_percent")) return;
            try {
                annotate(stack, lines);
            } catch (RuntimeException ignored) {
                // a tooltip must never break over an unexpected NBT shape
            }
        });
    }

    private static void annotate(ItemStack stack, List<Text> lines) {
        if (lines.isEmpty()) return;
        // the item's name, and the first lore line: the header that carries its level
        String item = RollCache.item(stack.getName().getString(), lines.size() > 1 ? lines.get(1).getString() : "");
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = custom == null ? new NbtCompound() : custom.copyNbt();
        NbtCompound values = data.getCompoundOrEmpty(BUKKIT_VALUES);
        if (!values.contains(RATIOS)) {
            // no roll data at all: a stand-in (a worn helmet under a cosmetic is sent as {p_hat:1}
            // with the real lore). Show what the real item taught us about these same lines, if it did.
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).getString();
                List<Integer> offsets = new ArrayList<>(List.of(Slot.END));
                Matcher m = NUMBER_THEN_ICON.matcher(line);
                while (m.find()) offsets.add(m.end(1));
                write(lines, i, offsets, offset -> CACHE.recall(item, cacheLine(line, offset)));
            }
            return;
        }

        Map<String, Double> ratios = new HashMap<>();
        readPairs(values.getCompoundOrEmpty(RATIOS), ratios, null);
        if (!anyRolled(ratios.values())) return;   // fixed stats only: nothing was rolled

        List<String> statKeys = new ArrayList<>();
        Map<String, Double> statValues = new HashMap<>();
        readPairs(values.getCompoundOrEmpty(STATS), statValues, statKeys);
        // only ROLLED stats are placed: a fixed one (armour's VOID_DEFENSE has a value, no ratio and
        // no line) would otherwise claim the line of a rolled stat that happens to show its number
        statKeys.removeIf(key -> { Double r = ratios.get(key); return r == null || r.isNaN(); });
        if (statKeys.isEmpty()) return;

        List<String> plain = new ArrayList<>(lines.size());
        for (Text line : lines) plain.add(line.getString());
        List<String> names = new ArrayList<>(statKeys.size());
        List<Double> valuesInOrder = new ArrayList<>(statKeys.size());
        for (String key : statKeys) {
            names.add(key.substring(0, key.indexOf('/')));
            valuesInOrder.add(statValues.get(key));
        }
        String itemType = data.getString(ITEM_TYPE, "");
        Slot[] slots = place(plain, names, valuesInOrder, itemType.isEmpty() ? null : ARMOUR.contains(itemType));

        for (int i = 0; i < lines.size(); i++) {
            Map<Integer, Double> rolls = new HashMap<>();
            for (int n = 0; n < slots.length; n++) {
                if (slots[n] == null || slots[n].line() != i) continue;
                double ratio = ratios.get(statKeys.get(n));
                rolls.put(slots[n].offset(), ratio);
                CACHE.remember(item, cacheLine(plain.get(i), slots[n].offset()), ratio);
            }
            if (!rolls.isEmpty()) write(lines, i, new ArrayList<>(rolls.keySet()), rolls::get);
        }

        // rolls no line could be tied to: listed by name, so none is ever missing
        for (int n = 0; n < slots.length; n++) {
            if (slots[n] != null) continue;
            double ratio = ratios.get(statKeys.get(n));
            lines.add(Text.literal(statLabel(names.get(n)))
                .styled(st -> st.withColor(TextColor.fromRgb(Theme.HUD_MUTED & 0xFFFFFF)).withItalic(false))
                .append(rollText(ratio)));
        }
    }

    /** "CRITICAL_STRIKE_DAMAGE" -> "Critical Strike Damage": a stat's name as the tooltip lists it. */
    static String statLabel(String statName) {
        StringBuilder out = new StringBuilder();
        for (String word : statName.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return out.toString();
    }

    /** The cache's name for one slot of a line: a line can carry several stats. */
    private static String cacheLine(String line, int offset) {
        return offset == Slot.END ? line : line.strip() + "@" + (offset - (line.length() - line.stripLeading().length()));
    }

    /**
     * Writes the rolls into line {@code at}: {@code rollAt} gives the roll for an offset (null =
     * none). Inline ones go in from the right so the offsets to their left stay true. A centred
     * line (the server pads it with leading spaces) then loses half the added width in padding, so
     * it stays centred.
     */
    private static void write(List<Text> lines, int at, List<Integer> offsets, java.util.function.Function<Integer, Double> rollAt) {
        Text original = lines.get(at);
        Text line = original;
        List<Integer> inline = new ArrayList<>(offsets);
        inline.remove(Integer.valueOf(Slot.END));
        inline.sort(java.util.Comparator.reverseOrder());
        for (int offset : inline) {
            Double ratio = rollAt.apply(offset);
            if (ratio != null) line = insert(line, offset, rollText(ratio));
        }
        Double atEnd = offsets.contains(Slot.END) ? rollAt.apply(Slot.END) : null;
        if (atEnd != null) line = line.copy().append(rollText(atEnd));
        if (line == original) return;
        lines.set(at, recentre(original, line));
    }

    private static Text rollText(double ratio) {
        return Text.literal(" " + label(ratio)).styled(s -> s.withColor(TextColor.fromRgb(colorFor(ratio))).withItalic(false));
    }

    /** {@code line} with {@code extra} put in after its first {@code offset} characters, every run keeping its style. */
    private static Text insert(Text line, int offset, Text extra) {
        MutableText out = Text.empty();
        int[] pos = { 0 };
        line.visit((style, run) -> {
            int cut = offset - pos[0];
            if (cut > 0 && cut <= run.length()) {
                out.append(Text.literal(run.substring(0, cut)).setStyle(style)).append(extra);
                if (cut < run.length()) out.append(Text.literal(run.substring(cut)).setStyle(style));
            } else {
                out.append(Text.literal(run).setStyle(style));
            }
            pos[0] += run.length();
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** Drops leading spaces worth half of what {@code grown} gained over {@code original}, if it has them. */
    private static Text recentre(Text original, Text grown) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String plain = grown.getString();
        int padding = plain.length() - plain.stripLeading().length();
        if (tr == null || padding < 4) return grown;   // not a centred line
        int drop = Math.min(padding, Math.round((tr.getWidth(grown) - tr.getWidth(original)) / 2f / Math.max(1, tr.getWidth(" "))));
        if (drop <= 0) return grown;
        MutableText out = Text.empty();
        int[] left = { drop };
        grown.visit((style, run) -> {
            int cut = Math.min(left[0], run.length());
            left[0] -= cut;
            if (cut < run.length()) out.append(Text.literal(run.substring(cut)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** Loads the remembered rolls; call once at startup. */
    public static void loadCache() {
        try {
            Path path = cachePath();
            if (Files.exists(path)) CACHE.readJson(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            IslesLog.runtimeWarn("Roll Percent: could not read the roll cache", e);
        }
    }

    /** Writes the remembered rolls if any are new. Called on join, disconnect and shutdown - never
     * from the tooltip, which runs every frame. */
    public static void saveCache() {
        if (!CACHE.dirty()) return;
        try {
            Path path = cachePath();
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, CACHE.toJson());
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            CACHE.markSaved();
        } catch (IOException | RuntimeException e) {
            IslesLog.runtimeWarn("Roll Percent: could not write the roll cache", e);
        }
    }

    /** Reads a two-level "k:N / v:N" map as "STAT/MODIFIER" -> number, keeping the map's order. */
    private static void readPairs(NbtCompound map, Map<String, Double> values, List<String> order) {
        int stats = map.getInt("k:s", 0);
        for (int i = 0; i < stats; i++) {
            String stat = map.getString("k:" + i, "");
            NbtCompound modifiers = map.getCompoundOrEmpty("v:" + i);
            int count = modifiers.getInt("k:s", 0);
            for (int j = 0; j < count; j++) {
                String key = stat + "/" + modifiers.getString("k:" + j, "");
                if (order != null) order.add(key);
                if (values != null) values.put(key, modifiers.getDouble("v:" + j, Double.NaN));
            }
        }
    }

    /** Whether anything on the item was rolled: fixed-stat items carry ratios too, but all 0. */
    static boolean anyRolled(Collection<Double> ratios) {
        for (Double r : ratios) {
            if (r != null && !r.isNaN() && r > 0) return true;
        }
        return false;
    }

    /**
     * For each stat (by its value, in the item's own order) the index of the tooltip line showing
     * it, or -1. First by the number on the line - the value itself or the value as a percentage,
     * to the precision the line shows; equal numbers are taken in order. Stats still unplaced are
     * then paired by position with the stat lines nobody claimed - signed numbers, and a weapon's
     * unsigned damage line (a number followed by its icon), whose number is scaled by level and so
     * matches nothing - but only if there are exactly as many of each.
     */
    static int[] matchLines(List<String> lines, List<Double> statValues) {
        int[] lineOf = new int[statValues.size()];
        Arrays.fill(lineOf, -1);
        matchLines(lines, statValues, lineOf, new boolean[lines.size()]);
        return lineOf;
    }

    /** Where a stat's roll goes: on which line, and after which character of it ({@link #END} =
     * at the end of the line, the usual place). */
    record Slot(int line, int offset) {
        static final int END = -1;
    }

    /** A number and the icon after it: "30.8 (icon)", "+9.4% (icon)". Group 1 is the icon. */
    private static final Pattern NUMBER_THEN_ICON = Pattern.compile("\\d+(?:\\.\\d+)?%?\\s*([\\uE000-\\uF8FF])");
    /** Stats whose line is known by its wording. "Base Defense" is scaled by level like the icon
     * stats, but has no icon. */
    private static final Map<String, String> STAT_OF_LABEL = Map.of("Base Defense", "GENERIC_DEFENSE");

    /** {@link #place(List, List, List, Boolean)} for an item of unknown kind. */
    static Slot[] place(List<String> lines, List<String> statNames, List<Double> statValues) {
        return place(lines, statNames, statValues, null);
    }

    /**
     * Where each stat's roll goes, or null for a stat with no place. {@code statNames} are the
     * names in {@code mythicmobs:stats} ("AURA_DAMAGE"), {@code statValues} their values;
     * {@code armour} is whether the item is an armour piece (null = unknown).
     * <p>First by ICON: defence and damage are displayed scaled by the item's level (a stored 5.5
     * reads "30.8"), so their numbers match nothing, but the icon after the number names the
     * damage type ({@link StatGlyphs}); see {@link #statForIcon} for defense vs damage. A line with
     * one icon takes the roll at its end; a line carrying several (armour's defence row) takes each
     * roll right after its own icon - counting every icon on the line, known or not, so a roll can
     * never end up beside another stat's number. Then by LABEL ("Base Defense"). Whatever is left
     * is matched by number, see {@link #matchLines}.
     */
    static Slot[] place(List<String> lines, List<String> statNames, List<Double> statValues, Boolean armour) {
        Slot[] slots = new Slot[statNames.size()];
        int[] lineOf = new int[statNames.size()];
        Arrays.fill(lineOf, -1);
        boolean[] taken = new boolean[lines.size()];

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            List<int[]> found = new ArrayList<>();   // {stat index, offset just after the icon}
            int icons = 0;
            Matcher m = NUMBER_THEN_ICON.matcher(line);
            while (m.find()) {
                icons++;
                int n = statForIcon(line, line.codePointAt(m.start(1)), statNames, lineOf, armour);
                if (n >= 0 && found.stream().noneMatch(f -> f[0] == n)) found.add(new int[] { n, m.end(1) });
            }
            for (int[] f : found) {
                slots[f[0]] = new Slot(i, icons == 1 ? Slot.END : f[1]);
                lineOf[f[0]] = i;
                taken[i] = true;
            }
            if (taken[i]) continue;
            for (Map.Entry<String, String> label : STAT_OF_LABEL.entrySet()) {
                int n = statNames.indexOf(label.getValue());
                if (n >= 0 && lineOf[n] < 0 && line.contains(label.getKey()) && LEADING_NUMBER.matcher(line.strip()).find()) {
                    slots[n] = new Slot(i, Slot.END);
                    lineOf[n] = i;
                    taken[i] = true;
                }
            }
        }

        matchLines(lines, statValues, lineOf, taken);
        for (int n = 0; n < slots.length; n++) {
            if (slots[n] == null && lineOf[n] >= 0) slots[n] = new Slot(lineOf[n], Slot.END);
        }
        return slots;
    }

    /**
     * The stat an icon on {@code line} stands for, as an index into {@code statNames}, or -1. The
     * icon gives the type; {@code <TYPE>_DAMAGE} or {@code <TYPE>_DEFENSE} is then picked by the
     * line's own word ("+9.4% (icon) Damage"), else - an unlabelled number - by which of the two
     * the item has, and when it has both (boots with Illusion Defense AND Illusion Damage) by the
     * item: armour shows defence that way, a weapon its damage. Only a stat the item has and that
     * is not placed yet is ever returned; anything unclear gives -1, never a guess.
     */
    private static int statForIcon(String line, int icon, List<String> statNames, int[] lineOf, Boolean armour) {
        String type = StatGlyphs.TYPE_OF.get(icon);
        if (type == null) return -1;
        int defense = unplaced(statNames, type + "_DEFENSE", lineOf);
        int damage = unplaced(statNames, type + "_DAMAGE", lineOf);
        if (line.contains("Damage")) return damage;
        if (line.contains("Defense")) return defense;
        if (defense < 0) return damage;
        if (damage < 0) return defense;
        return armour == null ? -1 : armour ? defense : damage;
    }

    private static int unplaced(List<String> statNames, String name, int[] lineOf) {
        int n = statNames.indexOf(name);
        return n >= 0 && lineOf[n] < 0 ? n : -1;
    }

    /** The long blank (struck-through) line between a tooltip's sections. */
    private static boolean isDivider(String line) {
        return line.length() >= 20 && line.isBlank();
    }

    /** {@link #matchLines(List, List)} for the stats still at -1 in {@code lineOf}, on the lines
     * not yet {@code taken}; both are updated. */
    private static void matchLines(List<String> lines, List<Double> statValues, int[] lineOf, boolean[] taken) {

        // Strict first: a "%" line shows the value as a percentage, any other line the value itself,
        // so "+9.4 Max Health" (9.4) and "+9.4% Damage" (0.094) cannot take each other's line. Then
        // once more reading either way, for a stat the server stores in the unit it displays.
        for (boolean strict : new boolean[] { true, false }) {
            for (int n = 0; n < statValues.size(); n++) {
                Double value = statValues.get(n);
                if (lineOf[n] >= 0 || value == null || value.isNaN()) continue;
                double magnitude = Math.abs(value);
                for (int i = 0; i < lines.size(); i++) {
                    if (taken[i]) continue;
                    String line = lines.get(i).strip();
                    Matcher m = LEADING_NUMBER.matcher(line);
                    if (!m.find()) continue;
                    double shown = Double.parseDouble(m.group(2));
                    int decimals = m.group(3) == null ? 0 : m.group(3).length();
                    double unit = Math.pow(10, -decimals);
                    boolean percent = line.startsWith("%", m.end());
                    boolean asIs = displays(shown, magnitude, unit), asPercent = displays(shown, magnitude * 100.0, unit);
                    if (strict ? (percent ? asPercent : asIs) : (asIs || asPercent)) {
                        lineOf[n] = i;
                        taken[i] = true;
                        break;
                    }
                }
            }
        }

        List<Integer> unplaced = new ArrayList<>();
        List<Integer> freeSigned = new ArrayList<>();
        for (int n = 0; n < lineOf.length; n++) {
            if (lineOf[n] < 0) unplaced.add(n);
        }
        // Only lines in a section (between dividers) that already holds a placed stat: a set bonus
        // or passive further down ("+15% damage per stack") is prose, never a stat line.
        int[] sectionOf = new int[lines.size()];
        Set<Integer> statSections = new java.util.HashSet<>();
        for (int i = 0, section = 0; i < lines.size(); i++) {
            if (isDivider(lines.get(i))) section++;
            sectionOf[i] = section;
            if (taken[i]) statSections.add(section);
        }
        for (int i = 0; i < lines.size(); i++) {
            if (taken[i] || !statSections.contains(sectionOf[i])) continue;
            Matcher m = LEADING_NUMBER.matcher(lines.get(i).strip());
            if (!m.find()) continue;
            // signed, or a weapon's unsigned damage line: that one carries its icon after the number,
            // which a sentence that merely opens with a number ("20 blocks of range") does not
            String rest = lines.get(i).strip().substring(m.end());
            if (!m.group(1).isEmpty() || rest.codePoints().anyMatch(cp -> cp >= 0xE000 && cp <= 0xF8FF)) freeSigned.add(i);
        }
        if (!unplaced.isEmpty() && unplaced.size() == freeSigned.size()) {
            for (int k = 0; k < unplaced.size(); k++) lineOf[unplaced.get(k)] = freeSigned.get(k);
        }
    }

    /** Whether {@code shown} is what {@code value} looks like at that precision, whether the server
     * rounds (within half a unit) or truncates (shown &lt;= value &lt; shown + unit). */
    private static boolean displays(double shown, double value, double unit) {
        double eps = unit * 1e-6;
        return shown > value - unit - eps && shown <= value + unit / 2 + eps;
    }

    /** "[78%]" - whole percent, clamped to 0..100. */
    static String label(double ratio) {
        return "[" + percent(ratio) + "%]";
    }

    static int percent(double ratio) {
        return (int) Math.max(0, Math.min(100, Math.round(ratio * 100.0)));
    }

    /** Red to lime by quarter, and verdigris for a perfect roll: the HUD tones, as RGB. */
    static int colorFor(double ratio) {
        int p = percent(ratio);
        int argb = p >= 100 ? Theme.HUD_VERDIGRIS : p >= 75 ? Theme.HUD_LIME : p >= 50 ? Theme.HUD_GOLD
            : p >= 25 ? Theme.HUD_WARN : Theme.HUD_RED;
        return argb & 0xFFFFFF;
    }
}
