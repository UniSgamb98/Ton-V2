package com.orodent.tonv2.features.laboratory.production.view.partial;

import com.orodent.tonv2.features.laboratory.production.presentation.BatchProductionViewState;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class BatchProductionSummaryView extends VBox {

    private final Label lineValue = valueLabel();
    private final Label productValue = valueLabel();
    private final Label itemsValue = valueLabel();
    private final Label quantityValue = valueLabel();
    private final Label documentValue = valueLabel();
    private final Label readiness = new Label("Seleziona una linea");

    public BatchProductionSummaryView() {
        Label title = new Label("Riepilogo batch");
        title.getStyleClass().add("batch-summary-title");
        readiness.getStyleClass().addAll("batch-readiness", "batch-readiness-warning");
        readiness.setWrapText(true);

        setSpacing(14);
        getStyleClass().add("batch-summary-card");
        getChildren().addAll(
                title,
                summaryRow("Linea", lineValue),
                summaryRow("Prodotto", productValue),
                summaryRow("Articoli configurati", itemsValue),
                summaryRow("Quantità totale", quantityValue),
                summaryRow("Documento", documentValue),
                fieldLabel("Stato"),
                readiness
        );
    }

    public void render(BatchProductionViewState state) {
        lineValue.setText(state.lineText());
        productValue.setText(state.productText());
        itemsValue.setText(Integer.toString(state.configuredItems()));
        quantityValue.setText(state.totalQuantity() + " pezzi");
        documentValue.setText(state.documentText());
        readiness.setText(state.readinessText());
        readiness.getStyleClass().removeAll("batch-readiness-ready", "batch-readiness-warning");
        readiness.getStyleClass().add(state.ready() ? "batch-readiness-ready" : "batch-readiness-warning");
    }

    private VBox summaryRow(String title, Label value) {
        return new VBox(3, fieldLabel(title), value);
    }

    private Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("batch-summary-label");
        return label;
    }

    private static Label valueLabel() {
        Label label = new Label("—");
        label.getStyleClass().add("batch-summary-value");
        label.setWrapText(true);
        return label;
    }
}
