package com.orodent.tonv2.features.documents.archive.view;

import com.orodent.tonv2.core.components.AppHeader;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class DocumentsArchiveView extends VBox {

    private final AppHeader header = new AppHeader("Documenti - Archivio Template");
    private final TextField filterNameField = new TextField();
    private final TableView<TemplateRow> templatesTable = new TableView<>();

    public DocumentsArchiveView() {
        setSpacing(12);
        setPadding(new Insets(16));

        filterNameField.setPromptText("Filtra per nome template");

        Label title = new Label("Template salvati");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        TableColumn<TemplateRow, String> nameColumn = new TableColumn<>("Nome template");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameColumn.setPrefWidth(460);

        TableColumn<TemplateRow, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()));
        idColumn.setPrefWidth(90);

        templatesTable.getColumns().addAll(nameColumn, idColumn);
        templatesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        templatesTable.setPlaceholder(new Label("Nessun template trovato."));

        VBox.setVgrow(templatesTable, Priority.ALWAYS);
        getChildren().addAll(header, title, filterNameField, templatesTable);
    }

    public void setTemplates(List<TemplateRow> rows) {
        ObservableList<TemplateRow> items = FXCollections.observableArrayList(rows);
        templatesTable.setItems(items);
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getFilterNameField() {
        return filterNameField;
    }

    public TableView<TemplateRow> getTemplatesTable() {
        return templatesTable;
    }

    public record TemplateRow(int id, String name) {
    }
}
