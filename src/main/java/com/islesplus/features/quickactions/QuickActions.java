package com.islesplus.features.quickactions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.islesplus.IslesClient;
import com.islesplus.mixin.HandledScreenAccessor;
import com.islesplus.mixin.KeyBindingAccessor;
import com.islesplus.screen.OverlayScreen;
import com.islesplus.screen.islesscreen.QuickActionDialog;
import com.islesplus.sync.FeatureFlags;
import com.islesplus.ui.Draw;
import com.islesplus.ui.Theme;
import com.islesplus.ui.widgets.NoteTooltip;
import com.islesplus.world.PlayerWorld;
import com.islesplus.world.WorldIdentification;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Three buttons in the player inventory, stacked above the off hand slot (button 1 at the
 * bottom). Left click runs the button, right click (or any click on an empty one) edits it.
 *
 * A button runs an Isles+ action ({@link IslesAction}), presses any keybind, or sends a command.
 * Remote kill key: "quick_actions" (hides the buttons). An Isles+ action is also blocked by its
 * own feature's key.
 */
public final class QuickActions {
    public static final String KILL_KEY = "quick_actions";
    public static final int COUNT = 3;
    /** The slot cell a button sits in (and its click area, plus the 1 px ring round it). */
    public static final int SIZE = 16;
    /** The drawn face, a little smaller than the cell and centred in it; the item is scaled to fit. */
    static final int FACE = 14, FACE_INSET = (SIZE - FACE) / 2;
    /** Column above the off hand slot, in the inventory texture's own coordinates: button 3 one
     * pixel below the helmet slot's top, the others below it at an even 16 px (2 px between the
     * 14 px faces). */
    static final int COLUMN_X = 77, TOP_Y = 9, PITCH = 16;

    public static boolean enabled = true;
    /** false = no tan square behind the buttons, only their icons (the ring still shows on hover). */
    public static boolean showBackground = true;
    public static final QuickAction[] actions = new QuickAction[COUNT];
    static {
        for (int i = 0; i < COUNT; i++) actions[i] = new QuickAction();
    }

    /** a keybind waiting to be pressed once the inventory has closed */
    private static KeyBinding pendingKey;
    private static boolean pendingHeld;

