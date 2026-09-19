package com.islesplus.mixin;

import com.islesplus.features.harvesttimer.HarvestTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public class TextDisplayEntityRendererMixin {


    /** Shadow = the text colour at this fraction of its brightness. */
    private static final float SHADOW_BRIGHTNESS = 0.25f;   // the same quarter brightness the game uses for its own text shadows

    private static OrderedText noShadow(OrderedText text) {
        // Only where the server left the shadow colour unset: an explicit one is drawn by the game
        // even with the label's shadow flag off, and must survive.
        return visitor -> text.accept((index, style, codePoint) -> visitor.accept(index,
            style.getShadowColor() == null ? style.withShadowColor(0) : style, codePoint));
    }

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void islesplus$injectHarvestTime(
            DisplayEntity.TextDisplayEntity entity,
            TextDisplayEntityRenderState state,
            float tickDelta,
            CallbackInfo ci) {

        if (!HarvestTimer.harvestTimerEnabled) return;

        UUID trackedUuid = HarvestTimer.getTrackedNodeUuid();
        if (trackedUuid == null) return;
        if (!entity.getUuid().equals(trackedUuid)) return;

        String timeStr = HarvestTimer.getFormattedTime();
        if (timeStr == null) return;

        DisplayEntity.TextDisplayEntity.TextLines originalLines = state.textLines;
        if (originalLines == null) return;
        if (state.data == null) return;   // a label with no data yet: nothing to restyle

        // Build the new time line
        // Bold Silkscreen in the timer colour, with a drop shadow in a darker shade of that colour.
        int rgb = HarvestTimer.getTimeColor();
        int shadow = 0xFF000000 | com.islesplus.ui.ColorMath.darken(rgb, SHADOW_BRIGHTNESS);
        Text timeText = com.islesplus.ui.Fonts.ofEstimate(timeStr)
            .styled(s -> s.withColor(TextColor.fromRgb(rgb)).withBold(true).withShadowColor(shadow));

        // A label only draws shadows when its shadow flag is set. If the server left it off, turn
        // it on for this label and give the server's own lines a transparent shadow, so they
        // look exactly as they did and only our line gains one.
        boolean serverShadow = (state.data.flags() & DisplayEntity.TextDisplayEntity.SHADOW_FLAG) != 0;
        if (!serverShadow) {
            state.data = new DisplayEntity.TextDisplayEntity.Data(state.data.text(), state.data.lineWidth(),
                state.data.textOpacity(), state.data.backgroundColor(),
                (byte) (state.data.flags() | DisplayEntity.TextDisplayEntity.SHADOW_FLAG));
        }
        OrderedText orderedTime = timeText.asOrderedText();
        int timeWidth = MinecraftClient.getInstance().textRenderer.getWidth(orderedTime);

        DisplayEntity.TextDisplayEntity.TextLine timeLine =
            new DisplayEntity.TextDisplayEntity.TextLine(orderedTime, timeWidth);

        // stick the timer line on top, skipping any empty leading lines
        // (server text usually starts with \n so the first line is zero width)
        List<DisplayEntity.TextDisplayEntity.TextLine> origList = originalLines.lines();
        List<DisplayEntity.TextDisplayEntity.TextLine> newLines = new ArrayList<>();
        newLines.add(timeLine);
        int start = 0;
        while (start < origList.size() && origList.get(start).width() == 0) {
            start++;
        }
        for (int i = start; i < origList.size(); i++) {
            DisplayEntity.TextDisplayEntity.TextLine line = origList.get(i);
            newLines.add(serverShadow ? line : new DisplayEntity.TextDisplayEntity.TextLine(noShadow(line.contents()), line.width()));
        }

        int newWidth = Math.max(timeWidth, originalLines.width());

        state.textLines = new DisplayEntity.TextDisplayEntity.TextLines(newLines, newWidth);
    }
}
