package com.orodent.tonv2.features.laboratory.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class LaboratoryView extends VBox {
    private final AppHeader header;
    private final Button createCompositionButton;
    private final Button createDiskModelButton;

    public LaboratoryView() {
        header = new AppHeader("Laboratorio");

        createCompositionButton = new Button("Nuova Composizione");
        createDiskModelButton = new Button("Nuovo modello disco");

        setSpacing(20);
        setPadding(new Insets(20));

        HBox content = new HBox(20, createCompositionButton, createDiskModelButton);

        getChildren().addAll(header, content);
    }

    public Button getCreateCompositionButton() { return createCompositionButton; }
    public Button getCreateDiskModelButton() {
        return createDiskModelButton;
    }
    public AppHeader getHeader() {
        return header;
    }
}
