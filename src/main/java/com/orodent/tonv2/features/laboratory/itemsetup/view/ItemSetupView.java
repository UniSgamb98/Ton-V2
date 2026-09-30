package com.orodent.tonv2.features.laboratory.itemsetup.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ItemSetupView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Nuovo Articolo");

    private final Label productLabel = new Label("Prodotto");
    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final Button activateLatestCompositionButton = new Button("Imposta ultima composizione come attiva");

    private final Label newItemLabel = new Label("Nuovo item");
    private final TextField itemCodeField = new TextField();
    private final TextField heightField = new TextField();
    private final Button createItemButton = new Button("Crea item");

    private final Label feedbackLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    public ItemSetupView() {
        getStyleClass().add("item-setup-view");

        setSpacing(16);
        setPadding(new Insets(20));

        productLabel.getStyleClass().add("section-label");
        newItemLabel.getStyleClass().add("section-label");

        productSelector.setPromptText("Seleziona prodotto");
        productSelector.setMaxWidth(Double.MAX_VALUE);

        activateLatestCompositionButton.getStyleClass().add("secondary-button");

        itemCodeField.setPromptText("Codice item (es. ZRA2-H18)");
        heightField.setPromptText("Altezza mm (es. 12.5)");

        feedbackLabel.getStyleClass().add("feedback-label");
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        HBox createRow = new HBox(10, itemCodeField, heightField, createItemButton);
        createRow.setAlignment(Pos.CENTER_LEFT);
        createRow.getStyleClass().add("create-row");

        getChildren().addAll(
                header,
                productLabel,
                productSelector,
                activateLatestCompositionButton,
                newItemLabel,
                createRow,
                progressIndicator,
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

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(error ? "feedback-error" : "feedback-success");
    }

    public void showProductsLoading() {
        setLoadingState("Caricamento prodotti...");
    }

    public void showActivationLoading() {
        setLoadingState("Attivazione composizione...");
    }

    public void showCreationLoading() {
        setLoadingState("Creazione item...");
    }

    public void showProducts(List<Product> products) {
        productSelector.getItems().setAll(products);
        finishLoading();
        setFeedback("", false);
    }

    public void showSuccess(String message) {
        finishLoading();
        setFeedback(message, false);
    }

    public void showLoadError(String message) {
        finishLoading();
        setFeedback(message, true);
    }

    private void setLoadingState(String message) {
        setFormDisabled(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        setFeedback(message, false);
    }

    private void finishLoading() {
        setFormDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
    }

    private void setFormDisabled(boolean disabled) {
        productSelector.setDisable(disabled);
        activateLatestCompositionButton.setDisable(disabled);
        itemCodeField.setDisable(disabled);
        heightField.setDisable(disabled);
        createItemButton.setDisable(disabled);
    }
}
