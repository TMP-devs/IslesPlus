package com.islesplus.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TypeFaceTest {
    // ---- which face ----------------------------------------------------------------------------

    @Test void cardHeadingsAreAlegreyaBold() {
        assertEquals(TypeFace.Face.HEADING, TypeFace.pick(TypeFace.TITLE, TypeFace.Kind.UI));
    }

    @Test void interfaceTextIsAlegreyaSansMedium() {
        assertEquals(TypeFace.Face.UI, TypeFace.pick(1f, TypeFace.Kind.UI));
        assertEquals(TypeFace.Face.UI, TypeFace.pick(1.25f, TypeFace.Kind.UI));
    }

    /** Labels, chips, tags: interface text a size down, still the medium weight. */
    @Test void smallTextIsAlegreyaSansMedium() {
        assertEquals(TypeFace.Face.SMALL, TypeFace.pick(TypeFace.SMALL, TypeFace.Kind.UI));
        assertEquals(TypeFace.Face.SMALL, TypeFace.pick(0.8f, TypeFace.Kind.UI));
    }

    /** A dialog title is a heading at body size. */
    @Test void aHeadingAtAnySizeIsAlegreya() {
        assertEquals(TypeFace.Face.HEADING, TypeFace.pick(1f, TypeFace.Kind.HEADING));
    }

    /** Over-the-world text is in the game's font; inside /ip it would be a bold control. */
    @Test void hudTextInsideIpIsBold() {
        assertEquals(TypeFace.Face.CONTROL, TypeFace.pick(2f, TypeFace.Kind.HUD));
    }

    @Test void controlsAreAlegreyaSansBold() {
        assertEquals(TypeFace.Face.CONTROL, TypeFace.pick(TypeFace.SMALL, TypeFace.Kind.CONTROL));
        assertEquals("alegreya_sans_700", TypeFace.Face.CONTROL.file);
    }

    @Test void typedTextIsPixelify() {
        assertEquals(TypeFace.Face.FIELD, TypeFace.pick(1f, TypeFace.Kind.FIELD));
    }

    // ---- sizes: the web spec at half size (16 px body = 8 GUI px) -------------------------------

    @Test void sizesFollowTheWebSpecAtHalf() {
        assertEquals(8f, TypeFace.em(TypeFace.BODY));
        assertEquals(11.5f, TypeFace.em(TypeFace.TITLE));   // card headings, 23 px
        assertEquals(7f, TypeFace.em(TypeFace.SMALL));      // smaller interface text, 14 px
    }

    // ---- which font file ------------------------------------------------------------------------

    @Test void anExactSizeIsDrawnAsIs() {
        TypeFace.Choice c = TypeFace.choose(TypeFace.Face.UI, 8f, 2);
        assertEquals("islesplus:alegreya_sans_500_80_x2", c.fontId());
        assertEquals(1f, c.stretch());
    }

    /** A size without a font file of its own borrows the nearest and stretches it. */
    @Test void anOddSizeStretchesTheNearest() {
        TypeFace.Choice c = TypeFace.choose(TypeFace.Face.UI, 8.5f, 3);
        assertEquals("islesplus:alegreya_sans_500_80_x3", c.fontId());
        assertEquals(8.5f / 8f, c.stretch(), 1e-6);
    }

    /** One copy per GUI scale, so a texel is exactly one screen pixel; past the last, the last. */
    @Test void guiScalesOutOfRangeUseTheNearestCopy() {
        assertTrue(TypeFace.choose(TypeFace.Face.UI, 8f, 0).fontId().endsWith("_x1"));
        assertTrue(TypeFace.choose(TypeFace.Face.UI, 8f, 9).fontId().endsWith("_x" + TypeFace.MAX_GUI));
    }

    /** Every font id Fonts can ask for has its json, sized and oversampled to match. */
    @Test void everyFontFileExists() throws Exception {
        for (TypeFace.Face face : TypeFace.Face.values()) {
            assertNotNull(resource("assets/islesplus/font/" + face.file + ".ttf"), face.file + ".ttf");
            for (float size : face.sizes) {
                for (int gui = 1; gui <= TypeFace.MAX_GUI; gui++) {
                    String id = TypeFace.choose(face, size, gui).fontId();
                    String path = "assets/islesplus/font/" + id.substring("islesplus:".length()) + ".json";
                    try (InputStream in = resource(path)) {
                        assertNotNull(in, path);
                        JsonObject ttf = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonArray("providers").get(0).getAsJsonObject();
                        assertEquals("islesplus:" + face.file + ".ttf", ttf.get("file").getAsString(), path);
                        assertEquals(size, ttf.get("size").getAsFloat(), 1e-6, path);
                        assertEquals(gui, ttf.get("oversample").getAsFloat(), 1e-6, path);
                    }
                }
            }
        }
    }

    private static InputStream resource(String path) {
        return TypeFaceTest.class.getClassLoader().getResourceAsStream(path);
    }
}
