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

/**
 * Isles+ text. Inside the /ip UI ({@link #islesUi}): Alegreya for headings, Alegreya Sans for
 * everything else, Pixelify Sans in text fields ({@link TypeFace} picks which, and which font file).
 * Everywhere else - the HUD, chat, world tags, the title screen, the inventory search bar - the
 * game's own font, at whole-pixel sizes. The vanilla font is also the fallback if the resource pack
 * fonts failed to load.
 */
public final class Fonts {
    private Fonts() {}
    public static final float TITLE = TypeFace.TITLE, BODY = TypeFace.BODY, SMALL = TypeFace.SMALL;

    /** Height of the text box at scale 1, in GUI pixels: body text's em. Text is vertically centred
     * in a box by offsetting it half of {@link #height(float)} - never by a bare literal 8. Capitals
     * fill most of it; descenders (g, y, p) hang a pixel or two below. */
    public static final int GLYPH_H = 8;

    /** Text box height at the given draw scale, rounded to whole pixels. */
    public static int height(float scale) { return Math.round(GLYPH_H * (islesFaces() ? scale : snap(scale))); }

    /**
     * The vanilla font is a pixel font: it only looks right when one font pixel covers a WHOLE
     * number of screen pixels. Outside the /ip UI a scale is snapped to the nearest multiple of
     * 1/guiScale (at GUI scale 3: 0.75 -> 2/3, 1.25 -> 4/3).
     */
    public static float snap(float scale) {
        int k = guiScale();
        return Math.max(1, Math.round(scale * k)) / (float) k;
    }

    /** Nesting depth of {@link #islesUi}: above 0, text is for the /ip UI. */
    private static int islesUiDepth;

    /** Runs {@code body} as /ip UI: its text is drawn and measured in the Isles+ faces. The /ip
     * screens wrap their init, render and input in this, and nothing else does. */
    public static void islesUi(Runnable body) {
        islesUiDepth++;
        try { body.run(); } finally { islesUiDepth--; }
    }

    public static <T> T islesUi(java.util.function.Supplier<T> body) {
        islesUiDepth++;
        try { return body.get(); } finally { islesUiDepth--; }
    }

    private static boolean islesFaces() { return islesUiDepth > 0 && fontAvailable(); }

    /** Where the game puts a TrueType glyph's baseline, below the y it is drawn at, whatever its
     * size (UploadableGlyph: top = 7 - ascent). */
    private static final float BASELINE = 7f;

    /** How long a fallback decision is trusted before it is measured again. */
    private static final long RECHECK_MS = 1000L;

    private static Boolean fontAvailable;
    private static long checkedAtMs;

    /** Re-check the font on the next call; use when a screen opens. */
    public static void resetFallbackCheck() { fontAvailable = null; WRAP_CACHE.clear(); }

    private static TextRenderer renderer() { return MinecraftClient.getInstance().textRenderer; }

