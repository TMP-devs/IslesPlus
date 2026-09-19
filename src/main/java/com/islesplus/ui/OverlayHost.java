package com.islesplus.ui;
public interface OverlayHost {
    void openOverlay(Widget overlay);
    void closeOverlay(Widget overlay);
    int screenWidth();
    int screenHeight();
    void closeScreen();
}
