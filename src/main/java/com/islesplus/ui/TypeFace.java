package com.islesplus.ui;

/**
 * Which typeface and font file a piece of Isles+ text is drawn with. No Minecraft types, so it
 * is unit tested; {@link Fonts} does the drawing.
 * <p>
 * Only the /ip UI uses these faces ({@link Fonts#islesUi}); everything else is in the game's own
 * font. The type system is the website's, at half size (16 px web body text = 8 GUI px, which at the
 * usual GUI scale 2 is 16 screen pixels):
 * <ul>
 * <li>Alegreya 700 for headings and titles - card headings 23 px, so 11.5.</li>
 * <li>Alegreya Sans for everything else: 500 for interface text (16 px, so 8) and smaller
 *     interface text (14 px, so 7 - labels, chips, tags); 700 for controls (toggle labels,
 *     buttons).</li>
 * <li>Pixelify Sans in text fields only.</li>
 * </ul>
 * Minecraft samples glyph textures without smoothing, so text is only crisp when one texel is one
 * screen pixel. Each face therefore has a font file per size it is drawn at and per GUI scale
 * ({@code oversample} = GUI scale): {@code font/alegreya_sans_500_80_x2.json} is Alegreya Sans
 * 500, 8 GUI px, for GUI scale 2. A size with no file of its own borrows the nearest one and is
 * stretched to fit.
 */
public final class TypeFace {
    /** Scales, relative to body text. */
    public static final float TITLE = 23f / 16f, BODY = 1.0f, SMALL = 7f / 8f;
    /** Em size of body text in GUI px. */
    public static final float BODY_EM = 8f;
    public static final int MAX_GUI = 6;

    /** What the text is, which decides its face along with its size. */
    public enum Kind { UI, HEADING, CONTROL, HUD, FIELD }

    public enum Face {
        HEADING("alegreya_700", 8f, 11.5f),
        UI("alegreya_sans_500", 8f, 10f),
        /** 6.4 is the rank badge. */
        SMALL("alegreya_sans_500", 6.4f, 7f),
        /** Toggle labels and buttons: bold, so they read on a coloured fill. */
        CONTROL("alegreya_sans_700", 7f, 8f),
        FIELD("pixelify_sans_400", 8f);

        /** The ttf in assets/islesplus/font, and the stem of its json files. */
        public final String file;
        /** Em sizes in GUI px that have font files. */
        public final float[] sizes;

        Face(String file, float... sizes) {
            this.file = file;
            this.sizes = sizes;
        }
    }

    private TypeFace() {}

    public static float em(float scale) {
        return BODY_EM * scale;
    }

    public static Face pick(float scale, Kind kind) {
        return switch (kind) {
            case HEADING -> Face.HEADING;
            case FIELD -> Face.FIELD;
            case CONTROL -> Face.CONTROL;
            // over-the-world text is never part of the /ip UI; if it is ever drawn there, as a control
            case HUD -> Face.CONTROL;
            case UI -> scale >= 1.4f ? Face.HEADING : scale < 0.95f ? Face.SMALL : Face.UI;
        };
    }

    /** A font file, and how much to stretch it to reach the size asked for (1 when it is exact). */
    public record Choice(String fontId, float stretch) {}

    public static Choice choose(Face face, float em, int guiScale) {
        float best = face.sizes[0];
        for (float size : face.sizes) {
            if (Math.abs(Math.log(size / em)) < Math.abs(Math.log(best / em))) best = size;
        }
        int gui = Math.max(1, Math.min(MAX_GUI, guiScale));
        return new Choice("islesplus:" + face.file + "_" + Math.round(best * 10) + "_x" + gui, em / best);
    }
}
