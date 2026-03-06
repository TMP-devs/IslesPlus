package com.islesplus.mixin;

import com.islesplus.screen.hudedit.ScoreboardTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(InGameHud.class)
public class InGameHudScoreboardMixin {

    @Inject(method = "renderScoreboardSidebar", at = @At("RETURN"))
    private void captureScoreboardBounds(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.textRenderer == null) {
            ScoreboardTracker.clear();
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            ScoreboardTracker.clear();
            return;
        }

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        if (entries == null || entries.isEmpty()) {
            ScoreboardTracker.clear();
            return;
        }

        TextRenderer tr = client.textRenderer;
        int lineHeight = tr.fontHeight + 1;

        // Filter hidden, cap at 15 (same as vanilla)
        long count = entries.stream().filter(e -> !e.hidden()).limit(15).count();
        if (count == 0) {
            ScoreboardTracker.clear();
            return;
        }

        // Max width: compare header vs each entry (name + space + score number)
        int maxWidth = tr.getWidth(objective.getDisplayName());
        for (ScoreboardEntry entry : entries) {
            if (entry.hidden()) continue;
            int nameW  = tr.getWidth(entry.display() != null ? entry.display() : entry.name());
            int scoreW = tr.getWidth(String.valueOf(entry.value()));
            maxWidth = Math.max(maxWidth, nameW + 8 + scoreW);
        }

        // Vanilla renders: x = screenWidth - maxWidth - 3
        // y is centered: y = screenHeight/2 - (count * lineHeight)/2 - lineHeight (header above)
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        int totalHeight = (int)(count + 1) * lineHeight + 2; // entries + header + padding
        int sbX = sw - maxWidth - 3;
        int sbY = sh / 2 - totalHeight / 2;

        ScoreboardTracker.setBounds(sbX, sbY, maxWidth + 3, totalHeight);
    }
}
