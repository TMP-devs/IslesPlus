package com.islesplus.screen.islesscreen;

import com.islesplus.sound.ModSounds;
import com.islesplus.sound.SoundConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.islesplus.screen.islesscreen.ScreenColors.*;

/**
 * Floating overlay panel for editing a SoundConfig (sound, volume, pitch).
 * Rendered on top of everything at the end of IslesScreen.render().
 * All state is static — only one editor can be open at a time.
 */
public final class SoundEditorOverlay {

    private static final int W = 290;
    private static final int H = 140;

    // ---- Persistent state ----
    private static boolean open = false;
    private static Supplier<SoundConfig> getter;
    private static Consumer<SoundConfig> setter;
    private static SoundConfig defaults;
    private static Runnable onSave;
    private static SoundConfig temp;
    private static int soundIdx = 0;

    // ---- Slider drag state ----
    private static boolean volDragging   = false;
    private static boolean pitchDragging = false;

    // ---- Geometry cached during draw() — used in onClick/drag ----
    private static int panelX, panelY;
    private static int prevBtnX, nextBtnX, soundRowY;
    private static int volTrackX, volTrackW, volRowY;
    private static int pitchTrackX, pitchTrackW, pitchRowY;
    private static int btn1X, btn2X, btn3X, btnY, btnW;

    private SoundEditorOverlay() {}

    // ----------------------------------------------------------------
    // API
    // ----------------------------------------------------------------

    public static void open(Supplier<SoundConfig> getter, Consumer<SoundConfig> setter,
                            SoundConfig defaults, Runnable onSave) {
        SoundEditorOverlay.getter   = getter;
        SoundEditorOverlay.setter   = setter;
        SoundEditorOverlay.defaults = defaults;
        SoundEditorOverlay.onSave   = onSave;
        SoundEditorOverlay.temp     = getter.get().copy();
        SoundEditorOverlay.soundIdx = findSoundIdx(temp.soundId);
        SoundEditorOverlay.open     = true;
    }

    public static boolean isOpen() { return open; }

    public static void close() {
        open         = false;
        volDragging  = false;
        pitchDragging = false;
    }

    // ----------------------------------------------------------------
    // Rendering
    // ----------------------------------------------------------------

