package com.orodent.tonv2.features.laboratory.composition.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.components.EditorActionBar;
import com.orodent.tonv2.core.components.NumberedSectionCard;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.composition.presentation.CompositionLayerViewState;
import com.orodent.tonv2.features.laboratory.composition.view.partial.LayerEditorView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CreateCompositionView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1180;

    private final AppHeader header = new AppHeader("Laboratorio - Nuova Composizione");
    private final BorderPane content = new BorderPane();
    private final ScrollPane editorScrollPane = new ScrollPane();

    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final ComboBox<String> lineSelector = new ComboBox<>();
    private final ComboBox<BlankModel> blankModelSelector = new ComboBox<>();
    private final Button loadLatestVersionBtn = new Button("↻  Carica ultima versione");
    private final TextArea notesArea = new TextArea();

    private final VBox layerNavigationBox = new VBox(8);
    private final StackPane selectedLayerContainer = new StackPane();
    private final Label selectedLayerTitle = new Label("Nessuno strato selezionato");
    private final Label selectedLayerTotal = new Label("Totale: 0%");
    private final Label selectedLayerMetrics = new Label("Configura le polveri per visualizzare le metriche.");
    private final ToggleGroup layerToggleGroup = new ToggleGroup();

    private final Button saveBtn = new Button("Salva composizione");
    private final Button backBtn = new Button("Indietro");
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final Label pageTitleLabel = new Label("Nuova composizione");

    private final List<LayerDraft> layers = new ArrayList<>();
    private final List<LayerEditorView> layerViews = new ArrayList<>();
    private final List<ToggleButton> layerNavigationButtons = new ArrayList<>();
    private List<Powder> availablePowders = new ArrayList<>();
    private int selectedLayerIndex = -1;
    private Function<LayerDraft, CompositionLayerViewState> layerStateProvider =
            layer -> CompositionLayerViewState.empty(layer.layerNumber());

    public CreateCompositionView() {
        buildLayout();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        getStyleClass().add("composition-editor");
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    private void buildLayout() {
        productSelector.setPromptText("Seleziona prodotto");
        lineSelector.setPromptText("Seleziona linea");
        blankModelSelector.setPromptText("Seleziona modello disco");
        for (ComboBox<?> selector : List.of(productSelector, lineSelector, blankModelSelector)) {
            selector.setMaxWidth(Double.MAX_VALUE);
            selector.getStyleClass().add("composition-selector");
        }

        loadLatestVersionBtn.setFocusTraversable(false);
        loadLatestVersionBtn.getStyleClass().add("load-version-action");
        setLoadLatestVersionVisible(false);

        GridPane selectorsGrid = new GridPane();
        selectorsGrid.setHgap(16);
        selectorsGrid.setVgap(12);
        selectorsGrid.add(createField("Prodotto", productSelector), 0, 0);
        selectorsGrid.add(createField("Linea", lineSelector), 1, 0);
        selectorsGrid.add(createField("Modello disco", blankModelSelector), 2, 0);
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
            column.setPercentWidth(100.0 / 3.0);
            column.setHgrow(Priority.ALWAYS);
            selectorsGrid.getColumnConstraints().add(column);
        }

        VBox identityContent = new VBox(12, selectorsGrid, loadLatestVersionBtn);

        layerNavigationBox.getStyleClass().add("layer-navigation-list");
        ScrollPane layerNavigationScroll = new ScrollPane(layerNavigationBox);
        layerNavigationScroll.getStyleClass().add("layer-navigation-scroll");
        layerNavigationScroll.setFitToWidth(true);
        layerNavigationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        layerNavigationScroll.setMinWidth(178);
        layerNavigationScroll.setPrefWidth(190);

        selectedLayerContainer.getStyleClass().add("selected-layer-container");
        selectedLayerContainer.setMinHeight(230);
        HBox.setHgrow(selectedLayerContainer, Priority.ALWAYS);

        selectedLayerTitle.getStyleClass().add("summary-title");
        selectedLayerTotal.getStyleClass().addAll("layer-completion", "layer-completion-invalid");
        selectedLayerMetrics.getStyleClass().add("layer-metrics-summary");
        selectedLayerMetrics.setWrapText(true);
        Label summaryHeading = new Label("Riepilogo strato");
        summaryHeading.getStyleClass().add("layer-summary-heading");
        VBox summary = new VBox(14,
                summaryHeading,
                selectedLayerTitle,
                selectedLayerTotal,
                selectedLayerMetrics
        );
        summary.getStyleClass().add("layer-summary-card");
        summary.setMinWidth(205);
        summary.setPrefWidth(220);
        summary.setMaxWidth(235);

        HBox layerWorkspace = new HBox(14, layerNavigationScroll, selectedLayerContainer, summary);
        layerWorkspace.setAlignment(Pos.TOP_LEFT);

        notesArea.setPromptText("Note valide per l'intera composizione...");
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().add("composition-notes");

        pageTitleLabel.getStyleClass().add("composition-page-title");
        Label pageSubtitle = new Label("Seleziona il prodotto e configura le polveri di ogni strato.");
        pageSubtitle.getStyleClass().add("composition-page-subtitle");

        VBox editorContent = new VBox(24,
                new VBox(6, pageTitleLabel, pageSubtitle),
                new NumberedSectionCard("composition", "1", "Informazioni composizione",
                        "Scegli prodotto, linea e modello disco da utilizzare.", identityContent),
                new NumberedSectionCard("composition", "2", "Configurazione strati",
                        "Seleziona uno strato e definisci le polveri che lo compongono.", layerWorkspace),
                new NumberedSectionCard("composition", "3", "Note generali",
                        "Aggiungi indicazioni valide per l'intera composizione.", notesArea)
        );
        editorContent.setMaxWidth(CONTENT_MAX_WIDTH);

        StackPane centered = new StackPane(editorContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(30, 28, 36, 28));

        editorScrollPane.setContent(centered);
        editorScrollPane.getStyleClass().add("composition-scroll");
        editorScrollPane.setFitToWidth(true);
        editorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.getStyleClass().add("composition-back-action");
        saveBtn.getStyleClass().add("composition-save-action");
        statusLabel.getStyleClass().add("composition-status");

        HBox status = new HBox(12, progressIndicator, statusLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        EditorActionBar actionBar = new EditorActionBar(
                "composition-action-bar", backBtn, status, saveBtn
        );

        content.setCenter(editorScrollPane);
        content.setBottom(actionBar);
    }

    private VBox createField(String text, Node control) {
        Label label = new Label(text);
        label.getStyleClass().add("composition-field-label");
        return new VBox(6, label, control);
    }

    public void setAvailablePowders(List<Powder> powders) {
        this.availablePowders = powders;
    }

    public void setLayerStateProvider(Function<LayerDraft, CompositionLayerViewState> layerStateProvider) {
        this.layerStateProvider = layerStateProvider;
    }

    public void showInitialLoading() { setLoadingState("Caricamento dati composizione..."); }
    public void showLinesLoading() { setLoadingState("Caricamento linee..."); }
    public void showLatestVersionLoading() { setLoadingState("Caricamento ultima versione..."); }
    public void showSaving() { setLoadingState("Salvataggio composizione..."); }

    public void showLoadSuccess() {
        editorScrollPane.setDisable(false);
        saveBtn.setDisable(false);
        backBtn.setDisable(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setText("");
        statusLabel.getStyleClass().remove("composition-status-error");
    }

    public void showLoadError(String message) {
        editorScrollPane.setDisable(false);
        saveBtn.setDisable(false);
        backBtn.setDisable(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        statusLabel.setText(message);
        if (!statusLabel.getStyleClass().contains("composition-status-error")) {
            statusLabel.getStyleClass().add("composition-status-error");
        }
    }

    private void setLoadingState(String message) {
        editorScrollPane.setDisable(true);
        saveBtn.setDisable(true);
        backBtn.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("composition-status-error");
    }

    private void createLayerView(LayerDraft layerDraft) {
        int index = layerViews.size();
        LayerEditorView layerView = new LayerEditorView(layerDraft, availablePowders);
        layerView.setLayerRemovalEnabled(false);
        layerView.setOnRemove(() -> {});
        layerView.setOnChanged(() -> refreshLayerState(index));
        layerViews.add(layerView);

        ToggleButton navigationButton = new ToggleButton();
        navigationButton.setToggleGroup(layerToggleGroup);
        navigationButton.getStyleClass().add("layer-navigation-button");
        navigationButton.setMaxWidth(Double.MAX_VALUE);
        navigationButton.setOnAction(e -> selectLayer(index));
        layerNavigationButtons.add(navigationButton);
        layerNavigationBox.getChildren().add(navigationButton);
        refreshLayerState(index);
    }

    private void selectLayer(int index) {
        if (index < 0 || index >= layerViews.size()) {
            return;
        }
        selectedLayerIndex = index;
        layerNavigationButtons.get(index).setSelected(true);
        selectedLayerContainer.getChildren().setAll(layerViews.get(index));
        refreshSelectedLayerSummary();
    }

    private void refreshLayerState(int index) {
        if (index < 0 || index >= layerViews.size()) {
            return;
        }
        CompositionLayerViewState state = layerStateProvider.apply(layers.get(index));
        layerNavigationButtons.get(index).setText(String.format(
                java.util.Locale.ROOT,
                "Layer %d    %s%n%d polveri · %.2f%%",
                index + 1,
                state.statusSymbol(),
                state.ingredientCount(),
                state.totalPercentage()
        ));
        if (selectedLayerIndex == index) {
            refreshSelectedLayerSummary();
        }
    }

    private void refreshSelectedLayerSummary() {
        if (selectedLayerIndex < 0 || selectedLayerIndex >= layerViews.size()) {
            selectedLayerTitle.setText("Nessuno strato selezionato");
            selectedLayerTotal.setText("Totale: 0%");
            selectedLayerMetrics.setText("Configura le polveri per visualizzare le metriche.");
            return;
        }

        CompositionLayerViewState state = layerStateProvider.apply(layers.get(selectedLayerIndex));
        selectedLayerTitle.setText("Layer " + (selectedLayerIndex + 1));
        selectedLayerTotal.setText(String.format(
                java.util.Locale.ROOT,
                "Totale: %.2f%%",
                state.totalPercentage()
        ));
        selectedLayerTotal.getStyleClass().removeAll("layer-completion-valid", "layer-completion-invalid");
        selectedLayerTotal.getStyleClass().add(
                state.valid() ? "layer-completion-valid" : "layer-completion-invalid"
        );
        selectedLayerMetrics.setText(state.metricsText());
    }

    public void setLayerCount(int layerCount) {
        clearLayers();
        for (int i = 1; i <= layerCount; i++) {
            LayerDraft layerDraft = new LayerDraft(i);
            layers.add(layerDraft);
            createLayerView(layerDraft);
        }
        if (!layerViews.isEmpty()) {
            selectLayer(0);
        }
    }

    private void clearLayers() {
        layers.clear();
        layerViews.clear();
        layerNavigationButtons.clear();
        layerNavigationBox.getChildren().clear();
        selectedLayerContainer.getChildren().clear();
        selectedLayerIndex = -1;
        refreshSelectedLayerSummary();
    }

    public void renumberLayers() {
        for (int i = 0; i < layers.size(); i++) {
            int number = i + 1;
            layers.get(i).setLayerNumber(number);
            layerViews.get(i).setLayerNumber(number);
            refreshLayerState(i);
        }
    }

    public void configureEditMode(boolean editMode) {
        if (editMode) {
            header.setTitle("Laboratorio - Modifica Composizione");
            pageTitleLabel.setText("Modifica composizione");
            saveBtn.setText("Salva modifiche");
            backBtn.setVisible(true);
            backBtn.setManaged(true);
        } else {
            header.setTitle("Laboratorio - Nuova Composizione");
            pageTitleLabel.setText("Nuova composizione");
            saveBtn.setText("Salva composizione");
            backBtn.setVisible(false);
            backBtn.setManaged(false);
        }
    }

    public void setLineSelectorLocked(boolean locked) { lineSelector.setDisable(locked); }
    public ComboBox<Product> getProductSelector() { return productSelector; }
    public ComboBox<BlankModel> getBlankModelSelector() { return blankModelSelector; }
    public ComboBox<String> getLineSelector() { return lineSelector; }

    public String getNotes() {
        String text = notesArea.getText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    public void setNotes(String notes) { notesArea.setText(notes == null ? "" : notes); }
    public List<LayerDraft> getLayers() { return List.copyOf(layers); }

    public void replaceLayers(List<LayerDraft> newLayers) {
        clearLayers();
        for (LayerDraft layer : newLayers) {
            layers.add(layer);
            createLayerView(layer);
        }
        renumberLayers();
        if (!layerViews.isEmpty()) {
            selectLayer(0);
        }
    }

    public Button getSaveButton() { return saveBtn; }
    public Button getBackButton() { return backBtn; }
    public Button getLoadLatestVersionButton() { return loadLatestVersionBtn; }

    public void setLoadLatestVersionVisible(boolean visible) {
        loadLatestVersionBtn.setVisible(visible);
        loadLatestVersionBtn.setManaged(visible);
    }

    public AppHeader getHeader() { return header; }
}
