package com.musomi.desktop.util;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Lightweight interaction polish for the desktop client: buttons gently scale
 * up on hover and press in on click. Installed once per scene root after
 * navigation; nodes loaded later can call {@link #install(Node)} themselves.
 */
public final class UiMotion {

    private static final Duration HOVER = Duration.millis(140);
    private static final Duration PRESS = Duration.millis(90);
    private static final String MARKER = "musomi.motion";

    private UiMotion() {
    }

    /** Walks {@code root} and installs motion on every button found. */
    public static void install(Node root) {
        if (root == null) {
            return;
        }
        if (root instanceof ButtonBase button) {
            installButton(button);
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                install(child);
            }
        }
    }

    private static void installButton(ButtonBase button) {
        if (button.getProperties().containsKey(MARKER)) {
            return;
        }
        button.getProperties().put(MARKER, Boolean.TRUE);

        button.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            if (!button.isDisabled()) {
                scale(button, 1.02, HOVER);
            }
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, event -> scale(button, 1.0, HOVER));
        button.pressedProperty().addListener((obs, wasPressed, isPressed) -> {
            if (button.isDisabled()) {
                return;
            }
            double target = isPressed ? 0.97 : (button.isHover() ? 1.02 : 1.0);
            scale(button, target, PRESS);
        });
    }

    private static void scale(Node node, double target, Duration duration) {
        ScaleTransition transition = new ScaleTransition(duration, node);
        transition.setToX(target);
        transition.setToY(target);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
    }
}
