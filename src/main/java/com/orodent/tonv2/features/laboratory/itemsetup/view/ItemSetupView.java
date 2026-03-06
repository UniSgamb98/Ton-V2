package com.orodent.tonv2.features.laboratory.itemsetup.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ItemSetupView extends VBox {

    private final AppHeader header = new AppHeader("Setup Item + Composizione");
    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final Label activeCompositionLabel = new Label("Composizione attiva: -");
    private final Button activateLatestCompositionButton = new Button("Imposta ultima composizione come attiva");

    private final TextField itemCodeField = new TextField();
    private final TextField heightField = new TextField();
    private final Button createItemButton = new Button("Crea item");

    private final Label feedbackLabel = new Label();

    public ItemSetupView() {
        setSpacing(16);
        setPadding(new Insets(20));

        productSelector.setPromptText("Seleziona prodotto");
        productSelector.setMaxWidth(Double.MAX_VALUE);

        itemCodeField.setPromptText("Codice item (es. ZRA2-H18)");
        heightField.setPromptText("Altezza mm (es. 12.5)");

        HBox createRow = new HBox(10, itemCodeField, heightField, createItemButton);
        createRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                header,
                new Label("Prodotto"),
                productSelector,
                activeCompositionLabel,
                activateLatestCompositionButton,
                new Label("Nuovo item"),
                createRow,
                feedbackLabel
        );
    }

    public AppHeader getHeader() {
        return header;
    }

    public ComboBox<Product> getProductSelector() {
        return productSelector;
    }

    public Button getActivateLatestCompositionButton() {
        return activateLatestCompositionButton;
    }

    public TextField getItemCodeField() {
        return itemCodeField;
    }

    public TextField getHeightField() {
        return heightField;
    }

    public Button getCreateItemButton() {
        return createItemButton;
    }

    public void setActiveCompositionText(String text) {
        activeCompositionLabel.setText(text);
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
