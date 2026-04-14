package com.orodent.tonv2.features.cubage.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CubageView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label introLabel = new Label("Caricamento...");

    public CubageView() {
        setSpacing(16);
        setPadding(new Insets(20));

        introLabel.setWrapText(true);
        getChildren().addAll(header, introLabel);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setIntroText(String text) {
        introLabel.setText(text == null ? "" : text);
    }
}
