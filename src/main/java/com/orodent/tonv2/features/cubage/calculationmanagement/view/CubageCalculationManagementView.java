package com.orodent.tonv2.features.cubage.calculationmanagement.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CubageCalculationManagementView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label titleLabel = new Label("Gestione Calcoli Cubaggio");
    private final Label infoLabel = new Label("Caricamento...");

    public CubageCalculationManagementView() {
        setSpacing(16);
        setPadding(new Insets(20));

        titleLabel.getStyleClass().add("page-title");
        infoLabel.setWrapText(true);

        getChildren().addAll(header, titleLabel, infoLabel);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setInfoText(String text) {
        infoLabel.setText(text == null ? "" : text);
    }
}
