package com.islesplus.features.rollpercent;

import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Theme;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

/**
 * Adds the item's age at the bottom of its tooltip: {@code Age: 2d 3h}. The server stamps every
 * generated item with {@code PublicBukkitValues > mythicmobs:timestamp} (epoch milliseconds); items
 * without one - materials, auction listings, a worn helmet under a cosmetic - get no line.
 */
public final class ItemAge {
    public static boolean itemAgeEnabled = true;

    private static final long MIN = 60_000L, HOUR = 60 * MIN, DAY = 24 * HOUR, YEAR = 365 * DAY;

    private ItemAge() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!itemAgeEnabled || FeatureFlags.isKilled("item_age")) return;
            try {
                annotate(stack, lines);
            } catch (RuntimeException ignored) {
                // a tooltip must never break over an unexpected NBT shape
            }
        });
    }

    private static void annotate(ItemStack stack, List<Text> lines) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return;
        long stamp = custom.copyNbt().getCompoundOrEmpty("PublicBukkitValues").getLong("mythicmobs:timestamp", 0L);
        if (stamp <= 0) return;
        lines.add(Text.literal(format(System.currentTimeMillis() - stamp))
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(Theme.HUD_MUTED & 0xFFFFFF)).withItalic(false)));
    }

    /** "Age: 2d 3h" - the two largest units, a zero second one left out; under a minute is "<1m". */
    static String format(long ageMs) {
        long ms = Math.max(0, ageMs);
        if (ms < MIN) return "Age: <1m";
        if (ms < HOUR) return "Age: " + ms / MIN + "m";
        if (ms < DAY) return "Age: " + pair(ms / HOUR, "h", ms % HOUR / MIN, "m");
        if (ms < YEAR) return "Age: " + pair(ms / DAY, "d", ms % DAY / HOUR, "h");
        return "Age: " + pair(ms / YEAR, "y", ms % YEAR / DAY, "d");
    }

    private static String pair(long big, String bigUnit, long small, String smallUnit) {
        return small == 0 ? big + bigUnit : big + bigUnit + " " + small + smallUnit;
    }
}
