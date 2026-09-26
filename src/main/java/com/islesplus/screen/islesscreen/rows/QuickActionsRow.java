package com.islesplus.screen.islesscreen.rows;

import com.islesplus.IslesPlusConfig;
import com.islesplus.features.quickactions.QuickAction;
import com.islesplus.features.quickactions.QuickActions;
import com.islesplus.screen.islesscreen.FeatureRow;
import com.islesplus.screen.islesscreen.QuickActionDialog;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.FootnoteBox;
import com.islesplus.ui.widgets.ItemButton;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.Panel;
import com.islesplus.ui.widgets.SmallToggle;

/**
 * Quick Actions card: one strip per inventory button, in the same order as in the inventory
 * (button 3 on top), each with its icon, what it does and EDIT. The editor is
 * {@link QuickActionDialog}, the same one right-clicking a button in the inventory opens.
 */
public final class QuickActionsRow {
    private static final int ROW_GAP = 4;

    private QuickActionsRow() {}

    public static FeatureRow build(OverlayHost host) {
        Flow.Column column = new Flow.Column(3);
        for (int i = QuickActions.COUNT - 1; i >= 0; i--) column.add(strip(host, i));
        column.add(new FootnoteBox("Click a button in your inventory to use it.",
            "Right-click it there to edit it."));

        return new FeatureRow("Quick Actions", "Inventory buttons for commands and keybinds.")
            .beta()
            .killedKey(QuickActions.KILL_KEY)
            .toggle(() -> QuickActions.enabled, v -> { QuickActions.enabled = v; IslesPlusConfig.save(); })
            .drawer(new Flow.Column(Metrics.DRAWER_GAP)
                .add(new SmallToggle("Button backgrounds", () -> QuickActions.showBackground, () -> {
                    QuickActions.showBackground = !QuickActions.showBackground;
                    IslesPlusConfig.save();
                }))
                .add(column));
    }

    private static Widget strip(OverlayHost host, int index) {
        Runnable edit = () -> QuickActionDialog.open(host, index, null);
        Widget row = new Flow.WrapRow(ROW_GAP, ROW_GAP)
            .add(new ItemButton(() -> QuickActions.iconStack(QuickActions.actions[index]), edit))
            .add(new Label(() -> summary(index), Theme.TEXT_STRONG, Fonts.BODY).wrap())
            .add(new Button("Edit", Button.Kind.SECONDARY, edit).small());
        return new Panel(row, 4, Theme.SURFACE, Theme.RAISED, Theme.SECONDARY, Theme.SURFACE_RING);
    }

    private static String summary(int index) {
        QuickAction a = QuickActions.actions[index];
        return (index + 1) + ": " + (a.isSet() ? QuickActions.describe(a) : "Empty");
    }
}
