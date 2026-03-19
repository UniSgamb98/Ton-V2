package com.orodent.tonv2.features.laboratory.production.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.documents.template.TemplateStorageService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatchProductionView extends VBox {

    private static final String PRODUCT_BUTTON_BASE_STYLE = "-fx-background-color: #e5e7eb; -fx-text-fill: #111827;";
    private static final String PRODUCT_BUTTON_SELECTED_STYLE = "-fx-background-color: #2563eb; -fx-text-fill: white;";

    private final AppHeader header = new AppHeader("Laboratorio - Produzione");
    private final ComboBox<Line> lineSelector = new ComboBox<>();
    private final Label productSelectorLabel = new Label("Prodotto");
    private final FlowPane productButtonsBox = new FlowPane();
    private final VBox rowsBox = new VBox(8);
    private final TextArea notesArea = new TextArea();
    private final ComboBox<TemplateStorageService.SavedTemplateRef> templateSelector = new ComboBox<>();
    private final Button produceButton = new Button("Produzione batch");
    private final Label feedbackLabel = new Label();
    private final Label templateParamsHelpLabel = new Label(
            """
                    Parametri template disponibili (demo batch):
                    - {{line.name}}
                    - {{notes}}
                    - {{#each items}} ... {{code}} ... {{quantity}} ... {{/each}}"""
    );

    private final List<BatchRow> rows = new ArrayList<>();
    private Product selectedProduct;
    private Consumer<Product> productSelectionHandler = product -> {};

    public BatchProductionView() {
        setSpacing(16);
        setPadding(new Insets(20));

        lineSelector.setPromptText("Seleziona linea di produzione");
        lineSelector.setMaxWidth(Double.MAX_VALUE);

        productSelectorLabel.setStyle("-fx-font-weight: bold;");
        productButtonsBox.setHgap(8);
        productButtonsBox.setVgap(8);

        notesArea.setPromptText("Note ordine (opzionale)");
        notesArea.setPrefRowCount(3);

        templateSelector.setPromptText("Seleziona template documento");
        templateSelector.setMaxWidth(Double.MAX_VALUE);

        feedbackLabel.setStyle("-fx-text-fill: #374151;");
        templateParamsHelpLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        getChildren().addAll(
                header,
                new Label("Linea"),
                lineSelector,
                productSelectorLabel,
                productButtonsBox,
                new Separator(),
                rowsBox,
                new Label("Note"),
                notesArea,
                new Label("Template documento"),
                templateSelector,
                templateParamsHelpLabel,
                produceButton,
                feedbackLabel
        );
    }

    public void setLines(List<Line> lines) {
        lineSelector.getItems().setAll(lines);
    }

    public void setTemplates(List<TemplateStorageService.SavedTemplateRef> templates) {
        templateSelector.getItems().setAll(templates);
    }

    public void setProductSelectionHandler(Consumer<Product> productSelectionHandler) {
        this.productSelectionHandler = productSelectionHandler != null ? productSelectionHandler : product -> {};
    }

    public void setSelectableProducts(List<Product> products, Product preselectedProduct) {
        selectedProduct = null;
        productButtonsBox.getChildren().clear();

        for (Product product : products) {
            Button productButton = new Button(product.code());
            productButton.setStyle(PRODUCT_BUTTON_BASE_STYLE);
            productButton.setOnAction(e -> {
                if (selectedProduct != null && selectedProduct.id() == product.id()) {
                    return;
                }
                highlightSelectedProduct(product);
                productSelectionHandler.accept(product);
            });
            productButtonsBox.getChildren().add(productButton);
        }

        if (preselectedProduct != null) {
            highlightSelectedProduct(preselectedProduct);
        }
    }

    public void clearProducts() {
        selectedProduct = null;
        productButtonsBox.getChildren().clear();
    }

    private void highlightSelectedProduct(Product product) {
        selectedProduct = product;
        for (int i = 0; i < productButtonsBox.getChildren().size(); i++) {
            Button button = (Button) productButtonsBox.getChildren().get(i);
            boolean isSelected = button.getText().equals(product.code());
            button.setStyle(isSelected ? PRODUCT_BUTTON_SELECTED_STYLE : PRODUCT_BUTTON_BASE_STYLE);
        }
    }

    public void setItemRows(List<Item> items) {
        rows.clear();
        rowsBox.getChildren().clear();

        for (Item item : items) {
            BatchRow row = new BatchRow(item);
            rows.add(row);
            rowsBox.getChildren().add(row.container);
        }
    }

    public List<BatchRow> getRows() {
        return rows;
    }

    public AppHeader getHeader() {
        return header;
    }

    public ComboBox<Line> getLineSelector() {
        return lineSelector;
    }

    public TextArea getNotesArea() {
        return notesArea;
    }

    public ComboBox<TemplateStorageService.SavedTemplateRef> getTemplateSelector() {
        return templateSelector;
    }

    public Button getProduceButton() {
        return produceButton;
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    public static class BatchRow {
        private final HBox container;
        private final Item item;
        private final TextField quantityField;

        private BatchRow(Item item) {
            this.item = item;

            Label itemCodeLabel = new Label(item.code());
            itemCodeLabel.setMinWidth(180);

            quantityField = new TextField();
            quantityField.setPromptText("Quantità");
            quantityField.setPrefWidth(120);

            container = new HBox(10, itemCodeLabel, quantityField);
            HBox.setHgrow(itemCodeLabel, Priority.ALWAYS);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        public Item getItem() {
            return item;
        }

        public TextField getQuantityField() {
            return quantityField;
        }
    }
}
