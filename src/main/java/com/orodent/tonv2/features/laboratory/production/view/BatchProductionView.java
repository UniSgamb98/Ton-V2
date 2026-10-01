package com.orodent.tonv2.features.laboratory.production.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.components.EditorActionBar;
import com.orodent.tonv2.core.components.NumberedSectionCard;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.production.presentation.BatchProductionViewState;
import com.orodent.tonv2.features.laboratory.production.view.partial.BatchItemRowView;
import com.orodent.tonv2.features.laboratory.production.view.partial.BatchProductionSummaryView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BatchProductionView extends VBox {

    private static final double CONTENT_MAX_WIDTH = 1120;

    private final AppHeader header = new AppHeader("Laboratorio - Produzione");
    private final BorderPane content = new BorderPane();
    private final ScrollPane editorScrollPane = new ScrollPane();
    private final ComboBox<Line> lineSelector = new ComboBox<>();
    private final FlowPane productButtonsBox = new FlowPane(9, 9);
    private final VBox rowsBox = new VBox(8);
    private final Label itemsEmptyState = new Label("Seleziona una linea e un prodotto per configurare le quantità.");
    private final Label itemsTotalsLabel = new Label("Nessun articolo configurato");
    private final ComboBox<String> templateSelector = new ComboBox<>();
    private final TextArea notesArea = new TextArea();
    private final Button clearQuantitiesButton = new Button("Azzera quantità");
    private final Button produceButton = new Button("Avvia produzione batch");
    private final Label feedbackLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private final BatchProductionSummaryView summaryView = new BatchProductionSummaryView();

    private final List<BatchItemRowView> rows = new ArrayList<>();
    private Product selectedProduct;
    private Consumer<Product> productSelectionHandler = product -> {};
    private BiConsumer<Item, Integer> quantityChangeHandler = (item, quantity) -> {};

    public BatchProductionView() {
        buildLayout();
        getStyleClass().add("batch-production-view");
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    private void buildLayout() {
        lineSelector.setPromptText("Seleziona linea di produzione");
        lineSelector.setMaxWidth(Double.MAX_VALUE);
        lineSelector.getStyleClass().add("batch-selector");

        productButtonsBox.getStyleClass().add("batch-product-list");
        productButtonsBox.setPrefWrapLength(620);

        itemsEmptyState.getStyleClass().add("batch-empty-state");
        itemsEmptyState.setWrapText(true);
        itemsTotalsLabel.getStyleClass().add("batch-items-total");
        clearQuantitiesButton.getStyleClass().add("batch-secondary-action");
        HBox itemFooter = new HBox(12, itemsTotalsLabel, createSpacer(), clearQuantitiesButton);
        itemFooter.setAlignment(Pos.CENTER_LEFT);

        templateSelector.setPromptText("Nessun documento");
        templateSelector.setMaxWidth(Double.MAX_VALUE);
        templateSelector.getStyleClass().add("batch-selector");
        notesArea.setPromptText("Aggiungi eventuali indicazioni per la produzione…");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.getStyleClass().add("batch-notes");

        VBox leftColumn = new VBox(18,
                new NumberedSectionCard("batch", "1", "Linea di produzione",
                        "Scegli la linea sulla quale verrà registrato il nuovo ordine.",
                        createField("Linea", lineSelector)),
                new NumberedSectionCard("batch", "2", "Prodotto",
                        "Seleziona uno dei prodotti disponibili per la linea scelta.",
                        productButtonsBox),
                new NumberedSectionCard("batch", "3", "Quantità da produrre",
                        "Configura solo gli articoli da includere nel batch.",
                        itemsEmptyState, rowsBox, itemFooter),
                new NumberedSectionCard("batch", "4", "Documento e note",
                        "Allega facoltativamente un documento e le indicazioni per la produzione.",
                        createField("Modello documento", templateSelector),
                        createField("Note interne", notesArea))
        );
        leftColumn.setMinWidth(470);
        leftColumn.setPrefWidth(760);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        summaryView.setMinWidth(250);
        summaryView.setPrefWidth(280);
        summaryView.setMaxWidth(300);

        FlowPane columns = new FlowPane(20, 20, leftColumn, summaryView);
        columns.setAlignment(Pos.TOP_CENTER);
        columns.setPrefWrapLength(CONTENT_MAX_WIDTH);

        Label title = new Label("Produzione batch");
        title.getStyleClass().add("batch-page-title");
        Label subtitle = new Label("Configura gli articoli e le quantità da inviare alla linea di produzione.");
        subtitle.getStyleClass().add("batch-page-subtitle");

        VBox editorContent = new VBox(24, new VBox(6, title, subtitle), columns);
        editorContent.setMaxWidth(CONTENT_MAX_WIDTH);
        StackPane centered = new StackPane(editorContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(30, 28, 36, 28));

        editorScrollPane.setContent(centered);
        editorScrollPane.setFitToWidth(true);
        editorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editorScrollPane.getStyleClass().add("batch-production-scroll");

        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        feedbackLabel.getStyleClass().add("batch-feedback");
        produceButton.getStyleClass().add("batch-primary-action");
        HBox status = new HBox(12, progressIndicator, feedbackLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        EditorActionBar actionBar = new EditorActionBar(
                "batch-action-bar", null, status, produceButton
        );

        content.setCenter(editorScrollPane);
        content.setBottom(actionBar);
        clearProducts();
    }

    private VBox createField(String labelText, javafx.scene.Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("batch-field-label");
        return new VBox(6, label, control);
    }

    private javafx.scene.layout.Region createSpacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public void setLines(List<Line> lines) {
        lineSelector.getItems().setAll(lines);
    }

    public void setItemRows(List<Item> items) {
        rows.clear();
        rowsBox.getChildren().clear();
        for (Item item : items) {
            BatchItemRowView row = new BatchItemRowView(item);
            row.setQuantityChangeHandler(quantity -> quantityChangeHandler.accept(item, quantity));
            rows.add(row);
            rowsBox.getChildren().add(row);
        }
        itemsEmptyState.setManaged(items.isEmpty());
        itemsEmptyState.setVisible(items.isEmpty());
    }

    public void clearQuantities() {
        rows.forEach(row -> row.setQuantity(0));
    }

    public AppHeader getHeader() { return header; }
    public ComboBox<Line> getLineSelector() { return lineSelector; }
    public TextArea getNotesArea() { return notesArea; }
    public Button getProduceButton() { return produceButton; }
    public Button getClearQuantitiesButton() { return clearQuantitiesButton; }
    public ComboBox<String> getTemplateSelector() { return templateSelector; }

    public void setProductSelectionHandler(Consumer<Product> handler) {
        productSelectionHandler = handler == null ? product -> {} : handler;
    }

    public void setQuantityChangeHandler(BiConsumer<Item, Integer> handler) {
        quantityChangeHandler = handler == null ? (item, quantity) -> {} : handler;
    }

    public void setSelectableProducts(List<Product> products, Product preselectedProduct) {
        selectedProduct = null;
        productButtonsBox.getChildren().clear();
        if (products.isEmpty()) {
            Label empty = new Label("Nessun prodotto disponibile per questa linea.");
            empty.getStyleClass().add("batch-empty-state");
            productButtonsBox.getChildren().add(empty);
        } else {
            for (Product product : products) {
                Button button = new Button(product.toString());
                button.getStyleClass().add("batch-product-button");
                button.setWrapText(true);
                button.setOnAction(event -> {
                    if (selectedProduct == null || selectedProduct.id() != product.id()) {
                        highlightSelectedProduct(product);
                        productSelectionHandler.accept(product);
                    }
                });
                button.setUserData(product);
                productButtonsBox.getChildren().add(button);
            }
        }
        if (preselectedProduct != null) {
            highlightSelectedProduct(preselectedProduct);
        }
    }

    public void clearProducts() {
        selectedProduct = null;
        productButtonsBox.getChildren().clear();
        Label placeholder = new Label("Seleziona prima una linea di produzione.");
        placeholder.getStyleClass().add("batch-empty-state");
        productButtonsBox.getChildren().add(placeholder);
    }

    private void highlightSelectedProduct(Product product) {
        selectedProduct = product;
        productButtonsBox.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(button -> {
                    Product candidate = (Product) button.getUserData();
                    button.getStyleClass().remove("batch-product-button-selected");
                    if (candidate.id() == product.id()) {
                        button.getStyleClass().add("batch-product-button-selected");
                    }
                });
    }

    public void setTemplateNames(List<String> names, String preselectedName) {
        templateSelector.getItems().setAll(names);
        templateSelector.setValue(preselectedName != null && names.contains(preselectedName)
                ? preselectedName
                : names.isEmpty() ? null : names.getFirst());
    }

    public void render(BatchProductionViewState state) {
        summaryView.render(state);
        itemsTotalsLabel.setText(state.configuredItems() == 0
                ? "Nessun articolo configurato"
                : state.configuredItems() + " articoli configurati · " + state.totalQuantity() + " pezzi");
        clearQuantitiesButton.setDisable(state.configuredItems() == 0);
        produceButton.setDisable(state.loading() || !state.ready());
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text == null ? "" : text);
        feedbackLabel.getStyleClass().removeAll("batch-feedback-success", "batch-feedback-error");
        if (text != null && !text.isBlank()) {
            feedbackLabel.getStyleClass().add(error ? "batch-feedback-error" : "batch-feedback-success");
        }
    }

    public void showInitialLoading() { setLoadingState("Caricamento dati produzione..."); }
    public void showProductsLoading() { setLoadingState("Caricamento prodotti..."); }
    public void showItemsLoading() { setLoadingState("Caricamento articoli..."); }
    public void showProductionSaving() { setLoadingState("Salvataggio produzione..."); }

    public void showLoadSuccess() {
        setControlsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        setFeedback("", false);
    }

    public void showLoadError(String message) {
        setControlsDisabled(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        setFeedback(message, true);
    }

    private void setLoadingState(String message) {
        setControlsDisabled(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        setFeedback(message, false);
    }

    private void setControlsDisabled(boolean disabled) {
        editorScrollPane.setDisable(disabled);
        produceButton.setDisable(disabled);
    }
}
