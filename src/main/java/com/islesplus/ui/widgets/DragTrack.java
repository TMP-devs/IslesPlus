package com.islesplus.ui.widgets;

import com.islesplus.IslesClient;
import com.islesplus.ui.Widget;

/** Shared click-drag-release state machine for track-style widgets (sliders, rails).
 * Button 0 starts a drag on top of the widget, plays the click sound and applies the
 * value immediately; mouseDragged keeps applying the value while the drag is active;
 * mouseReleased ends the drag and runs onRelease exactly once, only if a drag happened. */
abstract class DragTrack extends Widget {
    private final Runnable onRelease;
    private boolean dragging = false;

    DragTrack(Runnable onRelease) {
        this.onRelease = onRelease;
    }

    /** Compute the value from the mouse position and push it to the setter (live preview). */
    abstract void applyValue(double mx);

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !visible || !contains(mx, my)) return false;
        dragging = true;
        IslesClient.playMenuClickSound();
        applyValue(mx);
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        applyValue(mx);
        return true;
    }

    @Override public void mouseReleased() {
        if (dragging) {
            dragging = false;
            if (onRelease != null) onRelease.run();
        }
    }
}
