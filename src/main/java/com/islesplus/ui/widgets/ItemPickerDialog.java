package com.islesplus.ui.widgets;

import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** "Choose an item" pop-up: a search field (focused, type straight away) over an {@link ItemGrid},
 * and the name of the item under the mouse. Picking one closes it. */
public final class ItemPickerDialog {
    private static final int DIALOG_W = 216;

    private ItemPickerDialog() {}

    public static void open(OverlayHost host, String title, Supplier<String> currentId, Consumer<String> onPick) {
        Widget[] dialogRef = new Widget[1];
        String[] query = {""};

        TextField search = new TextField(() -> query[0], s -> query[0] = s, "Search items...", 40);
        ItemGrid grid = new ItemGrid(() -> query[0], currentId, id -> {
            onPick.accept(id);
            host.closeOverlay(dialogRef[0]);
        });
        Label hovered = new Label(() -> grid.hoveredName().isEmpty() ? "Hover an item to see its name" : grid.hoveredName(),
            Theme.TEXT_LABEL, Fonts.SMALL);

        Widget body = new Flow.Column(5)
            .add(search)
            .add(grid)
            .add(hovered);

        Dialog dialog = new Dialog(host, title, body, null).width(DIALOG_W);
        dialogRef[0] = dialog;
        host.openOverlay(dialog);
        search.focus();
    }
}
