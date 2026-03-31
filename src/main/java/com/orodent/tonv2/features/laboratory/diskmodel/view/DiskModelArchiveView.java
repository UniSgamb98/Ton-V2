package com.orodent.tonv2.features.laboratory.diskmodel.view;

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

public class DiskModelArchiveView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Archivio Dischi");
    private final TextField filterNameField = new TextField();
    private final TableView<DiskModelRow> diskModelsTable = new TableView<>();

    public DiskModelArchiveView() {
        setSpacing(12);
        setPadding(new Insets(16));

        filterNameField.setPromptText("Filtra per nome modello disco");

        Label title = new Label("Modelli disco salvati");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");

        TableColumn<DiskModelRow, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameColumn.setPrefWidth(460);

        TableColumn<DiskModelRow, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()));
        idColumn.setPrefWidth(90);

        diskModelsTable.getColumns().addAll(nameColumn, idColumn);
        diskModelsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        diskModelsTable.setPlaceholder(new Label("Nessun modello disco trovato."));

        VBox.setVgrow(diskModelsTable, Priority.ALWAYS);
        getChildren().addAll(header, title, filterNameField, diskModelsTable);
    }

    public void setDiskModels(List<DiskModelRow> rows) {
        ObservableList<DiskModelRow> items = FXCollections.observableArrayList(rows);
        diskModelsTable.setItems(items);
    }

    public AppHeader getHeader() {
        return header;
    }

    public TextField getFilterNameField() {
        return filterNameField;
    }

    public TableView<DiskModelRow> getDiskModelsTable() {
        return diskModelsTable;
    }

    public record DiskModelRow(int id, String name) {
    }
}