    private QuickActions() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof InventoryScreen)) return;
            ScreenMouseEvents.allowMouseClick(screen).register(QuickActions::onMouseClick);
            // Drawing is driven from InventoryScreenMixin, at the tail of the screen's own render:
            // ScreenEvents.afterRender runs after the hovered item's tooltip has been queued, which
            // put these buttons on top of it.
        });
    }

    public static boolean active() {
        return enabled && !FeatureFlags.isKilled(KILL_KEY) && WorldIdentification.world != PlayerWorld.OTHER;
    }

    /** Top left of button {@code index}'s face. Button 0 ("1") is the bottom one. */
    static int buttonX(int guiX) { return guiX + COLUMN_X; }

    static int buttonY(int guiY, int index) { return guiY + TOP_Y + (COUNT - 1 - index) * PITCH; }

    /** Which button is under the point, -1 for none. Hit area = the button's 16 px cell (the cells
     * touch, so each one owns exactly its own rows). */
    static int buttonAt(int guiX, int guiY, double mx, double my) {
        int x = buttonX(guiX);
        if (mx < x - 1 || mx >= x + SIZE + 1) return -1;
        for (int i = 0; i < COUNT; i++) {
            int y = buttonY(guiY, i);
            if (my >= y && my < y + SIZE) return i;
        }
        return -1;
    }

    private static int hovered(Screen screen, double mx, double my) {
        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        return buttonAt(acc.getGuiX(), acc.getGuiY(), mx, my);
    }

    private static boolean onMouseClick(Screen screen, Click click) {
        if (!active()) return true;
        int i = hovered(screen, click.x(), click.y());
        if (i < 0) return true;
        MinecraftClient client = MinecraftClient.getInstance();
        // Something on the cursor: let the click through, never eat a held item
        if (client.player != null && !client.player.currentScreenHandler.getCursorStack().isEmpty()) return true;
        IslesClient.playMenuClickSound();
        if (click.button() == 1 || !actions[i].isSet()) {
            openEditor(client, i);
        } else if (click.button() == 0) {
            run(client, screen, i);
        }
        return false;
    }

    /** The editor as a pop-up over the inventory; closing it reopens the inventory. */
    private static void openEditor(MinecraftClient client, int index) {
        OverlayScreen host = new OverlayScreen(() -> {
            if (client.player != null) client.setScreen(new InventoryScreen(client.player));
            else client.setScreen(null);
        });
        client.setScreen(host);
        QuickActionDialog.open(host, index, host::close);
    }

    public static void run(MinecraftClient client, Screen screen, int index) {
        QuickAction a = actions[index];
        switch (a.type) {
            case ISLES -> {
                IslesAction action = IslesAction.byId(a.target);
                if (action != null) action.run(client);
            }
            case KEYBIND -> {
                KeyBinding binding = KeyBinding.byId(a.target);
                if (binding == null) {
                    IslesClient.sendInfoMessage(client, "That keybind no longer exists (" + a.target + ").");
                    return;
                }
                // Vanilla keys only act with no screen open, so close the inventory first and press
                // the key on the next tick (see tick()).
                if (screen != null) screen.close();
                pendingKey = binding;
                pendingHeld = false;
            }
            case COMMAND -> {
                if (client.player != null && !a.target.isEmpty()) client.player.networkHandler.sendChatCommand(a.target);
            }
            case NONE -> {}
        }
    }

    /** End of every client tick: press a pending keybind for one tick, then let it go. */
    public static void tick(MinecraftClient client) {
        KeyBinding key = pendingKey;
        if (key == null) return;
        if (!pendingHeld) {
            if (client.currentScreen != null) return;   // still closing
            key.setPressed(true);
            KeyBindingAccessor acc = (KeyBindingAccessor) key;
            acc.setTimesPressed(acc.getTimesPressed() + 1);
            pendingHeld = true;
        } else {
            key.setPressed(false);
            pendingKey = null;
            pendingHeld = false;
        }
    }

    /** on disconnect */
    public static void reset() {
        if (pendingKey != null) pendingKey.setPressed(false);
        pendingKey = null;
        pendingHeld = false;
    }

    // ==============================
    // Rendering
    // ==============================

    /** Called from {@link com.islesplus.mixin.InventoryScreenMixin} at the tail of the inventory's
     * own render pass, so an item tooltip - drawn after it - stays in front of the buttons. */
    public static void render(Screen screen, DrawContext ctx, int mx, int my) {
        // The mixin fires for every container screen; these buttons belong to the inventory only.
        if (!(screen instanceof InventoryScreen) || !active()) return;
        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int x = buttonX(acc.getGuiX());
        int hover = buttonAt(acc.getGuiX(), acc.getGuiY(), mx, my);
        // In front of the panel and the item icons: without its own root layer this pass lands
        // under them. The item tooltip is painted after render returns, so it still wins.
        ctx.createNewRootLayer();
        for (int i = 0; i < COUNT; i++) {
            drawButton(ctx, actions[i], x, buttonY(acc.getGuiY(), i), i == hover);
        }
        if (hover >= 0) {
            ctx.createNewRootLayer();   // above the item icons
            NoteTooltip.draw(ctx, tooltip(hover), mx, my, screen.width, screen.height);
        }
    }

    /** One button in its slot cell (x, y = the cell): a raised tan square with an ink ring, a little
     * smaller than the cell, the item on top scaled to match (or a "+" when empty). With the
     * background off, only the icon - and an oxblood ring while hovered. */
    public static void drawButton(DrawContext ctx, QuickAction a, int x, int y, boolean hover) {
        int fx = x + FACE_INSET, fy = y + FACE_INSET;
        if (showBackground) {
            Draw.bevel(ctx, fx, fy, FACE, FACE, hover ? Theme.RAISED_HOVER : Theme.RAISED,
                Theme.RAISED_LIT, Theme.RAISED_SHADE, hover ? Theme.OXBLOOD : Theme.INK);
        } else if (hover) {
            Draw.ring(ctx, fx, fy, FACE, FACE, Theme.OXBLOOD);
        }
        ItemStack stack = iconStack(a);
        if (stack.isEmpty()) {
            Draw.plus(ctx, x + SIZE / 2, y + SIZE / 2, Theme.TEXT_META);
            return;
        }
        ctx.getMatrices().pushMatrix();
        try {
            ctx.getMatrices().translate((float) fx, (float) fy);
            ctx.getMatrices().scale(FACE / (float) SIZE, FACE / (float) SIZE);
            ctx.drawItem(stack, 0, 0);
        } finally {
            ctx.getMatrices().popMatrix();
        }
    }

    private static String tooltip(int index) {
        QuickAction a = actions[index];
        String head = "Button " + (index + 1);
        if (!a.isSet()) return head + ": empty. Click to set it up.";
        return head + ": " + describe(a) + ". Right-click to edit.";
    }

    /** What the button does, in words: "Open backpack (/bp)", "Keybind: Toggle Perspective", "/warp spawn". */
    public static String describe(QuickAction a) {
        return switch (a.type) {
            case ISLES -> {
                IslesAction action = IslesAction.byId(a.target);
                yield action == null ? "Unknown action" : action.label;
            }
            case KEYBIND -> "Keybind: " + keybindName(a.target);
            case COMMAND -> "/" + a.target;
            case NONE -> "Nothing";
        };
    }

    public static String keybindName(String id) {
        return I18n.hasTranslation(id) ? I18n.translate(id) : id;
    }

    /** The item drawn on the button: the chosen icon, else the action's own default; EMPTY for
     * a button with neither (drawn as a "+"). */
    public static ItemStack iconStack(QuickAction a) {
        Item item = itemById(a.icon);
        if (item == null && !a.isSet()) return ItemStack.EMPTY;
        if (item == null) item = itemById(defaultIcon(a));
        return new ItemStack(item == null ? Items.BARRIER : item);
    }

    public static String defaultIcon(QuickAction a) {
        return switch (a.type) {
            case ISLES -> {
                IslesAction action = IslesAction.byId(a.target);
                yield action == null ? "minecraft:barrier" : action.defaultIcon;
            }
            case KEYBIND -> "minecraft:tripwire_hook";
            case COMMAND -> "minecraft:command_block";
            case NONE -> "minecraft:barrier";
        };
    }

    public static Item itemById(String id) {
        if (id == null || id.isBlank()) return null;
        Identifier ident = Identifier.tryParse(id);
        if (ident == null || !Registries.ITEM.containsId(ident)) return null;
        return Registries.ITEM.get(ident);
    }

    // ==============================
    // Config
    // ==============================

    public static JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (QuickAction a : actions) arr.add(a.toJson());
        return arr;
    }

    public static void loadJson(JsonArray arr) {
        for (int i = 0; i < COUNT; i++) {
            JsonElement el = arr != null && i < arr.size() ? arr.get(i) : null;
            actions[i] = QuickAction.fromJson(el);
        }
    }
}
