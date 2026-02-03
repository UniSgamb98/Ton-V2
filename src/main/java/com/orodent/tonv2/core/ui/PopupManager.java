package com.orodent.tonv2.core.ui;

import javafx.scene.Node;

import java.util.function.Function;

public class PopupManager {

    private final HoverPopup popup;

    public PopupManager() {
        this.popup = new HoverPopup();
    }

    /**
     * Crea il popup quando il mouse passa o quando clicchi.
     * generateContent: funzione che genera il contenuto del popup.
     */
    public void attach(Node target, Function<Node, Node> generateContent) {

        // HOVER
        target.setOnMouseEntered(e -> {
            popup.setContent(generateContent.apply(target));
            popup.show(target, e.getScreenX(), e.getScreenY());
        });

        //TRACKING DEL MOUSE
        target.setOnMouseMoved(e -> {
            popup.show(target, e.getScreenX(), e.getScreenY());
        });

        // USCITA MOUSE
        target.setOnMouseExited(e -> popup.hide());
    }
}
