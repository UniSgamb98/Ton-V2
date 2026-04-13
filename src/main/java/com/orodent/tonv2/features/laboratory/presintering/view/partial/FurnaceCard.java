package com.orodent.tonv2.features.laboratory.presintering.view.partial;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class FurnaceCard extends VBox {

    public FurnaceCard(String furnaceName,
                       String lotCode,
                       Map<Integer, Integer> plannedItemQty,
                       Map<Integer, String> itemCodeById,
                       Map<Integer, String> productNameByItemId,
                       boolean selected,
                       Runnable onSelect) {
        super(8);
        setPrefWidth(165);
        setMinWidth(165);
        setPadding(new Insets(10));
        setStyle(buildCardStyle(selected));
        setOnMouseClicked(e -> onSelect.run());

        Label title = new Label(furnaceName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lotLabel = new Label((lotCode == null || lotCode.isBlank()) ? "" : lotCode);
        lotLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-opacity: 0.90;");

        HBox headerRow = new HBox(6, title, spacer, lotLabel);
        headerRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        VBox itemsBox = new VBox(4);
        if (plannedItemQty == null || plannedItemQty.isEmpty()) {
            Label row = new Label("Nessun disco pianificato.");
            row.setStyle("-fx-font-size: 12px; -fx-opacity: 0.75;");
            itemsBox.getChildren().add(row);
        } else {
            Map<String, Integer> totalsByProduct = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> entry : plannedItemQty.entrySet()) {
                String productName = productNameByItemId.get(entry.getKey());
                if (productName == null || productName.isBlank()) {
                    productName = itemCodeById.getOrDefault(entry.getKey(), "Item " + entry.getKey());
                }
                totalsByProduct.merge(productName, entry.getValue(), Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : totalsByProduct.entrySet()) {
                Label row = new Label(entry.getKey() + " — " + entry.getValue() + " pz");
                row.setStyle("-fx-font-size: 12px; -fx-opacity: 0.92;");
                itemsBox.getChildren().add(row);
            }
        }

        getChildren().addAll(headerRow, itemsBox);
    }

    private String buildCardStyle(boolean selected) {
        if (selected) {
            return "-fx-background-color: rgba(56, 189, 248, 0.36);"
                    + "-fx-border-color: rgba(14, 165, 233, 1.0);"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 10; -fx-background-radius: 10;";
        }

        return "-fx-background-color: rgba(56, 189, 248, 0.18);"
                + "-fx-border-color: rgba(125, 211, 252, 0.9);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;";
    }
}
