package com.orodent.tonv2.features.documents.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DocumentsView extends VBox {
    private final AppHeader header;
    private final Button createDocumentButton;
    private final Button archiveButton;
    private final Button searchButton;

    public DocumentsView() {
        header = new AppHeader("Documenti");

        createDocumentButton = new Button("Nuovo Documento");
        archiveButton = new Button("Archivio");
        searchButton = new Button("Ricerca");

        setSpacing(20);
        setPadding(new Insets(20));

        HBox documentActions = new HBox(20, createDocumentButton, archiveButton, searchButton);

        getChildren().addAll(header, documentActions);
    }

    public AppHeader getHeader() {
        return header;
    }

    public Button getCreateDocumentButton() {
        return createDocumentButton;
    }

    public Button getArchiveButton() {
        return archiveButton;
    }

    public Button getSearchButton() {
        return searchButton;
    }
}
