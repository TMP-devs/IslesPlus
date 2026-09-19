package com.islesplus.ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/** Silkscreen text: styled draws/measures with automatic fallback to the vanilla font
 * if the resource pack font failed to load. */
public final class Fonts {
    private Fonts() {}
    public static final Identifier SILKSCREEN = Identifier.of("islesplus", "silkscreen");
    public static final float TITLE = 1.25f, BODY = 1.0f, SMALL = 0.75f;

    /** Height of one glyph row at scale 1, in pixels. Text is vertically centred in a box by
     * offsetting it half of {@link #height(float)} — never by a bare literal 8. */
    public static final int GLYPH_H = 8;

    /** Glyph height at the given draw scale, rounded to whole pixels. */
    public static int height(float scale) { return Math.round(GLYPH_H * snap(scale)); }

    /**
     * Silkscreen is a pixel font: it only looks right when one font pixel covers a WHOLE number of
     * screen pixels. A requested scale (0.75, 1.25...) is therefore snapped to the nearest multiple
     * of 1/guiScale, e.g. at GUI scale 3: 0.75 -> 2/3 and 1.25 -> 4/3. Without this the glyph
     * columns come out uneven widths and the text looks smeared next to scale-1 text.
     */
    public static float snap(float scale) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return scale;
        int k = Math.max(1, (int) Math.round(mc.getWindow().getScaleFactor()));
        return Math.max(1, Math.round(scale * k)) / (float) k;
    }

    private static final Style STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(SILKSCREEN));
    /**
     * The same typeface rasterised at 24x instead of 8x, used whenever text is drawn at a size that
     * is not a whole multiple of body size (1.5x at GUI scale 2, 1.33x at scale 3...). Glyph
     * textures are sampled without smoothing, so they only come out even when the texture height
     * divides by the on-screen height: 64 texels into 24 screen pixels does not (lumpy R and P),
     * 192 texels does - and 192 also divides for 1x, 2x and 4x at GUI scales 2 and 3.
     */
    public static final Identifier SILKSCREEN_HD = Identifier.of("islesplus", "silkscreen_hd");
    private static final Style STYLE_HD = Style.EMPTY.withFont(new StyleSpriteSource.Font(SILKSCREEN_HD));
    /** How long a fallback decision is trusted before it is measured again. */
    private static final long RECHECK_MS = 1000L;

    private static Boolean fontAvailable;
    private static long checkedAtMs;

    /** Re-check the font on the next call; use when a screen opens. */
    public static void resetFallbackCheck() { fontAvailable = null; WRAP_CACHE.clear(); }

    private static TextRenderer renderer() { return MinecraftClient.getInstance().textRenderer; }

    /** Whether the Silkscreen resource-pack font is loaded right now.
     * <p>The answer is cached, but only for {@link #RECHECK_MS}: the in-inventory search bar and
     * calculator draw through Fonts without ever passing through a screen's init(), so they have
     * no {@link #resetFallbackCheck()} hook, and a resource reload while one of them is on screen
     * would otherwise leave a stale "unavailable" answer in place for the rest of the session. */
    private static boolean fontAvailable() {
        long now = Util.getMeasuringTimeMs();
        if (fontAvailable == null || now - checkedAtMs >= RECHECK_MS) {
            fontAvailable = renderer().getWidth(Text.literal("A").setStyle(STYLE)) != 0;
            checkedAtMs = now;
        }
        return fontAvailable;
    }

    /** Styled literal, falling back to the unstyled vanilla font if Silkscreen failed to load.
     * A leading "~" (our marker for "this is an estimate") is drawn as the estimate icon. */
    public static Text of(String s) {
        return fontAvailable() ? styled(s, STYLE) : Text.literal(s);
    }

    /**
     * The estimate icon: a single pixel wave (one low pixel, two high, two low, one high), sitting
     * on the third and fourth rows of Silkscreen's five-row digits. It lives in a one-glyph bitmap font
     * (font/estimate.json) mapped to "~", because Silkscreen has no such sign and its own tilde is a
     * tiny raised squiggle. Code keeps writing "~" in front of an estimate; every Isles+ text path
     * turns a LEADING "~" into the icon. Notes for whoever touches the font file: the game refuses a
     * bitmap glyph whose "ascent" exceeds its "height" (the whole font then fails and the glyph shows
     * as an empty box); "ascent" 5 is level with the digits' top row and each -1 is one row lower; it is 3. The icon is never bold - a bitmap glyph thickens by a whole pixel, the TTF digits
     * by almost nothing. Its own advance leaves the usual 1 px gap to the first digit.
     */
    public static final Identifier ESTIMATE = Identifier.of("islesplus", "estimate");
    public static final String ESTIMATE_MARK = "~";
    private static final Style ESTIMATE_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(ESTIMATE)).withBold(false);

    /**
     * The one place Isles+ text is assembled. Silkscreen draws most of it; two characters are
     * swapped for our own pixel glyphs: a LEADING "~" becomes the estimate icon, and every "/"
     * comes from its own smooth-stroke slash font (see {@link #SLASH}) - Silkscreen's slash is a
     * wide five-step diagonal that reads like a bracket at body size. The slash is never
     * bold (a bitmap glyph thickens by a whole pixel).
     */
    private static net.minecraft.text.MutableText styled(String s, Style style) {
        boolean estimate = s.startsWith(ESTIMATE_MARK);
        if (!estimate && s.indexOf('/') < 0) return Text.literal(s).setStyle(style);

        net.minecraft.text.MutableText out = Text.empty();
        int from = 0;
        if (estimate) {
            out.append(Text.literal(ESTIMATE_MARK).setStyle(ESTIMATE_STYLE));
            from = ESTIMATE_MARK.length();
        }
        while (from < s.length()) {
            int slash = s.indexOf('/', from);
            if (slash < 0) slash = s.length();
            if (slash > from) out.append(Text.literal(s.substring(from, slash)).setStyle(style));
            if (slash < s.length()) out.append(Text.literal("/").setStyle(SLASH_STYLE));
            from = slash + 1;
        }
        return out;
    }

    /** Chat symbols Silkscreen lacks, as a two-glyph bitmap font (font/symbols.json), five rows
     * tall and level with Silkscreen's capitals: the diamond line mark and the "go here" arrow. */
    public static final Identifier SYMBOLS = Identifier.of("islesplus", "symbols");
    public static final String DIAMOND = String.valueOf((char) 0x25C6), ARROW = String.valueOf((char) 0x2192);
    private static final Style SYMBOL_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(SYMBOLS));
    /** The slash is the one glyph that deliberately breaks the pixel look: a smooth straight stroke
     * drawn from a vector shape (parallelogram 0,5 1,5 3,0 2,0 in font pixels), rasterised at 16x
     * into font/slash.png and scaled down by the bitmap font to the height of the capitals. No
     * stepped pixel slash read as a "/" at this size - they came out wavy, too long or too wide. */
    public static final Identifier SLASH = Identifier.of("islesplus", "slash");
    private static final Style SLASH_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(SLASH)).withBold(false);

    /** {@link #DIAMOND} or {@link #ARROW} as text; falls back to the plain character. */
    public static net.minecraft.text.MutableText symbol(String s) {
        return fontAvailable() ? Text.literal(s).setStyle(SYMBOL_STYLE) : Text.literal(s);
    }

    /** {@link #of} as a mutable text, for callers that go on to add colour or bold. */
    public static net.minecraft.text.MutableText ofEstimate(String s) {
        return fontAvailable() ? styled(s, STYLE) : Text.literal(s);
    }

    /** Styled literal for drawing at {@code snappedScale}: the 24x face for in-between sizes. */
    private static Text ofScaled(String s, float snappedScale) {
        if (!fontAvailable()) return Text.literal(s);
        // The 8x face is 64 texels tall: fine whenever that divides evenly into (or by) the height
        // on screen. Otherwise (1.5x, 6x at GUI scale 2...) use the 24x face, 192 texels.
        MinecraftClient mc = MinecraftClient.getInstance();
        int gui = mc == null || mc.getWindow() == null ? 1 : Math.max(1, (int) Math.round(mc.getWindow().getScaleFactor()));
        int onScreen = Math.max(1, Math.round(GLYPH_H * snappedScale * gui));
        boolean baseIsEven = 64 % onScreen == 0 || onScreen % 64 == 0;
        return styled(s, baseIsEven ? STYLE : STYLE_HD);
    }

    public static int width(String s) { return renderer().getWidth(of(s)); }

    public static int width(String s, float scale) { return (int) Math.ceil(width(s) * snap(scale)); }

    public static void draw(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.drawText(renderer(), of(s), x, y, argb, false);
    }

    public static void draw(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        scale = snap(scale);
        if (scale == 1f) { draw(ctx, s, x, y, argb); return; }
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) x, (float) y);
            ctx.getMatrices().scale(scale, scale);
            ctx.drawText(renderer(), ofScaled(s, scale), 0, 0, argb, false);
        } finally {
            // Never leave the matrix stack unbalanced: everything drawn after this call on the
            // same frame would inherit the scale.
            ctx.getMatrices().popMatrix();
        }
    }

    /** Vanilla Minecraft font, for user-typed text whose letter case matters (Silkscreen is caps-only). */
    public static void drawPlain(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.drawText(renderer(), Text.literal(s), x, y, argb, false);
    }

    public static int plainWidth(String s) { return renderer().getWidth(Text.literal(s)); }

    /** The Isles+ drop shadow: the text colour at a quarter brightness, one TEXT pixel straight
     * down (so {@code scale} GUI pixels), which puts it right under every stroke with no background
     * showing between the two. */
    public static void drawShadowed(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        int drop = Math.max(1, Math.round(snap(scale)));
        draw(ctx, s, x, y + drop, 0xFF000000 | ColorMath.darken(argb & 0xFFFFFF, 0.25f), scale);
        draw(ctx, s, x, y, argb, scale);
    }

    /** Text over the game world with no panel behind it: a full 1 px ink outline (all eight
     * neighbours) under the text, so it stays readable on sky, grass or snow alike. */
    public static void drawHud(DrawContext ctx, String s, int x, int y, int argb) { drawHud(ctx, s, x, y, argb, BODY); }

    public static void drawHud(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) draw(ctx, s, x + dx, y + dy, Theme.HUD_SHADOW, scale);
            }
        }
        draw(ctx, s, x, y, argb, scale);
    }

    // ==============================
    // Signature script (Great Vibes) - the makers' byline only, never UI labels
    // ==============================

    public static final Identifier SCRIPT = Identifier.of("islesplus", "greatvibes");
    /** Em height of the script font in GUI pixels (the "size" in font/greatvibes.json). */
    public static final int SCRIPT_H = 20;
    private static final Style SCRIPT_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(SCRIPT));
    /**
     * The script has hairline strokes, and glyph textures are sampled without smoothing: rendered
     * at 4x and shown at GUI scale 2, every other texel is skipped and the hairlines drop out (the
     * top of the capital S vanished, which read as the letter being cut off). So there is one copy
     * of the font per GUI scale - greatvibes_x1/x2/x3 and the 4x original - and text is drawn with
     * the copy whose texels match screen pixels one to one.
     */
    private static final Style[] SCRIPT_BY_GUI_SCALE = {
        Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("islesplus", "greatvibes_x1"))),
        Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("islesplus", "greatvibes_x2"))),
        Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("islesplus", "greatvibes_x3"))),
        SCRIPT_STYLE
    };

    private static Style scriptStyle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int gui = mc == null || mc.getWindow() == null ? 4 : (int) Math.round(mc.getWindow().getScaleFactor());
        return SCRIPT_BY_GUI_SCALE[Math.max(1, Math.min(4, gui)) - 1];
    }

    /** Whether the script font loaded; callers fall back to Silkscreen when it did not. */
    public static boolean scriptAvailable() {
        return renderer().getWidth(Text.literal("A").setStyle(SCRIPT_STYLE)) != 0;
    }

    public static int scriptWidth(String s) { return renderer().getWidth(Text.literal(s).setStyle(scriptStyle())); }

    public static void drawScript(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.drawText(renderer(), Text.literal(s).setStyle(scriptStyle()), x, y, argb, false);
    }

    /** Width for a column of left-hand labels: the widest of them plus a little air, so none is
     * ever clipped whatever the GUI scale snaps the text size to. */
    public static int labelColumn(float scale, String... labels) {
        int widest = 0;
        for (String label : labels) widest = Math.max(widest, width(label, scale));
        return widest + 3;
    }

    public static void drawCentered(DrawContext ctx, String s, int cx, int y, int argb, float scale) {
        int w = width(s, scale);
        draw(ctx, s, cx - w / 2, y, argb, scale);
    }

    /** Wrapping is asked for on every layout pass of every frame, with the same few inputs, and a
     * wrap measures the font once per word. Results are remembered per (text, width, font state);
     * the map is simply emptied if it ever grows large (dynamic text such as counters). */
    private static final java.util.Map<String, List<String>> WRAP_CACHE = new java.util.HashMap<>();
    private static final int WRAP_CACHE_MAX = 512;

    /** Greedy word wrap on spaces; a single word longer than maxWidth is left unbroken on its own
     * line. The returned list is shared and must not be modified. */
    public static List<String> wrap(String s, int maxWidth) {
        if (s == null || s.isEmpty()) return List.of();
        String key = maxWidth + (fontAvailable() ? "|s|" : "|v|") + s;
        List<String> cached = WRAP_CACHE.get(key);
        if (cached != null) return cached;
        if (WRAP_CACHE.size() >= WRAP_CACHE_MAX) WRAP_CACHE.clear();
        List<String> fresh = List.copyOf(wrapUncached(s, maxWidth));
        WRAP_CACHE.put(key, fresh);
        return fresh;
    }

    private static List<String> wrapUncached(String s, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = s.trim().split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (cur.length() == 0) { cur.append(word); continue; }
            String trial = cur + " " + word;
            if (width(trial) <= maxWidth) cur = new StringBuilder(trial);
            else { lines.add(cur.toString()); cur = new StringBuilder(word); }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    /** Trims trailing characters and appends "..." until the string fits maxWidth; "..." at minimum. */
    public static String ellipsize(String s, int maxWidth) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        if (width(s) <= maxWidth) return s;
        String ellipsis = "...";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && width(sb + ellipsis) > maxWidth) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.length() == 0 ? ellipsis : sb + ellipsis;
    }
}
