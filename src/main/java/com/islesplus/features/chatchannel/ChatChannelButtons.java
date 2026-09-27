package com.islesplus.features.chatchannel;

import com.islesplus.IslesPlusConfig;
import com.islesplus.mixin.ChatScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Adds the channel selector to the vanilla chat screen. */
public final class ChatChannelButtons {
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 3;
    private static final int RIGHT_MARGIN = 4;
    private static final int ABOVE_CHAT_INPUT = 24;
    private static final int[] BUTTON_WIDTHS = {58, 52, 52};

    private ChatChannelButtons() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen chatScreen) || !ChatChannels.active()) return;

            List<ClickableWidget> buttons = Screens.getButtons(screen);
            int totalWidth = BUTTON_WIDTHS[0] + BUTTON_WIDTHS[1] + BUTTON_WIDTHS[2] + GAP * 2;
            int x = width - RIGHT_MARGIN - totalWidth;
            int y = height - ABOVE_CHAT_INPUT - BUTTON_HEIGHT;
            ButtonWidget[] channelButtons = new ButtonWidget[ChatChannel.values().length];

            for (ChatChannel channel : ChatChannel.values()) {
                int index = channel.ordinal();
                ButtonWidget button = ButtonWidget.builder(label(channel), pressed ->
                    select(client, chatScreen, channel, channelButtons))
                    .dimensions(x, y, BUTTON_WIDTHS[index], BUTTON_HEIGHT)
                    .build();
                channelButtons[index] = button;
                buttons.add(button);
                x += BUTTON_WIDTHS[index] + GAP;
            }
        });
    }

    private static Text label(ChatChannel channel) {
        MutableText text = Text.literal(channel.label);
        return channel == ChatChannels.selectedChannel
            ? text.formatted(Formatting.AQUA, Formatting.BOLD)
            : text.formatted(Formatting.GRAY);
    }

    private static void select(MinecraftClient client, ChatScreen screen,
                               ChatChannel channel, ButtonWidget[] buttons) {
        ChatChannels.selectedChannel = channel;
        IslesPlusConfig.save();
        for (ChatChannel available : ChatChannel.values()) {
            buttons[available.ordinal()].setMessage(label(available));
        }

        // Screen.mouseClicked focuses the clicked button after its onPress callback returns.
        // Defer this until the click has fully completed, otherwise vanilla immediately puts
        // focus back on the button.
        client.execute(() -> restoreChatFocus(client, screen, buttons));
    }

    private static void restoreChatFocus(MinecraftClient client, ChatScreen screen,
                                         ButtonWidget[] buttons) {
        if (client.currentScreen != screen) return;
        TextFieldWidget chatField = ((ChatScreenAccessor) screen).getChatField();
        chatField.setFocused(true);
        screen.setFocused(chatField);
        for (ButtonWidget button : buttons) {
            button.setFocused(false);
        }
    }
}