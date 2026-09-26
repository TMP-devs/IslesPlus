package com.islesplus.features.foodbuff;

import com.islesplus.features.rankcalculator.TabListReader;
import com.islesplus.hud.HudAnchor;
import com.islesplus.hud.HudElement;
import com.islesplus.hud.HudLayout;
import com.islesplus.hud.HudPlacement;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Theme;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * "[icon] Ponkberry Smoothie: 3:42" under the sidebar scoreboard (the "play.skyblockisles.net"
 * block on the right), while a dish is active. The dish and its minutes come from the tab list;
 * the seconds from {@link FoodBuff.Clock}. The icon is the dish's own model from the server's
 * resource pack.
 */
public final class FoodBuffTimer {
    public static boolean foodBuffTimerEnabled = true;

    /** The tab is read twice a second: its minutes change once a minute. */
    private static final int READ_EVERY_TICKS = 10;
    /** The icon is drawn at 12 px (an item is 16), level with the 8 px text. */
    static final int ICON = 12, GAP = 3, BELOW_SCOREBOARD = 3;
    /** Where the scoreboard's own text sits inside its box: the line starts and ends there, so
     * it is as wide as the scoreboard and the time stays off the edge of the screen. */
    static final int INSET_LEFT = 2, INSET_RIGHT = 3;

    /** Where each part of the line goes, from the left. */
    record Layout(int iconX, int nameX, int timeX) {}
    /** Dishes live in two folders of the pack: smoothies in dishes/, the rest in cooking/dishes/. */
    private static final String[] MODEL_FOLDERS = {"dishes/", "cooking/dishes/"};

    private static FoodBuff.Clock clock = new FoodBuff.Clock();
    private static final Map<String, ItemStack> icons = new HashMap<>();
    private static String dish;
    private static int ticks;

    private FoodBuffTimer() {}

    public static void tick(MinecraftClient client) {
        if (!foodBuffTimerEnabled || WorldIdentification.world != PlayerWorld.ISLE || client.player == null) {
            dish = null;
            return;
        }
        if (ticks++ % READ_EVERY_TICKS != 0) return;
        FoodBuff.Reading reading = FoodBuff.read(TabListReader.lines(client));
        if (reading == null) {
            dish = null;
            return;
        }
        dish = reading.dish();
        clock.update(reading.dish(), reading.minutes(), Util.getMeasuringTimeMs());
    }

    private static final String SAMPLE_DISH = "Moleberry Smoothie";

    /** The dish line, stuck under the scoreboard by default (the HUD editor can free it). */
    public static final HudElement ELEMENT = new HudElement("food_buff", "Food Buff",
        new HudPlacement(HudAnchor.END, HudAnchor.CENTER, 4, 0)) {
        @Override public boolean enabled() { return foodBuffTimerEnabled; }
        @Override public boolean active(MinecraftClient client) {
            if (dish == null || client.options.hudHidden) return false;
            if (!foodBuffTimerEnabled || FeatureFlags.isKilled("food_buff_timer")) return false;
            return !clock.done(Util.getMeasuringTimeMs());
        }
        @Override public boolean belowScoreboard() { return true; }
        @Override public Size measure(boolean preview) {
            Line l = line(preview);
            return new Size(Math.max(l.width(), minWidth()), ICON);
        }
        @Override public void draw(DrawContext ctx, Frame f) {
            Line l = line(f.preview());
            int w = Math.max(l.width(), minWidth());
            int textY = (ICON - Fonts.height(Fonts.BODY)) / 2;
            ctx.getMatrices().pushMatrix();
            try {
                ctx.getMatrices().scale(ICON / 16f, ICON / 16f);
                ctx.drawItem(icon(MinecraftClient.getInstance(), l.dish()), 0, 0);
            } finally {
                ctx.getMatrices().popMatrix();
            }
            Fonts.drawHud(ctx, l.dish(), ICON + GAP, textY, Theme.HUD_TEXT);
            if (!l.time().isEmpty()) drawTime(ctx, l.time(), w, textY);
        }
    };

    /** What the line says right now: the dish, its time, and the width of the time's slots. */
    private record Line(String dish, String time, int timeW) {
        int width() { return ICON + GAP + Fonts.hudWidth(dish) + (time.isEmpty() ? 0 : GAP + timeW); }
    }