    public static void draw(DrawContext ctx, TextRenderer tr, int screenW, int screenH, int mouseX, int mouseY) {
        if (!open) return;

        panelX = (screenW - W) / 2;
        panelY = (screenH - H) / 2;

        // Panel background + border glow
        ctx.fill(panelX - 2, panelY - 2, panelX + W + 2, panelY + H + 2, ACCENT_SOFT);
        ctx.fill(panelX, panelY, panelX + W, panelY + H, 0xFF141414);

        // ── Title ───────────────────────────────────────────────────
        ctx.drawText(tr, Text.literal("Edit Sound"), panelX + 10, panelY + 8, TEXT_PRIMARY, false);

        // [×] close button
        int closeX = panelX + W - 20;
        int closeY = panelY + 4;
        boolean closeHov = isIn(mouseX, mouseY, closeX, closeY, 16, 16);
        ctx.fill(closeX, closeY, closeX + 16, closeY + 16, closeHov ? 0xFF5C1A1A : PANEL_SOFT);
        ctx.fill(closeX, closeY, closeX + 16, closeY + 1, BORDER);
        ctx.drawText(tr, Text.literal("×"), closeX + 4, closeY + 4, closeHov ? 0xFFFF6B6B : TEXT_MUTED, false);

        // Separator
        ctx.fill(panelX + 6, panelY + 24, panelX + W - 6, panelY + 25, BORDER);

        // ── Sound picker row ─────────────────────────────────────────
        soundRowY = panelY + 32;
        ctx.drawText(tr, Text.literal("Sound:"), panelX + 10, soundRowY + 3, TEXT_MUTED, false);

        int soundLabelW = tr.getWidth("Sound:") + 8;
        int arrowW      = 20;

        prevBtnX = panelX + soundLabelW;
        nextBtnX = panelX + W - 10 - arrowW;

        boolean prevHov = isIn(mouseX, mouseY, prevBtnX, soundRowY, arrowW, 14);
        ctx.fill(prevBtnX, soundRowY, prevBtnX + arrowW, soundRowY + 14, prevHov ? PANEL_HOVER : PANEL_SOFT);
        ctx.fill(prevBtnX, soundRowY, prevBtnX + arrowW, soundRowY + 1, BORDER);
        ctx.drawText(tr, Text.literal("◄"), prevBtnX + 5, soundRowY + 3, TEXT_MUTED, false);

        boolean nextHov = isIn(mouseX, mouseY, nextBtnX, soundRowY, arrowW, 14);
        ctx.fill(nextBtnX, soundRowY, nextBtnX + arrowW, soundRowY + 14, nextHov ? PANEL_HOVER : PANEL_SOFT);
        ctx.fill(nextBtnX, soundRowY, nextBtnX + arrowW, soundRowY + 1, BORDER);
        ctx.drawText(tr, Text.literal("►"), nextBtnX + 5, soundRowY + 3, TEXT_MUTED, false);

        // Name display box between arrows
        int nameX = prevBtnX + arrowW + 2;
        int nameW = nextBtnX - 2 - nameX;
        ctx.fill(nameX, soundRowY, nameX + nameW, soundRowY + 14, PANEL_SOFT);
        ctx.fill(nameX, soundRowY, nameX + nameW, soundRowY + 1, BORDER);
        String sn = fitText(shortName(temp.soundId), tr, nameW - 8);
        ctx.drawText(tr, Text.literal(sn), nameX + (nameW - tr.getWidth(sn)) / 2, soundRowY + 3, TEXT_PRIMARY, false);

        // ── Volume row ───────────────────────────────────────────────
        volRowY = panelY + 56;
        int volLabelW  = tr.getWidth("Volume:") + 12;
        int valW       = tr.getWidth("100%") + 8;
        volTrackX = panelX + 10 + volLabelW;
        volTrackW = W - 10 - volLabelW - valW - 12;

        ctx.drawText(tr, Text.literal("Volume:"), panelX + 10, volRowY + 3, TEXT_MUTED, false);
        drawSlider(ctx, volTrackX, volRowY, volTrackW, clamp01(temp.volume), ACCENT);
        String volStr = Math.round(temp.volume * 100) + "%";
        ctx.drawText(tr, Text.literal(volStr), panelX + W - 8 - tr.getWidth(volStr), volRowY + 3, TEXT_PRIMARY, false);

        // ── Pitch row ─────────────────────────────────────────────────
        pitchRowY = panelY + 78;
        int pitchLabelW = tr.getWidth("Pitch:") + 12;
        pitchTrackX = panelX + 10 + pitchLabelW;
        pitchTrackW = W - 10 - pitchLabelW - valW - 12;

        ctx.drawText(tr, Text.literal("Pitch:"), panelX + 10, pitchRowY + 3, TEXT_MUTED, false);
        float pitchNorm = clamp01((temp.pitch - 0.5f) / 1.5f);
        drawSlider(ctx, pitchTrackX, pitchRowY, pitchTrackW, pitchNorm, 0xFFF9A825);
        String pitchStr = String.format("%.2f", temp.pitch);
        ctx.drawText(tr, Text.literal(pitchStr), panelX + W - 8 - tr.getWidth(pitchStr), pitchRowY + 3, TEXT_PRIMARY, false);

        // Separator
        ctx.fill(panelX + 6, panelY + 98, panelX + W - 6, panelY + 99, BORDER);

        // ── Buttons ───────────────────────────────────────────────────
        btnY = panelY + 108;
        btnW = (W - 20 - 8) / 3;
        btn1X = panelX + 10;
        btn2X = btn1X + btnW + 4;
        btn3X = btn2X + btnW + 4;

        drawBtn(ctx, tr, btn1X, btnY, btnW, 14, "Test Sound", mouseX, mouseY, false);
        drawBtn(ctx, tr, btn2X, btnY, btnW, 14, "Reset",      mouseX, mouseY, false);
        drawBtn(ctx, tr, btn3X, btnY, btnW, 14, "Save",       mouseX, mouseY, true);
    }

    // ----------------------------------------------------------------
    // Input
    // ----------------------------------------------------------------

