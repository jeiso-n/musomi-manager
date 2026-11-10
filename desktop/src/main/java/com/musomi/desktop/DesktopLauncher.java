package com.musomi.desktop;

/**
 * Non-JavaFX launcher entry point for packaged Windows distributions.
 * Keeping the launcher separate from the {@link javafx.application.Application}
 * subclass avoids Java launcher special-casing when starting a packaged app.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        DesktopApplication.main(args);
    }
}