    private static int guiScale() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc == null || mc.getWindow() == null ? 2 : Math.max(1, (int) Math.round(mc.getWindow().getScaleFactor()));
    }

    /** Whether the resource-pack fonts are loaded right now.
     * <p>The answer is cached, but only for {@link #RECHECK_MS}: the in-inventory search bar and
     * calculator draw through Fonts without ever passing through a screen's init(), so they have
     * no {@link #resetFallbackCheck()} hook, and a resource reload while one of them is on screen
     * would otherwise leave a stale "unavailable" answer in place for the rest of the session. */
    private static boolean fontAvailable() {
        long now = Util.getMeasuringTimeMs();
        if (fontAvailable == null || now - checkedAtMs >= RECHECK_MS) {
            Style body = style(TypeFace.choose(TypeFace.Face.UI, TypeFace.BODY_EM, guiScale()));
            // the vanilla font is the fallback inside every Isles+ font file, so compare against it
            fontAvailable = renderer().getWidth(Text.literal("m").setStyle(body)) != renderer().getWidth(Text.literal("m"));
            checkedAtMs = now;
        }
        return fontAvailable;
    }

    private static Style style(TypeFace.Choice choice) {
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of(choice.fontId())));
    }

    /** The text in its face and font file, and how much the file has to be stretched. */
    private record Run(Text text, float stretch) {}

    private static Run run(String s, float scale, TypeFace.Kind kind) {
        if (!islesFaces()) return new Run(Text.literal(s), snap(scale));
        float em = TypeFace.em(scale);
        TypeFace.Choice c = TypeFace.choose(TypeFace.pick(scale, kind), em, guiScale());
        return new Run(Text.literal(s).setStyle(style(c)), c.stretch());
    }

    /** Body text as a text component, for chat lines and other text the game lays out itself. */
    public static Text of(String s) {
        return run(s, BODY, TypeFace.Kind.UI).text();
    }

    /** Marks a time as an estimate; the face's own tilde. */
    public static final String ESTIMATE_MARK = "~";

    /** Symbols drawn from the body face, which has them: the diamond line mark and the "go here"
     * arrow. */
    public static final String DIAMOND = String.valueOf((char) 0x25C6), ARROW = String.valueOf((char) 0x2192);

    /** {@link #DIAMOND} or {@link #ARROW} as text. */
    public static net.minecraft.text.MutableText symbol(String s) {
        return run(s, BODY, TypeFace.Kind.UI).text().copy();
    }

    /** {@link #of} as a mutable text, for callers that go on to add colour or bold. */
    public static net.minecraft.text.MutableText ofEstimate(String s) {
        return run(s, BODY, TypeFace.Kind.UI).text().copy();
    }

    private static int measure(String s, float scale, TypeFace.Kind kind) {
        Run r = run(s, scale, kind);
        return (int) Math.ceil(renderer().getWidth(r.text()) * r.stretch());
    }

    public static int width(String s) { return width(s, BODY); }

    public static int width(String s, float scale) { return measure(s, scale, TypeFace.Kind.UI); }

    /** Width of a heading: Alegreya at any size (a dialog title, say). */
    public static int headingWidth(String s, float scale) { return measure(s, scale, TypeFace.Kind.HEADING); }

    private static void drawRun(DrawContext ctx, String s, int x, int y, int argb, float scale, TypeFace.Kind kind) {
        Run r = run(s, scale, kind);
        // the file puts the baseline at BASELINE, stretched; it belongs where scaling the body text
        // would put it, so every size sits in its box the way body text does
        // (the vanilla font keeps its baseline where the matrix puts it: on the pixel grid)
        float dy = islesFaces() ? BASELINE * scale - BASELINE * r.stretch() : 0f;
        if (r.stretch() == 1f && dy == 0f) {
            ctx.drawText(renderer(), r.text(), x, y, argb, false);
            return;
        }
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) x, y + dy);
            if (r.stretch() != 1f) ctx.getMatrices().scale(r.stretch(), r.stretch());
            ctx.drawText(renderer(), r.text(), 0, 0, argb, false);
        } finally {
            // Never leave the matrix stack unbalanced: everything drawn after this call on the
            // same frame would inherit the scale.
            ctx.getMatrices().popMatrix();
        }
    }

    public static void draw(DrawContext ctx, String s, int x, int y, int argb) {
        draw(ctx, s, x, y, argb, BODY);
    }

    public static void draw(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        drawRun(ctx, s, x, y, argb, scale, TypeFace.Kind.UI);
    }

    /** A heading in Alegreya at any size: dialog titles, tab labels. */
    public static void drawHeading(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        drawRun(ctx, s, x, y, argb, scale, TypeFace.Kind.HEADING);
    }

    /** A control's label - toggle "On"/"Off", button text - in Alegreya Sans Bold, which reads on
     * a coloured fill where the medium weight goes faint. */
    public static void drawControl(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        drawRun(ctx, s, x, y, argb, scale, TypeFace.Kind.CONTROL);
    }

    public static int controlWidth(String s, float scale) { return measure(s, scale, TypeFace.Kind.CONTROL); }

    public static void drawControlCentered(DrawContext ctx, String s, int cx, int y, int argb, float scale) {
        drawControl(ctx, s, cx - controlWidth(s, scale) / 2, y, argb, scale);
    }

    /** Text typed into a field, in Pixelify Sans. */
    public static void drawField(DrawContext ctx, String s, int x, int y, int argb) {
        drawRun(ctx, s, x, y, argb, BODY, TypeFace.Kind.FIELD);
    }

    public static int fieldWidth(String s) { return measure(s, BODY, TypeFace.Kind.FIELD); }

    /** The Isles+ drop shadow: the text colour at a quarter brightness, one text pixel straight
     * down (so {@code scale} GUI pixels), which puts it right under every stroke with no background
     * showing between the two. Drawn in the over-the-world faces. */
    public static void drawShadowed(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        int drop = Math.max(1, Math.round(scale));
        drawRun(ctx, s, x, y + drop, 0xFF000000 | ColorMath.darken(argb & 0xFFFFFF, 0.25f), scale, TypeFace.Kind.HUD);
        drawRun(ctx, s, x, y, argb, scale, TypeFace.Kind.HUD);
    }

    /** Width of text drawn with {@link #drawHud} or {@link #drawShadowed}. */
    public static int hudWidth(String s, float scale) { return measure(s, scale, TypeFace.Kind.HUD); }

    public static int hudWidth(String s) { return hudWidth(s, BODY); }

    /** Text over the game world with no panel behind it: Alegreya Sans Bold with a full 1 px ink
     * outline (all eight neighbours) under it, so it stays readable on sky, grass or snow alike. */
    public static void drawHud(DrawContext ctx, String s, int x, int y, int argb) { drawHud(ctx, s, x, y, argb, BODY); }

    public static void drawHud(DrawContext ctx, String s, int x, int y, int argb, float scale) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) drawRun(ctx, s, x + dx, y + dy, Theme.HUD_SHADOW, scale, TypeFace.Kind.HUD);
            }
        }
        drawRun(ctx, s, x, y, argb, scale, TypeFace.Kind.HUD);
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

    /** Whether the script font loaded; callers fall back to body text when it did not. */
    public static boolean scriptAvailable() {
        return renderer().getWidth(Text.literal("A").setStyle(SCRIPT_STYLE)) != 0;
    }

    public static int scriptWidth(String s) { return renderer().getWidth(Text.literal(s).setStyle(scriptStyle())); }

    public static void drawScript(DrawContext ctx, String s, int x, int y, int argb) {
        ctx.drawText(renderer(), Text.literal(s).setStyle(scriptStyle()), x, y, argb, false);
    }

    /** Width for a column of left-hand labels: the widest of them plus a little air, so none is
     * ever clipped. */
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
     * wrap measures the font once per word. Results are remembered per (text, width, size, font
     * state); the map is simply emptied if it ever grows large (dynamic text such as counters). */
    private static final java.util.Map<String, List<String>> WRAP_CACHE = new java.util.HashMap<>();
    private static final int WRAP_CACHE_MAX = 512;

    public static List<String> wrap(String s, int maxWidth) { return wrap(s, maxWidth, BODY); }

    /** Greedy word wrap on spaces, measured at {@code scale}; a single word longer than maxWidth is
     * left unbroken on its own line. The returned list is shared and must not be modified. */
    public static List<String> wrap(String s, int maxWidth, float scale) {
        if (s == null || s.isEmpty()) return List.of();
        String key = maxWidth + "|" + scale + "|" + guiScale() + (islesFaces() ? "|f|" : "|v|") + s;
        List<String> cached = WRAP_CACHE.get(key);
        if (cached != null) return cached;
        if (WRAP_CACHE.size() >= WRAP_CACHE_MAX) WRAP_CACHE.clear();
        List<String> fresh = List.copyOf(wrapUncached(s, maxWidth, scale));
        WRAP_CACHE.put(key, fresh);
        return fresh;
    }

    private static List<String> wrapUncached(String s, int maxWidth, float scale) {
        List<String> lines = new ArrayList<>();
        String[] words = s.trim().split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (cur.length() == 0) { cur.append(word); continue; }
            String trial = cur + " " + word;
            if (width(trial, scale) <= maxWidth) cur = new StringBuilder(trial);
            else { lines.add(cur.toString()); cur = new StringBuilder(word); }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    public static String ellipsize(String s, int maxWidth) { return ellipsize(s, maxWidth, BODY); }

    /** Trims trailing characters and appends "..." until the string fits maxWidth at
     * {@code scale}; "..." at minimum. */
    public static String ellipsize(String s, int maxWidth, float scale) {
        return ellipsize(s, maxWidth, scale, TypeFace.Kind.UI);
    }

    /** {@link #ellipsize} for a heading ({@link #drawHeading}). */
    public static String ellipsizeHeading(String s, int maxWidth, float scale) {
        return ellipsize(s, maxWidth, scale, TypeFace.Kind.HEADING);
    }

    private static String ellipsize(String s, int maxWidth, float scale, TypeFace.Kind kind) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;
        if (measure(s, scale, kind) <= maxWidth) return s;
        String ellipsis = "...";
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && measure(sb + ellipsis, scale, kind) > maxWidth) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.length() == 0 ? ellipsis : sb + ellipsis;
    }
}
