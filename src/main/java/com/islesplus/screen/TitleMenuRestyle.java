package com.islesplus.screen;

import com.islesplus.IslesClient;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The title screen's main column in the Isles+ look, plus the "Join Isles" button.
 * <p>Runs from Fabric's after-init screen event in a LATE phase, not from a mixin at the end of
 * {@code init}: mods that edit the title menu (Mod Menu adds its Mods button next to Realms, and
 * finds Realms by looking for a vanilla {@link ButtonWidget}) must see the untouched vanilla
 * buttons first. The stone twins are then made from the finished menu, their button included.
 * Fabric's button list edits the screen's drawn / clickable / narrated lists together, so nothing
 * else on the screen (draw-only or click-only elements of other mods) is touched.
 */
public final class TitleMenuRestyle {
    private static final String ISLES_HOST = "play.skyblockisles.net";
    private static final Identifier LATE = Identifier.of("islesplus", "late");

    /** Vanilla stacks its main buttons 24 px apart (20 px button + 4 px gap). */
    private static final int ROW_STEP = 24;
    /** Same height as the vanilla buttons, so the column stays even. */
    private static final int JOIN_H = 20;

    private TitleMenuRestyle() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE);
        ScreenEvents.AFTER_INIT.register(LATE, (client, screen, width, height) -> {
            if (screen instanceof TitleScreen) restyle(screen);
        });
    }

    private static void restyle(Screen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        ClickableWidget singleplayer = null;
        for (ClickableWidget w : buttons) {
            if (isSingleplayer(w.getMessage())) { singleplayer = w; break; }
        }
        if (singleplayer == null) {
            // Demo mode, or another mod rebuilt the menu: leave it alone, Join goes top-left.
            buttons.add(join(screen, 8, 8, 170, JOIN_H));
            return;
        }

        // Singleplayer moves UP one row and Join takes its old slot. Nothing below moves, so
        // Multiplayer, Realms (whose notification icons vanilla draws at a fixed height), Options,
        // Quit and the footer text all stay exactly where the game and other mods put them.
        int slotX = singleplayer.getX(), slotY = singleplayer.getY(), slotW = singleplayer.getWidth();
        singleplayer.setY(slotY - ROW_STEP);

        // In place and in order, so Tab still walks the menu top to bottom: plain text buttons in
        // the main column become stone-look twins. Icon buttons (language, accessibility) and the
        // copyright link are vanilla buttons too, and stay as they are.
        int joinIndex = -1;
        for (int i = 0; i < buttons.size(); i++) {
            ClickableWidget w = buttons.get(i);
            if (w == singleplayer) joinIndex = i + 1;
            boolean inColumn = w.getX() >= slotX - 1 && w.getX() + w.getWidth() <= slotX + slotW + 1
                && w.getY() >= slotY - ROW_STEP;
            if (inColumn && w instanceof ButtonWidget original
                && !(w instanceof TextIconButtonWidget) && !(w instanceof PressableTextWidget)) {
                buttons.set(i, new IslesMenuButton(w.getX(), w.getY(), w.getWidth(), w.getHeight(),
                    w.getMessage(), IslesMenuButton.Look.STONE, original::onPress).mirror(original));
            }
        }
        buttons.add(joinIndex, join(screen, slotX, slotY, slotW, JOIN_H));
    }

    /** By translation key, not by the rendered words: a resource pack or language cannot hide it. */
    private static boolean isSingleplayer(Text message) {
        return message.getContent() instanceof TranslatableTextContent t && "menu.singleplayer".equals(t.getKey());
    }

    private static IslesMenuButton join(Screen screen, int x, int y, int width, int height) {
        IslesMenuButton join = new IslesMenuButton(x, y, width, height, Text.literal("Join Isles"),
            IslesMenuButton.Look.JOIN, input -> connectToIsles(screen));
        join.setTooltip(Tooltip.of(Text.literal("Quick connect: " + ISLES_HOST)));
        return join;
    }

    private static void connectToIsles(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerAddress address = ServerAddress.parse(ISLES_HOST);
        ServerInfo info = new ServerInfo("Skyblock Isles", ISLES_HOST, ServerInfo.ServerType.OTHER);
        IslesClient.connectingToIsles = true;
        ConnectScreen.connect(screen, client, address, info, false, null);
    }
}
