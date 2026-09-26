package com.islesplus.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * One thing Isles+ draws on the player's screen outside its own menus. {@link HudLayout} places
 * and scales it; the element only measures and draws itself at (0, 0) in unscaled GUI pixels.
 * {@code preview} asks for sample content, for the HUD editor when there is nothing live to show.
 */
public abstract class HudElement {
    /** Where the element is on screen this frame, for an element that needs to know (e.g. to keep
     * a line of text on screen). Coordinates are screen GUI pixels. */
    public record Frame(int x, int y, float scale, float backgroundOpacity, int screenW, int screenH, boolean preview) {}

    public record Size(int w, int h) {}

    public final String id;
    public final String name;
    private final HudPlacement defaults;

    protected HudElement(String id, String name, HudPlacement defaults) {
        this.id = id;
        this.name = name;
        this.defaults = defaults;
    }

    /** Where it goes until the player moves it. Can depend on the game (e.g. "under the boss bars,
     * however many there are"), so it is asked every frame; override for that. */
    public HudPlacement defaults() { return defaults.copy(); }

    /** Whether the feature is switched on (the editor marks the ones that are not). */
    public abstract boolean enabled();

    /** Whether it should be drawn in game right now. */
    public abstract boolean active(MinecraftClient client);

    public abstract Size measure(boolean preview);

    public abstract void draw(DrawContext ctx, Frame frame);

    /** Drawn by the HUD pass. False for something drawn elsewhere (the inventory search bar). */
    public boolean inHud() { return true; }

    /** Can be stuck on top of the scoreboard (moving with it, whatever its size); the editor has a
     * toggle to place it freely instead. See {@link HudLayout#stuck}. */
    public boolean followsScoreboard() { return false; }

    /** Like {@link #followsScoreboard}, but stuck under the scoreboard, its right edge lined up
     * with the scoreboard's text. */
    public boolean belowScoreboard() { return false; }

    /** Can be stuck to the scoreboard at all (above or below it). */
    public final boolean scoreboardAttached() { return followsScoreboard() || belowScoreboard(); }

    /** Has a background panel whose opacity the editor lets the player set (0 = gone). */
    public boolean hasBackgroundOpacity() { return false; }

    /** How much of the element, from its top, may go off the top of the screen (unscaled). */
    public int topSlack(boolean preview) { return 0; }

    /** Shown under the name in the editor, e.g. where the element really appears. */
    public String editorHint() { return null; }
}
