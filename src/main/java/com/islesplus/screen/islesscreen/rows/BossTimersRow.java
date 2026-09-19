package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesPlusConfig;
import com.islesplus.features.bosstracker.BossTracker;
import com.islesplus.features.bosstracker.BossTracker.BossHudPosition;
import com.islesplus.features.bosstracker.BossTracker.TrackedBoss;
import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.AnchorGrid;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.FootnoteBox;
import com.islesplus.ui.widgets.InfoChip;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.Panel;
import com.islesplus.ui.widgets.CheckTile;
import com.islesplus.ui.widgets.SmallToggle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss Timers row: a toggle plus a drawer holding the auto-/bossary toggle, HUD anchor picker,
 * a "run /bossary" prompt shown only while no bosses are loaded, and a dynamic list of
 * per-{@link TrackedBoss} visibility toggles.
 *
 * <p>The boss toggle list is rebuilt from scratch (in {@link DrawerBody#layout}) whenever the
 * model's {@code BossTracker.bosses} names have structurally changed since the last build
 * (different size, or any name at the same index no longer equal). {@code BossTracker.bosses}
 * is only ever mutated from the game thread, same as rendering, so no locking is needed — but
 * it can still change between frames while this screen is open. A rebuild discards the whole
 * previous toggle subtree; since none of these toggles hold focus, nothing is lost.
 */
public final class BossTimersRow {
    private static final int ROW_GAP = 4;

    private BossTimersRow() {}

    public static FeatureRow build(OverlayHost host) {
        return new FeatureRow("Boss Timers", "Track world boss spawn timers.")
            .killedKey("boss_tracker")
            .toggle(() -> BossTracker.bossTrackerEnabled,
                v -> {
                    BossTracker.bossTrackerEnabled = v;
                    if (v && BossTracker.autoOpenBossary) {
                        BossTracker.pendingBossaryOpen = true;
                    }
                    IslesPlusConfig.save();
                })
            .drawer(new DrawerBody(host));
    }

    /** Drawer body: static auto-/bossary toggle + anchor picker, a "not loaded yet" panel whose
     * visibility is synced every layout, and a per-boss toggle list rebuilt when the boss
     * names change. */
    private static final class DrawerBody extends Widget {
        private final OverlayHost host;
        private final Flow.Column column = new Flow.Column(Metrics.DRAWER_GAP);
        private final Flow.Column bossColumn = new Flow.Column(ROW_GAP);
        private final Widget notLoadedPanel;
        private List<String> builtFrom;

        DrawerBody(OverlayHost host) {
            this.host = host;

            SmallToggle autoBossary = new SmallToggle("Auto /bossary on join",
                () -> BossTracker.autoOpenBossary,
                () -> { BossTracker.autoOpenBossary = !BossTracker.autoOpenBossary; IslesPlusConfig.save(); })
                .badge("BETA");

            AnchorGrid grid = new AnchorGrid(2,
                new String[]{"TOP LEFT", "TOP RIGHT", "BOTTOM LEFT", "BOTTOM RIGHT"},
                () -> BossTracker.hudPosition.ordinal(),
                i -> {
                    BossTracker.hudPosition = BossHudPosition.values()[i];
                    IslesPlusConfig.save();
                });

            notLoadedPanel = new Panel(
                new Flow.WrapRow(ROW_GAP, ROW_GAP)
                    .add(new Label("Open /bossary to load bosses", Theme.INK_DEEP, Fonts.BODY).wrap())
                    .add(new Button("RUN /BOSSARY", Button.Kind.PRIMARY, this::runBossary).small()),
                5, Theme.RAISED, Theme.RAISED_LIT, Theme.RAISED_SHADE, Theme.SURFACE_RING);

            column.add(autoBossary)
                .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                    .add(new Label("ANCHOR", Theme.TEXT_LABEL, Fonts.BODY))
                    .add(new InfoChip(grid::selectedLabel)))
                .add(grid)
                .add(notLoadedPanel)
                .add(bossColumn)
                .add(new FootnoteBox("~ estimated timer", "/bossary to refresh, or proximity to boss"));
        }

        private void runBossary() {
            if (!BossTracker.bossTrackerEnabled) {
                BossTracker.bossTrackerEnabled = true;
                IslesPlusConfig.save();
            }
            BossTracker.pendingBossaryOpen = true;
            host.closeScreen();
        }

        private boolean bossNamesChanged(List<String> currentNames) {
            return builtFrom == null || !builtFrom.equals(currentNames);
        }

        private void rebuildBossList(List<String> currentNames) {
            bossColumn.clear();
            for (String name : currentNames) {
                // Check tiles, like every other option list (main switched these from mini toggles).
                bossColumn.add(new CheckTile(name,
                    () -> !BossTracker.hiddenBossNames.contains(name),
                    () -> {
                        if (!BossTracker.hiddenBossNames.remove(name)) {
                            BossTracker.hiddenBossNames.add(name);
                        }
                        IslesPlusConfig.save();
                    }));
            }
            builtFrom = currentNames;
        }

        @Override public int prefWidth() { return Integer.MAX_VALUE; }

        @Override public int layout(int x, int y, int width) {
            List<TrackedBoss> current = BossTracker.bosses;
            List<String> currentNames = new ArrayList<>(current.size());
            for (TrackedBoss boss : current) currentNames.add(boss.name);
            if (bossNamesChanged(currentNames)) rebuildBossList(currentNames);
            boolean empty = current.isEmpty();
            notLoadedPanel.visible = empty;
            bossColumn.visible = !empty;

            this.x = x;
            this.y = y;
            this.w = width;
            this.h = column.layout(x, y, width);
            return this.h;
        }

        @Override public void render(DrawContext ctx, int mouseX, int mouseY) { column.render(ctx, mouseX, mouseY); }
        @Override public boolean mouseClicked(double mx, double my, int button) { return column.mouseClicked(mx, my, button); }
        @Override public boolean mouseDragged(double mx, double my) { return column.mouseDragged(mx, my); }
        @Override public void mouseReleased() { column.mouseReleased(); }
        @Override public boolean mouseScrolled(double mx, double my, double amount) { return column.mouseScrolled(mx, my, amount); }
        @Override public boolean keyPressed(KeyInput in) { return column.keyPressed(in); }
        @Override public boolean charTyped(CharInput in) { return column.charTyped(in); }
        @Override public void unfocus() { column.unfocus(); }
    }
}
