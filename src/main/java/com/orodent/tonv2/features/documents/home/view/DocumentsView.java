package com.orodent.tonv2.features.documents.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DocumentsView extends VBox {

    private final AppHeader header;

    public DocumentsView() {
        header = new AppHeader("Documentazione");

        Label content = new Label("Sezione documentazione");

        setSpacing(20);
        setPadding(new Insets(20));

        getChildren().addAll(header, content);
    }

    public AppHeader getHeader() {
        return header;
    }
}
