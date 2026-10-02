package com.orodent.tonv2.features.registers.home.view.partial;

import com.orodent.tonv2.features.registers.home.presentation.RegistersViewState;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class DocumentsRegisterView extends VBox {
    private final VBox documents = new VBox(9);

    public DocumentsRegisterView() {
        Label title = new Label("Template disponibili");
        title.getStyleClass().add("registers-section-title");
        Label description = new Label("Gli ultimi template salvati utilizzabili per ricostruire i documenti del registro.");
        description.getStyleClass().add("registers-muted");
        description.setWrapText(true);
        getChildren().addAll(title, description, documents);
        setSpacing(8);
        getStyleClass().add("registers-tab-content");
    }

    public void render(List<RegistersViewState.DocumentViewState> entries) {
        documents.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("Nessun template salvato disponibile.");
            empty.getStyleClass().add("registers-notice");
            documents.getChildren().add(empty);
            return;
        }
        entries.forEach(entry -> documents.getChildren().add(documentRow(entry)));
    }

    private HBox documentRow(RegistersViewState.DocumentViewState entry) {
        Label icon = new Label("▤");
        icon.getStyleClass().add("registers-document-icon");
        Label name = new Label(entry.name());
        name.getStyleClass().add("registers-row-value");
        Label details = new Label(entry.detailsText());
        details.getStyleClass().add("registers-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, icon, new VBox(3, name, details), spacer);
        row.getStyleClass().add("registers-data-row");
        return row;
    }
}
