package com.orodent.tonv2.features.laboratory.itemsetup.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.itemsetup.service.ItemSetupService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class ItemSetupView extends VBox {
    private static final double CONTENT_MAX_WIDTH = 1080;

    private final AppHeader header = new AppHeader("Laboratorio - Nuovo Articolo");
    private final BorderPane content = new BorderPane();
    private final ScrollPane editorScrollPane = new ScrollPane();

    private final ComboBox<Product> productSelector = new ComboBox<>();
    private final Button activateLatestCompositionButton = new Button("↻  Usa l'ultima composizione");
    private final Label compositionStateLabel = new Label("Seleziona un prodotto per verificare la composizione.");
    private final Label compositionDetailsLabel = new Label();

    private final TextField itemCodeField = new TextField();
    private final TextField heightField = new TextField();
    private final Button createItemButton = new Button("Crea articolo");

    private final Label summaryProductValue = new Label("Non selezionato");
    private final Label summaryCompositionValue = new Label("Da verificare");
    private final Label summaryBlankModelValue = new Label("Da verificare");
    private final Label summaryCodeValue = new Label("—");
    private final Label summaryHeightValue = new Label("—");
    private final Label readinessLabel = new Label("Seleziona un prodotto");

    private final Label feedbackLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();
    private ItemSetupService.ProductSetupStatus productSetupStatus = ItemSetupService.ProductSetupStatus.empty();
    private boolean loading;

    public ItemSetupView() {
        buildLayout();
        bindSummary();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        getStyleClass().add("item-setup-view");
        VBox.setVgrow(content, Priority.ALWAYS);
        getChildren().addAll(header, content);
    }

    private void buildLayout() {
        productSelector.setPromptText("Seleziona prodotto");
        productSelector.setMaxWidth(Double.MAX_VALUE);
        productSelector.getStyleClass().add("item-product-selector");

        compositionStateLabel.getStyleClass().add("composition-state-title");
        compositionDetailsLabel.getStyleClass().add("composition-state-details");
        compositionDetailsLabel.setWrapText(true);
        activateLatestCompositionButton.getStyleClass().add("item-secondary-action");

        itemCodeField.setPromptText("Es. ZRA2-H18");
        heightField.setPromptText("Es. 12.5");
        itemCodeField.getStyleClass().add("item-text-field");
        heightField.getStyleClass().add("item-text-field");

        GridPane articleGrid = new GridPane();
        articleGrid.setHgap(18);
        articleGrid.add(createField("Codice articolo", itemCodeField), 0, 0);
        articleGrid.add(createHeightField(), 1, 0);
        javafx.scene.layout.ColumnConstraints wideColumn = new javafx.scene.layout.ColumnConstraints();
        wideColumn.setPercentWidth(65);
        wideColumn.setHgrow(Priority.ALWAYS);
        javafx.scene.layout.ColumnConstraints heightColumn = new javafx.scene.layout.ColumnConstraints();
        heightColumn.setPercentWidth(35);
        heightColumn.setHgrow(Priority.ALWAYS);
        articleGrid.getColumnConstraints().addAll(wideColumn, heightColumn);

        VBox leftColumn = new VBox(18,
                createSectionCard("1", "Seleziona il prodotto",
                        "Il prodotto determina la composizione e il modello disco utilizzati.",
                        createField("Prodotto", productSelector)),
                createSectionCard("2", "Composizione attiva",
                        "Verifica la configurazione prima di creare l'articolo.",
                        new VBox(7, compositionStateLabel, compositionDetailsLabel),
                        activateLatestCompositionButton),
                createSectionCard("3", "Dati articolo",
                        "Inserisci il codice identificativo e l'altezza dell'articolo.", articleGrid)
        );
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        VBox summary = createSummaryCard();
        summary.setMinWidth(245);
        summary.setPrefWidth(265);
        summary.setMaxWidth(280);

        HBox columns = new HBox(20, leftColumn, summary);
        columns.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Nuovo articolo");
        title.getStyleClass().add("item-page-title");
        Label subtitle = new Label("Crea un articolo utilizzando la composizione attiva del prodotto.");
        subtitle.getStyleClass().add("item-page-subtitle");

        VBox editorContent = new VBox(24, new VBox(6, title, subtitle), columns);
        editorContent.setMaxWidth(CONTENT_MAX_WIDTH);

        StackPane centered = new StackPane(editorContent);
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setPadding(new Insets(30, 28, 36, 28));
        editorScrollPane.setContent(centered);
        editorScrollPane.getStyleClass().add("item-setup-scroll");
        editorScrollPane.setFitToWidth(true);
        editorScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        feedbackLabel.getStyleClass().add("item-feedback");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        createItemButton.getStyleClass().add("item-primary-action");
        HBox actionBar = new HBox(12, progressIndicator, feedbackLabel, spacer, createItemButton);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.getStyleClass().add("item-action-bar");

        content.setCenter(editorScrollPane);
        content.setBottom(actionBar);
        showProductSetup(ItemSetupService.ProductSetupStatus.empty());
    }

    private void bindSummary() {
        productSelector.valueProperty().addListener((obs, oldValue, newValue) -> {
            productSetupStatus = ItemSetupService.ProductSetupStatus.empty();
            refreshSummary();
        });
        itemCodeField.textProperty().addListener((obs, oldValue, newValue) -> refreshSummary());
        heightField.textProperty().addListener((obs, oldValue, newValue) -> refreshSummary());
    }

    private VBox createSummaryCard() {
        Label heading = new Label("Riepilogo articolo");
        heading.getStyleClass().add("item-summary-title");
        readinessLabel.getStyleClass().addAll("item-readiness", "item-readiness-warning");

        VBox card = new VBox(14,
                heading,
                createSummaryRow("Prodotto", summaryProductValue),
                createSummaryRow("Composizione attiva", summaryCompositionValue),
                createSummaryRow("Modello disco", summaryBlankModelValue),
                createSummaryRow("Codice articolo", summaryCodeValue),
                createSummaryRow("Altezza", summaryHeightValue),
                new Label("Stato"),
                readinessLabel
        );
        card.getStyleClass().add("item-summary-card");
        return card;
    }

    private VBox createSummaryRow(String labelText, Label value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("item-summary-label");
        value.getStyleClass().add("item-summary-value");
        value.setWrapText(true);
        return new VBox(3, label, value);
    }

    private VBox createField(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("item-field-label");
        return new VBox(6, label, control);
    }

    private VBox createHeightField() {
        Label unit = new Label("mm");
        unit.getStyleClass().add("item-field-unit");
        StackPane field = new StackPane(heightField, unit);
        StackPane.setAlignment(unit, Pos.CENTER_RIGHT);
        StackPane.setMargin(unit, new Insets(0, 10, 0, 0));
        heightField.setPadding(new Insets(8, 48, 8, 10));
        return createField("Altezza", field);
    }

    private VBox createSectionCard(String number, String titleText, String descriptionText, Node... nodes) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("item-section-number");
        numberLabel.setMinSize(30, 30);
        numberLabel.setAlignment(Pos.CENTER);
        Label title = new Label(titleText);
        title.getStyleClass().add("item-section-title");
        Label description = new Label(descriptionText);
        description.getStyleClass().add("item-section-description");
        description.setWrapText(true);
        HBox heading = new HBox(12, numberLabel, new VBox(2, title, description));
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(16, heading);
        card.getChildren().addAll(nodes);
        card.getStyleClass().add("item-section-card");
        return card;
    }

    public AppHeader getHeader() { return header; }
    public ComboBox<Product> getProductSelector() { return productSelector; }
    public Button getActivateLatestCompositionButton() { return activateLatestCompositionButton; }
    public TextField getItemCodeField() { return itemCodeField; }
    public TextField getHeightField() { return heightField; }
    public Button getCreateItemButton() { return createItemButton; }

    public void showProductSetupLoading() {
        compositionStateLabel.setText("Verifica configurazione...");
        compositionDetailsLabel.setText("");
        productSetupStatus = ItemSetupService.ProductSetupStatus.empty();
        refreshSummary();
    }

    public void showProductSetup(ItemSetupService.ProductSetupStatus status) {
        productSetupStatus = status == null ? ItemSetupService.ProductSetupStatus.empty() : status;
        if (!productSetupStatus.hasActiveComposition()) {
            compositionStateLabel.setText("Nessuna composizione attiva");
            compositionDetailsLabel.setText("Puoi rendere attiva l'ultima composizione disponibile.");
        } else if (!productSetupStatus.hasBlankModel()) {
            compositionStateLabel.setText("Composizione #" + productSetupStatus.activeCompositionId());
            compositionDetailsLabel.setText("La composizione attiva non ha un modello disco associato.");
        } else {
            compositionStateLabel.setText("✓ Composizione #" + productSetupStatus.activeCompositionId());
            compositionDetailsLabel.setText("Modello disco: " + productSetupStatus.blankModelCode());
        }
        refreshSummary();
    }

    private void refreshSummary() {
        Product product = productSelector.getValue();
        String code = itemCodeField.getText() == null ? "" : itemCodeField.getText().trim();
        String height = heightField.getText() == null ? "" : heightField.getText().trim();

        summaryProductValue.setText(product == null ? "Non selezionato" : product.code());
        summaryCompositionValue.setText(productSetupStatus.hasActiveComposition()
                ? "#" + productSetupStatus.activeCompositionId() + "  ✓" : "Non disponibile");
        summaryBlankModelValue.setText(productSetupStatus.hasBlankModel()
                ? productSetupStatus.blankModelCode() + "  ✓" : "Non disponibile");
        summaryCodeValue.setText(code.isEmpty() ? "—" : code);
        summaryHeightValue.setText(height.isEmpty() ? "—" : height + " mm");

        String readiness;
        boolean ready;
        if (product == null) {
            readiness = "Seleziona un prodotto";
            ready = false;
        } else if (!productSetupStatus.hasActiveComposition()) {
            readiness = "Attiva una composizione";
            ready = false;
        } else if (!productSetupStatus.hasBlankModel()) {
            readiness = "Modello disco mancante";
            ready = false;
        } else if (code.isEmpty()) {
            readiness = "Inserisci il codice";
            ready = false;
        } else if (!isPositiveNumber(height)) {
            readiness = "Inserisci un'altezza valida";
            ready = false;
        } else {
            readiness = "Pronto per la creazione  ✓";
            ready = true;
        }

        readinessLabel.setText(readiness);
        readinessLabel.getStyleClass().removeAll("item-readiness-ready", "item-readiness-warning");
        readinessLabel.getStyleClass().add(ready ? "item-readiness-ready" : "item-readiness-warning");
        activateLatestCompositionButton.setDisable(loading || product == null);
        createItemButton.setDisable(loading || !ready);
    }

    private boolean isPositiveNumber(String value) {
        try {
            return Double.parseDouble(value.replace(',', '.')) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text == null ? "" : text);
        feedbackLabel.getStyleClass().removeAll("item-feedback-success", "item-feedback-error");
        if (text != null && !text.isBlank()) {
            feedbackLabel.getStyleClass().add(error ? "item-feedback-error" : "item-feedback-success");
        }
    }

    public void showProductsLoading() { setLoadingState("Caricamento prodotti..."); }
    public void showActivationLoading() { setLoadingState("Attivazione composizione..."); }
    public void showCreationLoading() { setLoadingState("Creazione articolo..."); }

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
        loading = true;
        editorScrollPane.setDisable(true);
        createItemButton.setDisable(true);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        setFeedback(message, false);
    }

    private void finishLoading() {
        loading = false;
        editorScrollPane.setDisable(false);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        refreshSummary();
    }
}
