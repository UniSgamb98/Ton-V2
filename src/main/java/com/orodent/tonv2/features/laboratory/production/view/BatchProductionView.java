package com.orodent.tonv2.features.laboratory.production.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.features.documents.template.service.TemplateStorageService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class BatchProductionView extends VBox {

    private final AppHeader header = new AppHeader("Produzione batch");
    private final ComboBox<Line> lineSelector = new ComboBox<>();
    private final VBox rowsBox = new VBox(8);
    private final TextArea notesArea = new TextArea();
    private final ComboBox<TemplateStorageService.SavedTemplateRef> templateSelector = new ComboBox<>();
    private final Button addRowButton = new Button("Aggiungi riga");
    private final Button produceButton = new Button("Produzione batch");
    private final Label feedbackLabel = new Label();

    private final List<BatchRow> rows = new ArrayList<>();

    public BatchProductionView() {
        setSpacing(16);
        setPadding(new Insets(20));

        lineSelector.setPromptText("Seleziona linea di produzione");
        lineSelector.setMaxWidth(Double.MAX_VALUE);

        notesArea.setPromptText("Note ordine (opzionale)");
        notesArea.setPrefRowCount(3);

        templateSelector.setPromptText("Seleziona template documento");
        templateSelector.setMaxWidth(Double.MAX_VALUE);

        feedbackLabel.setStyle("-fx-text-fill: #374151;");

        HBox actions = new HBox(10, addRowButton, produceButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
                header,
                new Label("Linea"),
                lineSelector,
                new Separator(),
                rowsBox,
                new Label("Note"),
                notesArea,
                new Label("Template documento"),
                templateSelector,
                actions,
                feedbackLabel
        );
    }

    public void setLines(List<Line> lines) {
        lineSelector.getItems().setAll(lines);
    }

    public void setTemplates(List<TemplateStorageService.SavedTemplateRef> templates) {
        templateSelector.getItems().setAll(templates);
    }

    public BatchRow addRow(List<Item> items, Item preselectedItem) {
        BatchRow row = new BatchRow(items, preselectedItem);
        rows.add(row);
        rowsBox.getChildren().add(row.container);

        row.removeButton.setOnAction(e -> {
            rows.remove(row);
            rowsBox.getChildren().remove(row.container);
        });

        return row;
    }

    public void replaceRows(List<Item> items) {
        rows.clear();
        rowsBox.getChildren().clear();
        addRow(items, null);
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

    public Button getAddRowButton() {
        return addRowButton;
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
        private final ComboBox<Item> itemSelector;
        private final TextField quantityField;
        private final Button removeButton;

        private BatchRow(List<Item> items, Item preselectedItem) {
            itemSelector = new ComboBox<>();
            itemSelector.getItems().setAll(items);
            itemSelector.setPromptText("Seleziona item");
            itemSelector.setMaxWidth(Double.MAX_VALUE);
            if (preselectedItem != null) {
                itemSelector.setValue(preselectedItem);
            }

            quantityField = new TextField();
            quantityField.setPromptText("Quantità");
            quantityField.setPrefWidth(120);

            removeButton = new Button("✕");

            container = new HBox(10, itemSelector, quantityField, removeButton);
            HBox.setHgrow(itemSelector, Priority.ALWAYS);
            container.setAlignment(Pos.CENTER_LEFT);
        }

        public ComboBox<Item> getItemSelector() {
            return itemSelector;
        }

        public TextField getQuantityField() {
            return quantityField;
        }
    }
}
