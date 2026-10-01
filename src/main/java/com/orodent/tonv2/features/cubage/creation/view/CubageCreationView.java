package com.orodent.tonv2.features.cubage.creation.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CubageCreationView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label titleLabel = new Label("Gestione Calcoli Cubaggio");

    private final ComboBox<String> calculationSetSelector = new ComboBox<>();

    private final ComboBox<CubageCreationService.PayloadOption> payloadSelector = new ComboBox<>();
    private final Button selectLegacyPayloadButton = new Button("Seleziona Payload Legacy");
    private final ComboBox<CubageCreationService.PayloadOption> legacyPayloadSelector = new ComboBox<>();
    private final TextArea payloadPreviewArea = new TextArea();
    private final TextArea formulaBuilderArea = new TextArea();
    private final TextArea resultsArea = new TextArea();
    private final Button saveCalculationSetButton = new Button("Salva Set di Calcolo");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label();

    public CubageCreationView() {
        setSpacing(16);
        setPadding(new Insets(20));

        titleLabel.getStyleClass().add("page-title");

        calculationSetSelector.setPromptText("Seleziona set calcolo");
        calculationSetSelector.setMaxWidth(420);

        payloadSelector.setPromptText("Seleziona payload attivo");
        payloadSelector.setCellFactory(listView -> new PayloadOptionListCell());
        payloadSelector.setButtonCell(new PayloadOptionListCell());

        legacyPayloadSelector.setPromptText("Seleziona versione legacy");
        legacyPayloadSelector.setVisible(false);
        legacyPayloadSelector.setManaged(false);
        legacyPayloadSelector.setCellFactory(listView -> new PayloadOptionListCell());
        legacyPayloadSelector.setButtonCell(new PayloadOptionListCell());

        payloadPreviewArea.setEditable(false);
        payloadPreviewArea.setWrapText(true);
        payloadPreviewArea.setText("Nessun payload selezionato.");

        formulaBuilderArea.setWrapText(true);
        formulaBuilderArea.setPromptText("Una formula per riga: variabile = espressione");
        formulaBuilderArea.setText("");

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setText("Nessuna validazione eseguita.");

        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        VBox leftPanel = buildLeftPanel();
        VBox centerPanel = buildCenterPanel();
        VBox rightPanel = buildRightPanel();

        HBox contentRow = new HBox(16, leftPanel, centerPanel, rightPanel);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        leftPanel.setPrefWidth(260);
        centerPanel.setPrefWidth(420);
        rightPanel.setPrefWidth(320);

        HBox footerActions = new HBox(10, progressIndicator, statusLabel, saveCalculationSetButton);
        footerActions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(
                header,
                titleLabel,
                calculationSetSelector,
                contentRow,
                footerActions
        );
    }

    public AppHeader getHeader() {
        return header;
    }

    public ComboBox<String> getCalculationSetSelector() {
        return calculationSetSelector;
    }

    public Button getSaveCalculationSetButton() {
        return saveCalculationSetButton;
    }


    public String getFormulaBuilderText() {
        return formulaBuilderArea.getText();
    }

    public void setResultsText(String text) {
        resultsArea.setText(text == null ? "" : text);
    }
    public ComboBox<CubageCreationService.PayloadOption> getPayloadSelector() {
        return payloadSelector;
    }

    public ComboBox<CubageCreationService.PayloadOption> getLegacyPayloadSelector() {
        return legacyPayloadSelector;
    }

    public Button getSelectLegacyPayloadButton() {
        return selectLegacyPayloadButton;
    }

    public void setPayloadPreviewText(String text) {
        payloadPreviewArea.setText(text == null ? "" : text);
    }

    public void setLegacySelectorVisible(boolean visible) {
        legacyPayloadSelector.setVisible(visible);
        legacyPayloadSelector.setManaged(visible);
    }

    public void setSelectLegacyPayloadButtonText(String text) {
        selectLegacyPayloadButton.setText(text);
    }

    public void setPayloadOptions(ObservableList<CubageCreationService.PayloadOption> options) {
        payloadSelector.setItems(options);
    }

    public void setLegacyPayloadOptions(ObservableList<CubageCreationService.PayloadOption> options) {
        legacyPayloadSelector.setItems(options);
    }

    public void showInitialLoading() {
        setBusy(true, "Caricamento dati di cubaggio...");
        setInputsDisabled(true);
    }

    public void showPayloadLoading() {
        setBusy(true, "Caricamento payload...");
    }

    public void showSaving() {
        setBusy(true, "Salvataggio set di calcolo...");
        setInputsDisabled(true);
    }

    public void showLoadSuccess() {
        setBusy(false, "");
        setInputsDisabled(false);
    }

    public void showLoadError(String message) {
        setBusy(false, message);
        setInputsDisabled(false);
        setResultsText(message);
    }

    public void showValidationError(String message) {
        setBusy(false, "Validazione non superata");
        setInputsDisabled(false);
        setResultsText(message);
    }

    public void showSaveSuccess(String message) {
        setBusy(false, "Salvataggio completato");
        setInputsDisabled(false);
        setResultsText(message);
    }

    public void showSaveError(String message) {
        setBusy(false, message);
        setInputsDisabled(false);
        setResultsText(message);
    }

    private void setBusy(boolean busy, String status) {
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
        statusLabel.setText(status == null ? "" : status);
        statusLabel.setVisible(status != null && !status.isBlank());
        statusLabel.setManaged(statusLabel.isVisible());
    }

    private void setInputsDisabled(boolean disabled) {
        calculationSetSelector.setDisable(disabled);
        payloadSelector.setDisable(disabled);
        legacyPayloadSelector.setDisable(disabled);
        selectLegacyPayloadButton.setDisable(disabled);
        formulaBuilderArea.setDisable(disabled);
        saveCalculationSetButton.setDisable(disabled);
    }

    private VBox buildLeftPanel() {
        Label panelTitle = new Label("Selezione Payload");
        panelTitle.getStyleClass().add("section-title");

        VBox panel = new VBox(10, panelTitle, payloadSelector, selectLegacyPayloadButton, legacyPayloadSelector);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("card");
        return panel;
    }

    private VBox buildCenterPanel() {
        Label payloadTitle = new Label("Payload");
        payloadTitle.getStyleClass().add("section-title");
        Label formulasTitle = new Label("Creazione Formule");
        formulasTitle.getStyleClass().add("section-title");

        BorderPane panel = new BorderPane();
        panel.setTop(new VBox(8, payloadTitle, payloadPreviewArea));
        panel.setCenter(new VBox(8, formulasTitle, formulaBuilderArea));
        BorderPane.setMargin(payloadPreviewArea, new Insets(0, 0, 8, 0));

        VBox wrapper = new VBox(panel);
        wrapper.setPadding(new Insets(12));
        wrapper.getStyleClass().add("card");
        VBox.setVgrow(panel, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildRightPanel() {
        Label panelTitle = new Label("Risultati");
        panelTitle.getStyleClass().add("section-title");

        VBox panel = new VBox(10, panelTitle, resultsArea);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("card");
        VBox.setVgrow(resultsArea, Priority.ALWAYS);
        return panel;
    }
}
