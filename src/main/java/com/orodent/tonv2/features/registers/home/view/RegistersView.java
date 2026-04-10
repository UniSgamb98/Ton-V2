package com.orodent.tonv2.features.registers.home.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegistersView extends VBox {
    private final AppHeader header;
    private final TextField articleField;
    private final TextField lotField;

    public RegistersView() {
        header = new AppHeader("Registri");
        articleField = new TextField();
        lotField = new TextField();

        setSpacing(20);
        setPadding(new Insets(20));

        VBox articleBox = new VBox(8, new Label("Articolo"), articleField);
        VBox lotBox = new VBox(8, new Label("Lotto"), lotField);
        HBox filtersBox = new HBox(20, articleBox, lotBox);

        getChildren().addAll(header, filtersBox);
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getArticleField() {
        return articleField;
    }

    public TextField getLotField() {
        return lotField;
    }
}
