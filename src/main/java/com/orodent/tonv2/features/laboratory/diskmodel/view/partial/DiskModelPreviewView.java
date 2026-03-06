package com.orodent.tonv2.features.laboratory.diskmodel.view.partial;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class DiskModelPreviewView extends VBox {

    private final VBox stack = new VBox(2);

    public DiskModelPreviewView() {
        setSpacing(8);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Anteprima modello disco");
        title.setStyle("-fx-font-weight: bold;");

        stack.setPrefWidth(220);
        stack.setMaxWidth(220);

        getChildren().addAll(title, stack);
        update(1.0, 1.0, List.of(100.0));
    }

    public void update(double superiorOvermaterialMm, double inferiorOvermaterialMm, List<Double> layerPercentages) {
        stack.getChildren().clear();

        addSegment("Over sup. " + fmt(superiorOvermaterialMm) + "mm", 18, Color.web("#f8b4b4"));

        for (int i = 0; i < layerPercentages.size(); i++) {
            double pct = layerPercentages.get(i);
            double h = Math.max(12, Math.min(60, pct * 0.9));
            addSegment("Layer " + (i + 1) + "  " + fmt(pct) + "%", h, pickColor(i));
        }

        addSegment("Over inf. " + fmt(inferiorOvermaterialMm) + "mm", 18, Color.web("#f8b4b4"));
    }

    private void addSegment(String text, double height, Color color) {
        StackPane box = new StackPane();
        box.setMinHeight(height);
        box.setPrefHeight(height);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: " + toHex(color) + "; -fx-border-color: #374151;");

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000;");
        box.getChildren().add(label);

        stack.getChildren().add(box);
    }

    private Color pickColor(int idx) {
        Color[] palette = new Color[] {
                Color.web("#bfdbfe"),
                Color.web("#c7f9cc"),
                Color.web("#fde68a"),
                Color.web("#e9d5ff"),
                Color.web("#fecdd3"),
                Color.web("#bae6fd")
        };
        return palette[idx % palette.length];
    }

    private String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
