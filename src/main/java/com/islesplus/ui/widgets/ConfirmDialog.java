package com.islesplus.ui.widgets;

import com.islesplus.ui.Flow;
import com.islesplus.ui.Fonts;
import com.islesplus.ui.OverlayHost;
import com.islesplus.ui.Theme;
import com.islesplus.ui.Widget;

/**
 * "Are you sure?" card for an action that cannot be undone: the shared {@link Dialog} with a
 * message, a quiet CANCEL and an oxblood confirm button. Closing it any other way (the close
 * button, Escape) is the same as CANCEL - nothing happens.
 */
public final class ConfirmDialog {
    private static final int DIALOG_W = 260;

    private ConfirmDialog() {}

    public static void open(OverlayHost host, String title, String message, String confirmLabel, Runnable onConfirm) {
        Widget[] dialogRef = new Widget[1];

        Widget body = new Flow.Column(9)
            .add(new Label(message, Theme.TEXT_LABEL, Fonts.BODY).wrap())
            .add(new Flow.WrapRow(4, 4)
                .add(new Button("Cancel", Button.Kind.QUIET, () -> host.closeOverlay(dialogRef[0])).fill())
                .add(new Button(confirmLabel, Button.Kind.PRIMARY, () -> {
                    onConfirm.run();
                    host.closeOverlay(dialogRef[0]);
                }).fill()));

        Dialog dialog = new Dialog(host, title, body, null).width(DIALOG_W);
        dialogRef[0] = dialog;
        host.openOverlay(dialog);
    }
}
