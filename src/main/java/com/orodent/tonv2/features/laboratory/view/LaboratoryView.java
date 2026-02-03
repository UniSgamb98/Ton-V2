package com.orodent.tonv2.features.laboratory.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class LaboratoryView extends VBox {
    private final AppHeader header;
    private final Button createCompositionButton;
    private final Button placeHolder;

    public LaboratoryView() {
        header = new AppHeader("Laboratorio");

        createCompositionButton = new Button("Nuova Composizione");
        placeHolder = new Button("PlaceHolder");

        setSpacing(20);
        setPadding(new Insets(20));

        HBox content = new HBox(20, createCompositionButton, placeHolder);

        getChildren().addAll(header, content);
    }

    public Button getCreateCompositionButton() { return createCompositionButton; }
    public Button getPlaceHolder() {
        return placeHolder;
    }
    public AppHeader getHeader() {
        return header;
    }
}
