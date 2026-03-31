package com.orodent.tonv2.features.laboratory.composition.view;

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

public class CompositionArchiveView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Archivio Composizioni");
    private final TextField filterNameField = new TextField();
    private final TableView<CompositionRow> compositionsTable = new TableView<>();

    public CompositionArchiveView() {
        setSpacing(12);
        setPadding(new Insets(16));

        filterNameField.setPromptText("Filtra per nome composizione");

        Label title = new Label("Composizioni salvate");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        TableColumn<CompositionRow, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameColumn.setPrefWidth(460);

        TableColumn<CompositionRow, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()));
        idColumn.setPrefWidth(90);

        compositionsTable.getColumns().addAll(nameColumn, idColumn);
        compositionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        compositionsTable.setPlaceholder(new Label("Nessuna composizione trovata."));

        VBox.setVgrow(compositionsTable, Priority.ALWAYS);
        getChildren().addAll(header, title, filterNameField, compositionsTable);
    }

    public void setCompositions(List<CompositionRow> rows) {
        ObservableList<CompositionRow> items = FXCollections.observableArrayList(rows);
        compositionsTable.setItems(items);
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getFilterNameField() {
        return filterNameField;
    }

    public TableView<CompositionRow> getCompositionsTable() {
        return compositionsTable;
    }

    public record CompositionRow(int id, String name) {
    }
}
