package com.orodent.tonv2.features.laboratory.presintering.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.laboratory.presintering.view.partial.FurnaceCarouselView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class PresinteringView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Presinterizza");
    private final VBox rowsBox = new VBox(8);
    private final ScrollPane rowsScrollPane = new ScrollPane(rowsBox);
    private final Button insertDisksButton = new Button();
    private final Label feedbackLabel = new Label();
    private final FurnaceCarouselView furnaceCarouselView = new FurnaceCarouselView();
    private final List<TextField> quantityFields = new ArrayList<>();

    private String selectedFurnaceName;

    public PresinteringView() {
        setSpacing(16);
        setPadding(new Insets(20));

        rowsScrollPane.setFitToWidth(true);
        rowsScrollPane.setPrefViewportHeight(320);
        rowsScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(rowsScrollPane, Priority.ALWAYS);

        insertDisksButton.setVisible(false);
        insertDisksButton.setManaged(false);
        insertDisksButton.setStyle("-fx-font-weight: bold;");
        insertDisksButton.setOnAction(e -> setFeedback(insertDisksButton.getText(), false));

        Label leftTitle = new Label("Dischi prodotti");
        leftTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox leftColumn = new VBox(10, leftTitle, rowsScrollPane, insertDisksButton);
        leftColumn.setMinWidth(260);
        leftColumn.setPrefWidth(320);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(leftColumn, Priority.ALWAYS);

        VBox rightColumn = new VBox(furnaceCarouselView);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox contentSplit = new HBox(20, leftColumn, rightColumn);
        contentSplit.setFillHeight(true);
        VBox.setVgrow(contentSplit, Priority.ALWAYS);

        furnaceCarouselView.setOnFurnaceSelectionChanged(furnaceName -> {
            selectedFurnaceName = furnaceName;
            updateInsertButton();
        });

        getChildren().addAll(header, contentSplit, feedbackLabel);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setProducedDisks(List<ProductionRepository.ProducedDiskRow> rows) {
        rowsBox.getChildren().clear();
        quantityFields.clear();

        if (rows == null || rows.isEmpty()) {
            rowsBox.getChildren().add(new Label("Nessun disco prodotto disponibile."));
            updateInsertButton();
            return;
        }

        for (ProductionRepository.ProducedDiskRow row : rows) {
            rowsBox.getChildren().add(buildDiskRow(row));
        }

        updateInsertButton();
    }

    public void setFurnaces(List<Furnace> furnaces) {
        furnaceCarouselView.setFurnaces(furnaces);
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    private HBox buildDiskRow(ProductionRepository.ProducedDiskRow row) {
        Label itemLabel = new Label(row.itemCode());
        itemLabel.setPrefWidth(110);
        itemLabel.setMinWidth(110);
        itemLabel.setStyle("-fx-font-weight: bold;");

        Label quantityLabel = new Label(row.totalQuantity() + " pz");
        quantityLabel.setPrefWidth(60);

        TextField pickField = new TextField();
        pickField.setPromptText("Preleva");
        pickField.setPrefWidth(90);
        pickField.textProperty().addListener((obs, oldValue, newValue) -> {
            String sanitized = newValue == null ? "" : newValue.replaceAll("[^\\d]", "");
            if (!sanitized.equals(newValue)) {
                pickField.setText(sanitized);
                return;
            }
            updateInsertButton();
        });
        quantityFields.add(pickField);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rowBox = new HBox(8, itemLabel, quantityLabel, spacer, pickField);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setPadding(new Insets(8));
        rowBox.setStyle(
                "-fx-background-color: rgba(226, 232, 240, 0.35);"
                        + "-fx-border-color: rgba(148, 163, 184, 0.45);"
                        + "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        return rowBox;
    }

    private void updateInsertButton() {
        int requestedDisks = quantityFields.stream()
                .map(TextField::getText)
                .filter(text -> text != null && !text.isBlank())
                .mapToInt(Integer::parseInt)
                .sum();

        boolean hasRequestedDisks = requestedDisks > 0;
        insertDisksButton.setVisible(hasRequestedDisks);
        insertDisksButton.setManaged(hasRequestedDisks);

        if (!hasRequestedDisks) {
            return;
        }

        if (selectedFurnaceName == null || selectedFurnaceName.isBlank()) {
            insertDisksButton.setDisable(true);
            insertDisksButton.setText("Seleziona un forno per inserire " + requestedDisks + " dischi");
            return;
        }

        insertDisksButton.setDisable(false);
        insertDisksButton.setText("Inserisci " + requestedDisks + " dischi nel " + selectedFurnaceName);
    }
}
