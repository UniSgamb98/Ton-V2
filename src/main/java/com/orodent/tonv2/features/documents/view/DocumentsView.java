package com.orodent.tonv2.features.documents.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DocumentsView extends VBox {
    private final AppHeader header;
    private final Button createCompositionButton;
    private final Button createDiskModelButton;
    private final Button createArticleButton;
    private final Button productionButton;
    private final Button presinterButton;

    public DocumentsView() {
        header = new AppHeader("Documentazione");

        createCompositionButton = new Button("Nuova Composizione");
        createDiskModelButton = new Button("Nuovo Modello Disco");
        createArticleButton = new Button("Nuovo Articolo");
        productionButton = new Button("Produzione");
        presinterButton = new Button("Presinterizza");

        setSpacing(20);
        setPadding(new Insets(20));

        HBox primaryActions = new HBox(20, createCompositionButton, createDiskModelButton, createArticleButton, productionButton);
        HBox secondaryActions = new HBox(20, presinterButton);

        Separator separator = new Separator();
        separator.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(header, primaryActions, separator, secondaryActions);
    }

    public AppHeader getHeader() {
        return header;
    }

    public Button getCreateCompositionButton() {
        return createCompositionButton;
    }

    public Button getCreateDiskModelButton() {
        return createDiskModelButton;
    }

    public Button getCreateArticleButton() {
        return createArticleButton;
    }

    public Button getProductionButton() {
        return productionButton;
    }

    public Button getPresinterButton() {
        return presinterButton;
    }
}