    public static boolean onClick(int mouseX, int mouseY) {
        if (!open) return false;

        // Close button
        int closeX = panelX + W - 20;
        if (isIn(mouseX, mouseY, closeX, panelY + 4, 16, 16)) {
            close();
            return true;
        }

        // Click outside — close
        if (!isIn(mouseX, mouseY, panelX, panelY, W, H)) {
            close();
            return true;
        }

        // Prev / Next sound
        if (isIn(mouseX, mouseY, prevBtnX, soundRowY, 20, 14)) {
            soundIdx = (soundIdx - 1 + ModSounds.SOUND_LIST.length) % ModSounds.SOUND_LIST.length;
            temp.soundId = ModSounds.SOUND_LIST[soundIdx];
            return true;
        }
        if (isIn(mouseX, mouseY, nextBtnX, soundRowY, 20, 14)) {
            soundIdx = (soundIdx + 1) % ModSounds.SOUND_LIST.length;
            temp.soundId = ModSounds.SOUND_LIST[soundIdx];
            return true;
        }

        // Volume slider — tight hit area so it can't bleed into pitch row
        if (volTrackW > 0 && isIn(mouseX, mouseY, volTrackX, volRowY, volTrackW, 14)) {
            temp.volume  = clamp01((float)(mouseX - volTrackX) / volTrackW);
            volDragging  = true;
            return true;
        }

        // Pitch slider
        if (pitchTrackW > 0 && isIn(mouseX, mouseY, pitchTrackX, pitchRowY, pitchTrackW, 14)) {
            temp.pitch    = 0.5f + clamp01((float)(mouseX - pitchTrackX) / pitchTrackW) * 1.5f;
            pitchDragging = true;
            return true;
        }

        // Buttons
        if (isIn(mouseX, mouseY, btn1X, btnY, btnW, 14)) {
            ModSounds.playConfig(MinecraftClient.getInstance(), temp);
            return true;
        }
        if (isIn(mouseX, mouseY, btn2X, btnY, btnW, 14)) {
            temp.copyFrom(defaults);
            soundIdx = findSoundIdx(temp.soundId);
            return true;
        }
        if (isIn(mouseX, mouseY, btn3X, btnY, btnW, 14)) {
            setter.accept(temp.copy());
            if (onSave != null) onSave.run();
            close();
            return true;
        }

        return true; // consume all clicks inside panel
    }

    public static boolean onMouseDragged(double x, double y) {
        if (!open) return false;
        int mx = (int) x;
        if (volDragging && volTrackW > 0) {
            temp.volume = clamp01((float)(mx - volTrackX) / volTrackW);
            return true;
        }
        if (pitchDragging && pitchTrackW > 0) {
            temp.pitch = 0.5f + clamp01((float)(mx - pitchTrackX) / pitchTrackW) * 1.5f;
            return true;
        }
        return false;
    }

    public static void onMouseReleased() {
        volDragging   = false;
        pitchDragging = false;
    }

    // ----------------------------------------------------------------
    // Drawing helpers
    // ----------------------------------------------------------------

    private static void drawSlider(DrawContext ctx, int x, int y, int w, float norm, int color) {
        int midY   = y + 7;
        // Track
        ctx.fill(x, midY - 1, x + w, midY + 1, BORDER);
        // Filled portion
        int filled = (int)(norm * w);
        if (filled > 0) ctx.fill(x, midY - 1, x + filled, midY + 1, color);
        // Knob — clamped so it never bleeds outside the track
        int knobX = x + Math.max(4, Math.min(w - 4, filled));
        ctx.fill(knobX - 4, midY - 4, knobX + 4, midY + 4, TEXT_PRIMARY);
    }

    private static void drawBtn(DrawContext ctx, TextRenderer tr,
                                int x, int y, int w, int h, String label,
                                int mouseX, int mouseY, boolean primary) {
        boolean hov = isIn(mouseX, mouseY, x, y, w, h);
        int bg = primary ? (hov ? 0xFF3DA875 : POSITIVE) : (hov ? PANEL_HOVER : PANEL_SOFT);
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + w, y + 1, BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
        ctx.drawText(tr, Text.literal(label),
            x + (w - tr.getWidth(label)) / 2,
            y + (h - tr.fontHeight) / 2,
            TEXT_PRIMARY, false);
    }

    // ----------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------

    private static String shortName(String id) {
        if (id.startsWith("minecraft:"))            id = id.substring("minecraft:".length());
        if (id.startsWith("block.note_block."))     return id.substring("block.note_block.".length());
        if (id.startsWith("block."))                return id.substring("block.".length());
        if (id.startsWith("entity."))               return id.substring("entity.".length());
        if (id.startsWith("ui."))                   return id.substring("ui.".length());
        if (id.startsWith("item."))                 return id.substring("item.".length());
        return id;
    }

    /** Truncates from the left with "…" prefix if the text is wider than maxW pixels. */
    private static String fitText(String text, TextRenderer tr, int maxW) {
        if (tr.getWidth(text) <= maxW) return text;
        while (text.length() > 1 && tr.getWidth("…" + text) > maxW) text = text.substring(1);
        return "…" + text;
    }

    private static int findSoundIdx(String soundId) {
        for (int i = 0; i < ModSounds.SOUND_LIST.length; i++) {
            if (ModSounds.SOUND_LIST[i].equalsIgnoreCase(soundId)) return i;
        }
        return 0;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
