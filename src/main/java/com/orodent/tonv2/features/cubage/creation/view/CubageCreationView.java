package com.orodent.tonv2.features.cubage.creation.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.cubage.creation.service.CubageCreationService;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CubageCreationView extends VBox {

    private final AppHeader header = new AppHeader("Cubaggio");
    private final Label titleLabel = new Label("Gestione Calcoli Cubaggio");

    private final TextField calculationSetNameField = new TextField();

    private final ComboBox<CubageCreationService.PayloadOption> payloadSelector = new ComboBox<>();
    private final Button selectLegacyPayloadButton = new Button("Seleziona Payload Legacy");
    private final ComboBox<CubageCreationService.PayloadOption> legacyPayloadSelector = new ComboBox<>();
    private final TextArea payloadPreviewArea = new TextArea();
    private final TextArea formulaBuilderArea = new TextArea();
    private final TextArea resultsArea = new TextArea();
    private final Button saveCalculationSetButton = new Button("Salva Set di Calcolo");

    public CubageCreationView() {
        setSpacing(16);
        setPadding(new Insets(20));

        titleLabel.getStyleClass().add("page-title");

        calculationSetNameField.setPromptText("Nome set di calcolo");
        calculationSetNameField.setMaxWidth(420);

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
        formulaBuilderArea.setText("""
                Esempio formula:
                calc_volume_density_ratio = input_1 / input_2
                """);

        resultsArea.setEditable(false);
        resultsArea.setWrapText(true);
        resultsArea.setText("Nessuna validazione eseguita.");

        VBox leftPanel = buildLeftPanel();
        VBox centerPanel = buildCenterPanel();
        VBox rightPanel = buildRightPanel();

        HBox contentRow = new HBox(16, leftPanel, centerPanel, rightPanel);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        leftPanel.setPrefWidth(260);
        centerPanel.setPrefWidth(420);
        rightPanel.setPrefWidth(320);

        HBox footerActions = new HBox(saveCalculationSetButton);
        footerActions.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(
                header,
                titleLabel,
                calculationSetNameField,
                contentRow,
                footerActions
        );
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getCalculationSetNameField() {
        return calculationSetNameField;
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
