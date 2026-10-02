package com.orodent.tonv2.features.inventory.importing.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.inventory.importing.presentation.InventoryImportViewState;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class InventorySalesUpdateView extends VBox {
    private final AppHeader header = new AppHeader("Registri - Aggiorna vendite");
    private final Button selectFileButton = new Button("Seleziona file CSV");
    private final Label selectedFileLabel = new Label("Nessun file selezionato");
    private final Label totalRowsValue = metricValue();
    private final Label validRowsValue = metricValue();
    private final Label errorRowsValue = metricValue();
    private final Label statusLabel = new Label("Seleziona un file per avviare la verifica.");
    private final ListView<String> errorsList = new ListView<>();
    private final ProgressIndicator progress = new ProgressIndicator();

    public InventorySalesUpdateView() {
        selectFileButton.getStyleClass().add("sales-import-primary-action");
        selectedFileLabel.getStyleClass().add("sales-import-file-name");
        statusLabel.getStyleClass().add("sales-import-status");
        statusLabel.setWrapText(true);
        progress.setMaxSize(24, 24);
        progress.setVisible(false);
        progress.setManaged(false);

        VBox uploadCard = new VBox(12,
                sectionTitle("File vendite"),
                new Label("Formato previsto: CSV separato da punto e virgola con intestazioni."),
                new HBox(12, selectFileButton, selectedFileLabel));
        uploadCard.getStyleClass().add("sales-import-card");

        FlowPane metrics = new FlowPane(12, 12,
                metric("Righe lette", totalRowsValue),
                metric("Righe valide", validRowsValue),
                metric("Errori", errorRowsValue));
        VBox resultCard = new VBox(14, sectionTitle("Esito verifica"), metrics,
                new HBox(10, progress, statusLabel));
        resultCard.getStyleClass().add("sales-import-card");

        errorsList.setPrefHeight(220);
        errorsList.getStyleClass().add("sales-import-errors");
        VBox errorsCard = new VBox(12, sectionTitle("Dettaglio errori"), errorsList);
        errorsCard.getStyleClass().add("sales-import-card");
        errorsCard.visibleProperty().bind(Bindings.isNotEmpty(errorsList.getItems()));
        errorsCard.managedProperty().bind(errorsCard.visibleProperty());

        Label title = new Label("Aggiorna vendite");
        title.getStyleClass().add("sales-import-title");
        Label subtitle = new Label("Carica il riepilogo del gestionale e verifica i dati prima dell'aggiornamento.");
        subtitle.getStyleClass().add("sales-import-subtitle");
        VBox content = new VBox(22, new VBox(6, title, subtitle), uploadCard, resultCard, errorsCard);
        content.setMaxWidth(980);
        StackPane wrapper = new StackPane(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(30, 28, 40, 28));
        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("sales-import-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getStyleClass().add("sales-import-view");
        getChildren().addAll(header, scroll);
    }

    private VBox metric(String labelText, Label value) {
        Label label = new Label(labelText.toUpperCase());
        label.getStyleClass().add("sales-import-metric-label");
        VBox card = new VBox(4, label, value);
        card.setPrefWidth(190);
        card.getStyleClass().add("sales-import-metric");
        return card;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sales-import-section-title");
        return label;
    }

    private static Label metricValue() {
        Label label = new Label("—");
        label.getStyleClass().add("sales-import-metric-value");
        return label;
    }

    public AppHeader getHeader() { return header; }
    public Button getSelectFileButton() { return selectFileButton; }

    public void showSelectedFile(String name) { selectedFileLabel.setText(name); }

    public void showLoading() {
        selectFileButton.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        statusLabel.setText("Analisi del CSV in corso…");
        statusLabel.getStyleClass().removeAll("sales-import-status-valid", "sales-import-status-error");
    }

    public void render(InventoryImportViewState state) {
        finishLoading();
        totalRowsValue.setText(state.totalRowsText());
        validRowsValue.setText(state.validRowsText());
        errorRowsValue.setText(state.errorRowsText());
        statusLabel.setText(state.statusText());
        statusLabel.getStyleClass().removeAll("sales-import-status-valid", "sales-import-status-error");
        statusLabel.getStyleClass().add(state.valid() ? "sales-import-status-valid" : "sales-import-status-error");
        errorsList.getItems().setAll(state.errors());
    }

    public void showError(String message) {
        finishLoading();
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("sales-import-status-valid", "sales-import-status-error");
        statusLabel.getStyleClass().add("sales-import-status-error");
    }

    private void finishLoading() {
        selectFileButton.setDisable(false);
        progress.setVisible(false);
        progress.setManaged(false);
    }
}