    private static Line line(boolean preview) {
        long ms = Util.getMeasuringTimeMs();
        boolean live = dish != null && !clock.done(ms);
        String d = live ? dish : SAMPLE_DISH;
        String time = live ? clock.text(ms) : "4:59";
        int timeW = time.isEmpty() ? 0 : timeWidth(time, c -> Fonts.hudWidth(String.valueOf(c)));
        return new Line(d, time, timeW);
    }

    /** As wide as the scoreboard's text while it sits under it, so the time lines up with the
     * scoreboard's right edge the way it always did. */
    private static int minWidth() {
        int[] sb = HudLayout.scoreboard();
        if (sb == null || !HudLayout.stuck(ELEMENT)) return 0;
        return sb[2] - INSET_LEFT - INSET_RIGHT;
    }

    /** The width of one character in the font, for {@link #cellXs}. */
    interface CharWidth { int of(char c); }

    private static final String DIGITS = "0123456789";
    private static final char ESTIMATE = '~';

    /** A digit's slot is as wide as the widest digit, so "1:11" and "2:47" line up exactly. */
    private static int cellWidth(char c, CharWidth w) {
        if (DIGITS.indexOf(c) < 0) return w.of(c);
        int widest = 0;
        for (char d : DIGITS.toCharArray()) widest = Math.max(widest, w.of(d));
        return widest;
    }

    private static String digitsOf(String time) {
        return time.startsWith(String.valueOf(ESTIMATE)) ? time.substring(1) : time;
    }

    /** The time's width: its slots, plus one for the "~" that is kept whether it shows or not. */
    static int timeWidth(String time, CharWidth w) {
        int total = w.of(ESTIMATE);
        for (char c : digitsOf(time).toCharArray()) total += cellWidth(c, w);
        return total;
    }

    /** Left x of each character's slot (the "~" not included), the last one ending at
     * {@code right}. The same for every time of the same shape, so the seconds tick in place. */
    static int[] cellXs(String time, int right, CharWidth w) {
        String digits = digitsOf(time);
        int[] xs = new int[digits.length()];
        int x = right;
        for (int i = digits.length() - 1; i >= 0; i--) {
            x -= cellWidth(digits.charAt(i), w);
            xs[i] = x;
        }
        return xs;
    }

    /** Each character centred in its slot; the "~" in the slot before the first, while the time
     * is an estimate. */
    private static void drawTime(DrawContext ctx, String time, int right, int y) {
        CharWidth w = c -> Fonts.hudWidth(String.valueOf(c));
        String digits = digitsOf(time);
        int[] xs = cellXs(time, right, w);
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            int x = xs[i] + (cellWidth(c, w) - w.of(c)) / 2;
            Fonts.drawHud(ctx, String.valueOf(c), x, y, Theme.HUD_GOOD);
        }
        if (!digits.equals(time)) {
            int first = xs.length > 0 ? xs[0] : right;
            Fonts.drawHud(ctx, String.valueOf(ESTIMATE), first - w.of(ESTIMATE), y, Theme.HUD_GOOD);
        }
    }

    /** Icon and name from the scoreboard's left, the time against its right; when they do not
     * fit that width, the line grows to the left - the name is never cut. */
    static Layout layout(int sbX, int sbWidth, int nameW, int timeW) {
        int right = sbX + sbWidth - INSET_RIGHT;
        int timeX = right - timeW;
        int iconX = Math.min(sbX + INSET_LEFT, timeX - GAP - nameW - GAP - ICON);
        return new Layout(iconX, iconX + ICON + GAP, timeX);
    }

    /** The dish's model from the pack, looked up once per dish; bread if the pack has none. */
    private static ItemStack icon(MinecraftClient client, String dish) {
        return icons.computeIfAbsent(dish, d -> {
            String name = FoodBuff.modelName(d);
            for (String folder : MODEL_FOLDERS) {
                Identifier model = Identifier.of("isles", folder + name);
                Identifier file = Identifier.of("isles", "items/" + folder + name + ".json");
                if (client.getResourceManager().getResource(file).isPresent()) {
                    ItemStack stack = new ItemStack(Items.PAPER);
                    stack.set(DataComponentTypes.ITEM_MODEL, model);
                    return stack;
                }
            }
            return new ItemStack(Items.BREAD);
        });
    }

    /** On joining: time passed while away, so a lower minute count is not a live drop - the
     * clock starts over as an estimate. */
    public static void reset() {
        dish = null;
        clock = new FoodBuff.Clock();
        icons.clear();
    }
}
