package com.islesplus.screen.islesscreen;

import com.islesplus.IslesPlusConfig;
import com.islesplus.features.quickactions.IslesAction;
import com.islesplus.features.quickactions.QuickAction;
import com.islesplus.features.quickactions.QuickActions;
import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.Metrics;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;
import com.islesplus.ui.widgets.Button;
import com.islesplus.ui.widgets.CheckTile;
import com.islesplus.ui.widgets.Dialog;
import com.islesplus.ui.widgets.Dropdown;
import com.islesplus.ui.widgets.FootnoteBox;
import com.islesplus.ui.widgets.ItemButton;
import com.islesplus.ui.widgets.ItemPickerDialog;
import com.islesplus.ui.widgets.Label;
import com.islesplus.ui.widgets.TextField;

/**
 * Editor for one quick action button: its icon (item picker), what kind of action it is, and
 * the action itself (an Isles+ action or a command). Every change saves at once.
 */
public final class QuickActionDialog {
    private static final int DIALOG_W = 240;
    private static final int GAP = 4;

    private QuickActionDialog() {}

    public static void open(OverlayHost host, int index, Runnable onClose) {
        QuickAction a = QuickActions.actions[index];
        Widget[] dialogRef = new Widget[1];

        // Actions the json has "killed" are left out of the picker; re-read on every call so
        // a refreshed features json takes effect with the dialog open.
        java.util.function.Supplier<IslesAction[]> isles = () -> java.util.Arrays.stream(IslesAction.values())
            .filter(x -> !x.hidden()).toArray(IslesAction[]::new);

        // Icon
        ItemButton icon = new ItemButton(() -> QuickActions.iconStack(a),
            () -> ItemPickerDialog.open(host, "Choose an icon", () -> a.icon, id -> {
                a.icon = id;
                IslesPlusConfig.save();
            }));
        Button defaultIcon = new Button("Default", Button.Kind.QUIET, () -> {
            a.icon = "";
            IslesPlusConfig.save();
        }).small();
        Widget iconRow = new Flow.WrapRow(GAP, GAP)
            .add(icon)
            .add(new Label(() -> a.icon.isEmpty() ? "Icon: default (click to choose)"
                : "Icon: " + QuickActions.iconStack(a).getName().getString(), Theme.TEXT_LABEL, Fonts.SMALL).wrap())
            .add(defaultIcon);

        // Action kind
        CheckTile islesTile = new CheckTile("Isles+", () -> a.type == QuickAction.Type.ISLES,
            () -> setType(a, QuickAction.Type.ISLES));
        String[] draft = {a.type == QuickAction.Type.COMMAND ? "/" + a.target : ""};
        CheckTile cmdTile = new CheckTile("Command", () -> a.type == QuickAction.Type.COMMAND, () -> {
            setType(a, QuickAction.Type.COMMAND);
            // back to a command typed earlier in this dialog: it is still in the field, so keep it
            a.target = QuickAction.normalizeCommand(draft[0]);
            IslesPlusConfig.save();
        });
        Flow.Grid kinds = new Flow.Grid(2, GAP, 0).minCellWidth(() ->
            Math.max(islesTile.naturalWidth(), cmdTile.naturalWidth()));
        kinds.add(islesTile).add(cmdTile);

        // The action itself: one of two controls, shown by kind
        Label islesLabel = new Label("Isles+ action (type to search)", Theme.TEXT_LABEL, Fonts.SMALL);
        // A refresh on the fetch thread can shrink the list between the dropdown's layout and a
        // draw or click, so every lookup is bounds-checked (out of range = nothing there).
        java.util.function.IntFunction<IslesAction> at = i -> {
            IslesAction[] list = isles.get();
            return i >= 0 && i < list.length ? list[i] : null;
        };
        Dropdown islesPick = new Dropdown(host, Dropdown.Style.DARK, () -> isles.get().length,
            i -> { IslesAction x = at.apply(i); return x == null ? "" : x.label; },
            i -> { IslesAction x = at.apply(i); return x != null && x.killed() ? "Disabled" : null; },
            () -> {
                IslesAction cur = IslesAction.byId(a.target);
                return cur == null ? -1 : java.util.Arrays.asList(isles.get()).indexOf(cur);
            },
            i -> {
                IslesAction x = at.apply(i);
                if (x == null) return;
                a.target = x.id;
                IslesPlusConfig.save();
            });

        Label cmdLabel = new Label("Command", Theme.TEXT_LABEL, Fonts.SMALL);
        TextField cmdField = new TextField(() -> draft[0], s -> draft[0] = s, "/trash", 256)
            .onBlur(() -> {
                if (a.type != QuickAction.Type.COMMAND) return;
                a.target = QuickAction.normalizeCommand(draft[0]);
                IslesPlusConfig.save();
            });

        FootnoteBox noneHelp = new FootnoteBox("Pick what this button does.");
        FootnoteBox islesHelp = new FootnoteBox("Runs an Isles+ action without closing your inventory.");
        FootnoteBox cmdHelp = new FootnoteBox("Sends the command as if you typed it. The / is optional.");

        Widget buttons = new Flow.WrapRow(GAP, GAP)
            .add(new Button("Clear", Button.Kind.SECONDARY, () -> {
                a.clear();
                draft[0] = "";
                IslesPlusConfig.save();
            }))
            .add(new Button("Done", Button.Kind.PRIMARY, () -> host.closeOverlay(dialogRef[0])))
            .align(Flow.Align.END);

        Widget body = new Flow.Column(Metrics.DRAWER_GAP) {
            @Override public int layout(int x, int y, int width) {
                QuickAction.Type t = a.type;
                islesLabel.visible = islesPick.visible = islesHelp.visible = t == QuickAction.Type.ISLES;
                cmdLabel.visible = cmdField.visible = cmdHelp.visible = t == QuickAction.Type.COMMAND;
                noneHelp.visible = t == QuickAction.Type.NONE;
                return super.layout(x, y, width);
            }
        }
            .add(iconRow)
            .add(new Label("What it does", Theme.TEXT_LABEL, Fonts.BODY))
            .add(kinds)
            .add(islesLabel).add(islesPick)
            .add(cmdLabel).add(cmdField)
            .add(noneHelp).add(islesHelp).add(cmdHelp)
            .add(buttons);

        Dialog dialog = new Dialog(host, "Quick action " + (index + 1), body, onClose).width(DIALOG_W);
        dialogRef[0] = dialog;
        host.openOverlay(dialog);
    }

    /** Switching kind starts the new kind fresh; an Isles+ action starts on the first entry so
     * the button works straight away. */
    private static void setType(QuickAction a, QuickAction.Type type) {
        if (a.type == type) return;
        a.type = type;
        a.target = type == QuickAction.Type.ISLES ? IslesAction.values()[0].id : "";
        IslesPlusConfig.save();
    }
}
