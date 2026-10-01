package com.orodent.tonv2.features.laboratory.diskmodel.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.features.laboratory.diskmodel.view.partial.DiskModelPreviewView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class CreateDiskModelView extends VBox {

    private static final double CONTENT_MAX_WIDTH = 1120;

    private final AppHeader header = new AppHeader("Laboratorio - Nuovo Modello Disco");
    private final BorderPane content = new BorderPane();
    private final ScrollPane editorScrollPane = new ScrollPane();

    private final TextField codeField = new TextField();
    private final TextField diameterField = new TextField();
    private final TextField superiorOvermaterialField = new TextField();
    private final TextField inferiorOvermaterialField = new TextField();
    private final TextField pressureField = new TextField();
    private final TextField gramsPerMmField = new TextField();
    private final TextField numLayersField = new TextField();

    private final VBox layersPercentagesBox = new VBox(8);
    private final Label layersSummaryLabel = new Label("Somma layer: 0%");

    private final VBox rangesBox = new VBox(8);
    private final Button addRangeBtn = new Button("Aggiungi fascia altezza");
    private final Button saveBtn = new Button("Salva modello disco");
    private final Button backBtn = new Button("Indietro");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final Label pageTitleLabel = new Label("Nuovo modello disco");

    private final DiskModelPreviewView previewView = new DiskModelPreviewView();

    private final List<HeightRangeRow> rangeRows = new ArrayList<>();
    private final List<LayerPercentageRow> layerRows = new ArrayList<>();

    public CreateDiskModelView() {
        buildLayout();
        bindPreview();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        getStyleClass().add("disk-model-editor");
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    private void buildLayout() {
        codeField.setPromptText("Es. BM-98-A");
        diameterField.setPromptText("Es. 98.0");
        superiorOvermaterialField.setPromptText("Es. 1.2");
        inferiorOvermaterialField.setPromptText("Es. 0.7");
        pressureField.setPromptText("Es. 2300");
        gramsPerMmField.setPromptText("Es. 0.550");
        numLayersField.setPromptText("Es. 4");
        configureFieldWidths();

        GridPane identityGrid = createTwoColumnGrid();
        identityGrid.add(createField("Codice modello", null, codeField), 0, 0);
        identityGrid.add(createField("Diametro", "mm", diameterField), 1, 0);

        GridPane productionGrid = createTwoColumnGrid();
        productionGrid.add(createField("Overmaterial superiore", "mm", superiorOvermaterialField), 0, 0);
        productionGrid.add(createField("Overmaterial inferiore", "mm", inferiorOvermaterialField), 1, 0);
        productionGrid.add(createField("Pressione", "kg/cm²", pressureField), 0, 1);
        productionGrid.add(createField("Peso per millimetro", "g/mm", gramsPerMmField), 1, 1);

        layersPercentagesBox.getStyleClass().add("layer-list");
        layersSummaryLabel.getStyleClass().addAll("layer-total", "layer-total-invalid");
        VBox layersContent = new VBox(12,
                createField("Numero strati", null, numLayersField),
                layersPercentagesBox,
                layersSummaryLabel
        );

        Label rangesDescription = new Label(
                "Personalizza gli overmaterial soltanto per specifici intervalli di altezza."
        );
        rangesDescription.getStyleClass().add("editor-section-description");
        rangesDescription.setWrapText(true);
        addRangeBtn.setOnAction(e -> addRangeRow());
        addRangeBtn.getStyleClass().add("secondary-action");

        VBox leftBox = new VBox(18,
                createSectionCard("1", "Informazioni principali", "Identifica il modello e le sue dimensioni.", identityGrid),
                createSectionCard("2", "Parametri di produzione", "Definisci pressatura e sovramateriali predefiniti.", productionGrid),
                createSectionCard("3", "Struttura degli strati", "Distribuisci il modello assicurandoti che il totale sia 100%.", layersContent),
                createSectionCard("4", "Fasce di altezza", "Configurazione opzionale", rangesDescription,
                rangesBox,
                addRangeBtn)
        );
        leftBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        VBox.setVgrow(layersPercentagesBox, Priority.NEVER);

        VBox previewCard = new VBox(14, previewView);
        previewCard.getStyleClass().add("preview-card");
        previewCard.setMinWidth(250);
        previewCard.setPrefWidth(270);
        previewCard.setMaxWidth(290);

        HBox editorColumns = new HBox(20, leftBox, previewCard);
        editorColumns.setAlignment(Pos.TOP_CENTER);

        pageTitleLabel.getStyleClass().add("editor-page-title");
        Label pageSubtitle = new Label(
                "Configura geometria, parametri di pressatura e struttura degli strati."
        );
        pageSubtitle.getStyleClass().add("editor-page-subtitle");
        VBox pageHeading = new VBox(6, pageTitleLabel, pageSubtitle);

        VBox editorContent = new VBox(24, pageHeading, editorColumns);
        editorContent.setMaxWidth(CONTENT_MAX_WIDTH);

        StackPane centered = new StackPane(editorContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(30, 28, 36, 28));

        editorScrollPane.setContent(centered);
        editorScrollPane.getStyleClass().add("disk-model-scroll");
        editorScrollPane.setFitToWidth(true);
        editorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        saveBtn.getStyleClass().add("primary-save-action");
        backBtn.getStyleClass().add("back-action");
        statusLabel.getStyleClass().add("editor-status");
        HBox bottom = new HBox(12, backBtn, progressIndicator, statusLabel, spacer, saveBtn);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.getStyleClass().add("editor-action-bar");

        content.setCenter(editorScrollPane);
        content.setBottom(bottom);

        numLayersField.textProperty().addListener((obs, oldVal, newVal) -> rebuildLayerRows());
    }

    private void configureFieldWidths() {
        for (TextField field : List.of(
                codeField, diameterField, superiorOvermaterialField, inferiorOvermaterialField,
                pressureField, gramsPerMmField, numLayersField
        )) {
            field.setMaxWidth(Double.MAX_VALUE);
            field.getStyleClass().add("editor-text-field");
        }
        numLayersField.setMaxWidth(220);
    }

    private GridPane createTwoColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(14);
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        column.setHgrow(Priority.ALWAYS);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(column, secondColumn);
        return grid;
    }

    private VBox createField(String labelText, String unit, TextField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("editor-field-label");

        if (unit == null) {
            return new VBox(6, label, field);
        }

        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("editor-field-unit");
        StackPane fieldContainer = new StackPane(field, unitLabel);
        StackPane.setAlignment(unitLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(unitLabel, new Insets(0, 10, 0, 0));
        field.setPadding(new Insets(8, 66, 8, 10));
        return new VBox(6, label, fieldContainer);
    }

    private VBox createSectionCard(String number, String titleText, String descriptionText, Node... contentNodes) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("editor-section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.getStyleClass().add("editor-section-title");
        Label description = new Label(descriptionText);
        description.getStyleClass().add("editor-section-description");
        description.setWrapText(true);

        VBox headingText = new VBox(2, title, description);
        HBox heading = new HBox(12, numberLabel, headingText);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, heading);
        card.getChildren().addAll(contentNodes);
        card.getStyleClass().add("editor-section-card");
        return card;
    }

    private void bindPreview() {
        superiorOvermaterialField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        inferiorOvermaterialField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        refreshPreview();
    }

    private void rebuildLayerRows() {
        int layers = parseIntSafe(numLayersField.getText());
        layerRows.clear();
        layersPercentagesBox.getChildren().clear();

        if (layers <= 0) {
            updateLayerSummary();
            refreshPreview();
            return;
        }

        double defaultPct = 100.0 / layers;
        for (int i = 1; i <= layers; i++) {
            LayerPercentageRow row = new LayerPercentageRow(i, defaultPct);
            row.percentageField.textProperty().addListener((obs, oldVal, newVal) -> {
                updateLayerSummary();
                refreshPreview();
            });
            layerRows.add(row);
            layersPercentagesBox.getChildren().add(row.container);
        }

        updateLayerSummary();
        refreshPreview();
    }

    private void updateLayerSummary() {
        double sum = getLayerPercentageValues().stream().mapToDouble(Double::doubleValue).sum();
        layersSummaryLabel.setText(String.format(java.util.Locale.ROOT, "Somma layer: %.2f%%", sum));
        layersSummaryLabel.getStyleClass().removeAll("layer-total-valid", "layer-total-invalid");
        if (Math.abs(sum - 100.0) < 0.0001) {
            layersSummaryLabel.setText(layersSummaryLabel.getText() + "  ✓");
            layersSummaryLabel.getStyleClass().add("layer-total-valid");
        } else {
            layersSummaryLabel.getStyleClass().add("layer-total-invalid");
        }
    }

    private void refreshPreview() {
        double sup = parseDoubleSafe(superiorOvermaterialField.getText());
        double inf = parseDoubleSafe(inferiorOvermaterialField.getText());
        List<Double> percentages = getLayerPercentageValues();
        if (percentages.isEmpty()) {
            percentages = List.of(100.0);
        }
        previewView.update(sup, inf, percentages);
    }

    private void addRangeRow() {
        HeightRangeRow row = new HeightRangeRow();
        rangeRows.add(row);

        row.removeButton.setOnAction(e -> {
            rangeRows.remove(row);
            rangesBox.getChildren().remove(row.container);
        });

        rangesBox.getChildren().add(row.container);
    }

    public void configureEditMode(boolean editMode) {
        if (editMode) {
            header.setTitle("Laboratorio - Modifica Modello Disco");
            pageTitleLabel.setText("Modifica modello disco");
            saveBtn.setText("Salva Modifiche");
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        } else {
            header.setTitle("Laboratorio - Nuovo Modello Disco");
            pageTitleLabel.setText("Nuovo modello disco");
            saveBtn.setText("Salva modello disco");
            backBtn.setVisible(false);
            backBtn.setManaged(false);
        }
    }

    public AppHeader getHeader() {
        return header;
    }

    public Button getSaveButton() {
        return saveBtn;
    }

    public Button getBackButton() {
        return backBtn;
    }

    public void showInitialLoading() {
        setLoadingState("Caricamento modello...");
    }

    public void showSaving() {
        setLoadingState("Salvataggio modello...");
    }

    public void showLoadSuccess() {
        editorScrollPane.setDisable(false);
        saveBtn.setDisable(false);
        backBtn.setDisable(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("editor-status-error");
    }

    public void showLoadError(String message) {
        editorScrollPane.setDisable(false);
        saveBtn.setDisable(false);
        backBtn.setDisable(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setText(message);
        if (!statusLabel.getStyleClass().contains("editor-status-error")) {
            statusLabel.getStyleClass().add("editor-status-error");
        }
    }

    private void setLoadingState(String message) {
        editorScrollPane.setDisable(true);
        saveBtn.setDisable(true);
        backBtn.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("editor-status-error");
    }

    public String getCode() { return codeField.getText(); }
    public String getDiameter() { return diameterField.getText(); }
    public String getSuperiorOvermaterial() { return superiorOvermaterialField.getText(); }
    public String getInferiorOvermaterial() { return inferiorOvermaterialField.getText(); }
    public String getPressure() { return pressureField.getText(); }
    public String getGramsPerMm() { return gramsPerMmField.getText(); }
    public String getNumLayers() { return numLayersField.getText(); }

    public List<LayerPercentageDraft> getLayerPercentageDrafts() {
        List<LayerPercentageDraft> drafts = new ArrayList<>();
        for (LayerPercentageRow row : layerRows) {
            drafts.add(new LayerPercentageDraft(row.layerNumber, row.percentageField.getText()));
        }
        return drafts;
    }


    public void fillFromModel(String code,
                              Double diameter,
                              Double superiorOvermaterial,
                              Double inferiorOvermaterial,
                              Double pressure,
                              Double gramsPerMm,
                              Integer numLayers,
                              List<Double> layerPercentages,
                              List<HeightRangeDraft> ranges) {
        codeField.setText(valueOrEmpty(code));
        diameterField.setText(formatDouble(diameter));
        superiorOvermaterialField.setText(formatDouble(superiorOvermaterial));
        inferiorOvermaterialField.setText(formatDouble(inferiorOvermaterial));
        pressureField.setText(formatDouble(pressure));
        gramsPerMmField.setText(formatDouble(gramsPerMm));
        numLayersField.setText(numLayers == null ? "" : String.valueOf(numLayers));
        rebuildLayerRows();

        if (layerPercentages != null) {
            for (int i = 0; i < Math.min(layerPercentages.size(), layerRows.size()); i++) {
                layerRows.get(i).percentageField.setText(formatDouble(layerPercentages.get(i)));
            }
        }

        rangeRows.clear();
        rangesBox.getChildren().clear();
        if (ranges != null) {
            for (HeightRangeDraft draft : ranges) {
                HeightRangeRow row = new HeightRangeRow();
                row.minHeightField.setText(valueOrEmpty(draft.minHeight()));
                row.maxHeightField.setText(valueOrEmpty(draft.maxHeight()));
                row.superiorField.setText(valueOrEmpty(draft.superiorOvermaterial()));
                row.inferiorField.setText(valueOrEmpty(draft.inferiorOvermaterial()));
                row.removeButton.setOnAction(e -> {
                    rangeRows.remove(row);
                    rangesBox.getChildren().remove(row.container);
                });
                rangeRows.add(row);
                rangesBox.getChildren().add(row.container);
            }
        }

        updateLayerSummary();
        refreshPreview();
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "";
        }
        return String.format(java.util.Locale.ROOT, "%.4f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public List<HeightRangeDraft> getRangeDrafts() {
        List<HeightRangeDraft> drafts = new ArrayList<>();
        for (HeightRangeRow row : rangeRows) {
            drafts.add(new HeightRangeDraft(
                    row.minHeightField.getText(),
                    row.maxHeightField.getText(),
                    row.superiorField.getText(),
                    row.inferiorField.getText()
            ));
        }
        return drafts;
    }

    private List<Double> getLayerPercentageValues() {
        List<Double> values = new ArrayList<>();
        for (LayerPercentageRow row : layerRows) {
            values.add(parseDoubleSafe(row.percentageField.getText()));
        }
        return values;
    }

    private int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String raw) {
        try {
            String normalized = (raw == null ? "" : raw.trim()).replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public record HeightRangeDraft(String minHeight, String maxHeight, String superiorOvermaterial, String inferiorOvermaterial) {}
    public record LayerPercentageDraft(int layerNumber, String percentage) {}

    private static class LayerPercentageRow {
        private final int layerNumber;
        private final HBox container;
        private final TextField percentageField = new TextField();

        private LayerPercentageRow(int layerNumber, double defaultPercentage) {
            this.layerNumber = layerNumber;
            percentageField.setPromptText("% layer");
            percentageField.setText(String.format(java.util.Locale.ROOT, "%.2f", defaultPercentage));
            percentageField.setMaxWidth(140);
            percentageField.getStyleClass().add("layer-percentage-field");

            container = new HBox(8,
                    new Label("Layer " + layerNumber),
                    percentageField,
                    new Label("%")
            );
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("layer-row");
            HBox.setHgrow(percentageField, Priority.ALWAYS);
        }
    }

    private static class HeightRangeRow {
        private final HBox container;
        private final TextField minHeightField = new TextField();
        private final TextField maxHeightField = new TextField();
        private final TextField superiorField = new TextField();
        private final TextField inferiorField = new TextField();
        private final Button removeButton = new Button("Rimuovi");

        private HeightRangeRow() {
            minHeightField.setPromptText("Min mm");
            maxHeightField.setPromptText("Max mm");
            superiorField.setPromptText("Over sup.");
            inferiorField.setPromptText("Over inf.");
            removeButton.getStyleClass().add("danger-action");

            HBox.setHgrow(minHeightField, Priority.ALWAYS);
            HBox.setHgrow(maxHeightField, Priority.ALWAYS);
            HBox.setHgrow(superiorField, Priority.ALWAYS);
            HBox.setHgrow(inferiorField, Priority.ALWAYS);

            container = new HBox(8,
                    new Label("Da"), minHeightField,
                    new Label("a"), maxHeightField,
                    new Label("Sup"), superiorField,
                    new Label("Inf"), inferiorField,
                    removeButton
            );
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("height-range-row");
        }
    }
}
