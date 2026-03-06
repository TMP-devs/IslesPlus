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

    private static final int CYAN_COLOR = 0x55FFFF;

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

        // Build the new time line
        Text timeText = Text.literal(timeStr)
            .styled(s -> s.withColor(TextColor.fromRgb(CYAN_COLOR)).withBold(true));
        OrderedText orderedTime = timeText.asOrderedText();
        int timeWidth = MinecraftClient.getInstance().textRenderer.getWidth(orderedTime);

        DisplayEntity.TextDisplayEntity.TextLine timeLine =
            new DisplayEntity.TextDisplayEntity.TextLine(orderedTime, timeWidth);

        // Prepend time line to existing lines, skipping any leading zero-width lines
        // (the server text often starts with \n which produces a zero-width first line)
        List<DisplayEntity.TextDisplayEntity.TextLine> origList = originalLines.lines();
        List<DisplayEntity.TextDisplayEntity.TextLine> newLines = new ArrayList<>();
        newLines.add(timeLine);
        int start = 0;
        while (start < origList.size() && origList.get(start).width() == 0) {
            start++;
        }
        for (int i = start; i < origList.size(); i++) {
            newLines.add(origList.get(i));
        }

        int newWidth = Math.max(timeWidth, originalLines.width());

        state.textLines = new DisplayEntity.TextDisplayEntity.TextLines(newLines, newWidth);
    }
}
