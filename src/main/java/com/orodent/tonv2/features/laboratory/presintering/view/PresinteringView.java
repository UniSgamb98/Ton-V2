package com.orodent.tonv2.features.laboratory.presintering.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class PresinteringView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Presinterizza");
    private final VBox rowsBox = new VBox(8);
    private final Label feedbackLabel = new Label();

    public PresinteringView() {
        setSpacing(16);
        setPadding(new Insets(20));

        getChildren().addAll(
                header,
                new Label("Dischi prodotti"),
                rowsBox,
                feedbackLabel
        );
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setProducedDisks(List<ProductionRepository.ProducedDiskRow> rows) {
        rowsBox.getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            rowsBox.getChildren().add(new Label("Nessun disco prodotto disponibile."));
            return;
        }

        for (ProductionRepository.ProducedDiskRow row : rows) {
            String text = row.itemCode() + " — " + row.totalQuantity() + " pz";
            rowsBox.getChildren().add(new Label(text));
        }
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }
}
