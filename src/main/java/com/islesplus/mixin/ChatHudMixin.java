package com.islesplus.mixin;

import com.islesplus.features.chatfilter.ChatFilter;
import com.islesplus.features.ownerdecorator.OwnerRepository;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    // matches valid Minecraft username characters; used to scan tokens before separator
    private static final Pattern TOKEN = Pattern.compile("[a-zA-Z0-9_]+");
    private static final String CHAT_SEPARATOR = "\u00BB";
    private static final Text OWNER_PREFIX = Text.empty()
        .append(Text.literal("[").styled(s -> s.withColor(Formatting.DARK_GRAY)))
        .append(Text.literal("+").styled(s -> s
            .withColor(TextColor.fromRgb(0x55FFFF))
            .withBold(true)
            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Isles+ Dev").formatted(Formatting.GOLD)))))
        .append(Text.literal("] ").styled(s -> s.withColor(Formatting.DARK_GRAY)));

    @Unique
    private static boolean injecting = false;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void islesplus$decorateOwnerChat(Text message, CallbackInfo ci) {
        if (injecting) return;
        String plain = message.getString();

        if (ChatFilter.shouldFilter(plain)) {
            ci.cancel();
            return;
        }

        int sepIdx = plain.indexOf(CHAT_SEPARATOR);
        if (sepIdx < 0) return;

        // If any token left of the separator is an owner, prepend a marker and keep original formatting.
        Matcher m = TOKEN.matcher(plain.substring(0, sepIdx));
        boolean foundOwner = false;
        while (m.find()) {
            if (OwnerRepository.isOwner(m.group())) {
                foundOwner = true;
                break;
            }
        }
        if (!foundOwner) return;

        Text decorated = Text.empty().append(OWNER_PREFIX).append(message);

        injecting = true;
        try {
            ((ChatHud) (Object) this).addMessage(decorated);
        } finally {
            injecting = false;
        }
        ci.cancel();
    }
}
