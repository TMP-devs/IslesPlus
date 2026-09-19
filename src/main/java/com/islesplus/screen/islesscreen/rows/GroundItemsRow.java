package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesPlusConfig;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier;
import com.islesplus.features.grounditemsnotifier.GroundItemsNotifier.WatchedItem;
import com.islesplus.screen.islesscreen.EditSoundDialog;
import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.CheckChip;
import com.islesplus.ui.widgets.EmptyBox;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.Panel;
import com.islesplus.ui.widgets.SmallToggle;
import com.islesplus.ui.widgets.TextField;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.List;

/**
 * Ground Items Notifier row: a toggle plus a drawer holding a dynamic list of per-{@link
 * WatchedItem} sub-cards and an "ADD ITEM" button.
 *
 * <p>The list is rebuilt from scratch (in {@link ListBody#layout}) whenever the model's {@code
 * watchedItems} list has structurally changed since the last build: different size, or any
 * element at the same index is no longer the same (identity) instance. That covers this row's
 * own add/remove clicks (which change the size) as well as any external mutation of the list.
 * Because a rebuild discards the whole previous widget subtree, nothing from before the rebuild
 * (in particular a focused {@link TextField}) can remain reachable afterwards.
 */
public final class GroundItemsRow {
    private static final int ROW_GAP = 4;

    private GroundItemsRow() {}

    public static FeatureRow build(OverlayHost host) {
        return new FeatureRow("Ground Items Notifier", "Highlight dropped items on the ground.")
            .killedKey("ground_items_notifier")
            .toggle(() -> GroundItemsNotifier.groundItemsNotifierEnabled,
                v -> { GroundItemsNotifier.groundItemsNotifierEnabled = v; IslesPlusConfig.save(); })
            .drawer(new ListBody(host));
    }

    /** Status text shown on an item's sub-card: {@code "WATCHING · n ALERT(S)"} while enabled
     * (n = how many of its four alert flags are on), otherwise {@code "PAUSED"}. Package-visible
     * so {@code GroundItemsRowTest} can exercise it directly. */
    static String statusText(WatchedItem item) {
        if (!item.enabled) return "PAUSED";
        int n = 0;
        if (item.lineTracker) n++;
        if (item.screenNotifier) n++;
        if (item.highlight) n++;
        if (item.soundPing) n++;
        return "WATCHING · " + n + " ALERT" + (n == 1 ? "" : "S");
    }

    private static Widget buildItemCard(OverlayHost host, WatchedItem item, Runnable onRemove) {
        SmallToggle enabledToggle = new SmallToggle(null,
            () -> item.enabled,
            () -> { item.enabled = !item.enabled; IslesPlusConfig.save(); });
        Label status = new Label(() -> statusText(item), Theme.TEXT_META, Fonts.SMALL).wrap();
        Button remove = new Button("REMOVE", Button.Kind.ICON, onRemove);

        TextField keyword = new TextField(
            () -> item.customKeyword,
            v -> item.customKeyword = v,
            "item name or #lore", 64)
            .onBlur(IslesPlusConfig::save)
            .ring(Theme.INK_DEEP);

        CheckChip lineChip = new CheckChip("Line", () -> item.lineTracker,
            () -> { item.lineTracker = !item.lineTracker; IslesPlusConfig.save(); });
        CheckChip screenChip = new CheckChip("Notify", () -> item.screenNotifier,
            () -> { item.screenNotifier = !item.screenNotifier; IslesPlusConfig.save(); });
        CheckChip glowChip = new CheckChip("Glow", () -> item.highlight,
            () -> { item.highlight = !item.highlight; IslesPlusConfig.save(); });
        CheckChip soundChip = new CheckChip("Sound", () -> item.soundPing,
            () -> { item.soundPing = !item.soundPing; IslesPlusConfig.save(); });

        Button editSound = new Button("EDIT SOUND", Button.Kind.PRIMARY,
            () -> EditSoundDialog.open(host, () -> item.soundConfig, v -> item.soundConfig = v,
                GroundItemsNotifier.DEFAULT_ITEM_SOUND));
        Label summary = new Label(() -> EditSoundDialog.summary(item.soundConfig), Theme.TEXT_META, Fonts.SMALL);
        // Own row under the alert chips, shown only while the Sound chip is on.
        Flow.WrapRow soundRow = new Flow.WrapRow(ROW_GAP, ROW_GAP)
            .add(editSound)
            .add(summary);

        Flow.Column body = new Flow.Column(4)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(enabledToggle)
                .add(status)
                .add(remove))
            .add(keyword)
            .add(new Flow.WrapRow(ROW_GAP, ROW_GAP)
                .add(lineChip)
                .add(screenChip)
                .add(glowChip)
                .add(soundChip))
            .add(soundRow);

        return new ItemCard(body, item, soundRow, summary);
    }

    /** A sub-card's bevel (SURFACE fill, RAISED lit, SECONDARY shade, SURFACE_RING ring, 5 px
     * padding), plus the per-frame visibility sync that makes the "EDIT SOUND" button and its
     * summary label appear only while {@link WatchedItem#soundPing} is on. */
    private static final class ItemCard extends Panel {
        private final WatchedItem item;
        private final Widget editSoundButton;
        private final Widget summaryLabel;

        ItemCard(Widget body, WatchedItem item, Widget editSoundButton, Widget summaryLabel) {
            super(body, 5, Theme.SURFACE, Theme.RAISED, Theme.SECONDARY, Theme.SURFACE_RING);
            this.item = item;
            this.editSoundButton = editSoundButton;
            this.summaryLabel = summaryLabel;
        }

        @Override public int layout(int x, int y, int width) {
            editSoundButton.visible = item.soundPing;
            summaryLabel.visible = item.soundPing;
            return super.layout(x, y, width);
        }
    }

    /** Drawer body: rebuilds its child list whenever {@link GroundItemsNotifier#watchedItems}
     * has structurally changed since the last build, then delegates everything to that list. */
    private static final class ListBody extends Widget {
        private final OverlayHost host;
        private final Flow.Column column = new Flow.Column(Metrics.DRAWER_GAP);
        private List<WatchedItem> builtFrom;

        ListBody(OverlayHost host) {
            this.host = host;
        }

        private boolean structurallyChanged() {
            List<WatchedItem> current = GroundItemsNotifier.watchedItems;
            if (builtFrom == null || builtFrom.size() != current.size()) return true;
            for (int i = 0; i < current.size(); i++) {
                if (builtFrom.get(i) != current.get(i)) return true;
            }
            return false;
        }

        private void rebuild() {
            List<WatchedItem> current = GroundItemsNotifier.watchedItems;
            column.clear();
            if (current.isEmpty()) {
                column.add(new EmptyBox("NO ITEMS WATCHED YET"));
            } else {
                for (WatchedItem item : current) {
                    column.add(buildItemCard(host, item, () -> removeItem(item)));
                }
            }
            column.add(new Flow.WrapRow(ROW_GAP, ROW_GAP).align(Flow.Align.CENTER)
                .add(new Button("ADD ITEM", Button.Kind.PRIMARY, ListBody::addItem).plus()));
            builtFrom = new ArrayList<>(current);
        }

        private static void addItem() {
            GroundItemsNotifier.watchedItems.add(new WatchedItem());
            IslesPlusConfig.save();
        }

        private static void removeItem(WatchedItem item) {
            GroundItemsNotifier.watchedItems.remove(item);
            IslesPlusConfig.save();
        }

        @Override public int prefWidth() { return Integer.MAX_VALUE; }

        @Override public int layout(int x, int y, int width) {
            if (structurallyChanged()) rebuild();
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
