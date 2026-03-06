package com.orodent.tonv2.features.laboratory.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class LaboratoryView extends VBox {
    private final AppHeader header;
    private final Button createCompositionButton;
    private final Button createDiskModelButton;
    private final Button produceButton;
    private final Button createArticleButton;
    private final Button presinterButton;

    public LaboratoryView() {
        header = new AppHeader("Laboratorio");

        createCompositionButton = new Button("Nuova Composizione");
        createDiskModelButton = new Button("Nuovo Modello Disco");
        createArticleButton = new Button("Nuovo Articolo");
        produceButton = new Button("Produci");
        presinterButton = new Button("Presinterizza");

        setSpacing(20);
        setPadding(new Insets(20));

        HBox primaryActions = new HBox(20, createCompositionButton, createDiskModelButton, createArticleButton);
        HBox secondaryActions = new HBox(20, produceButton, presinterButton);

        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(header, primaryActions, separator, secondaryActions);
    }

    public Button getCreateCompositionButton() { return createCompositionButton; }
    public Button getCreateDiskModelButton() {
        return createDiskModelButton;
    }
    public Button getProduceButton() {
        return produceButton;
    }
    public Button getCreateArticleButton() {
        return createArticleButton;
    }
    public Button getPresinterButton() {
        return presinterButton;
    }
    public AppHeader getHeader() {
        return header;
    }
}
