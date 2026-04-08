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
import java.util.function.Consumer;

public class PresinteringView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Presinterizza");
    private final VBox rowsBox = new VBox(8);
    private final ScrollPane rowsScrollPane = new ScrollPane(rowsBox);
    private final Button insertDisksButton = new Button();
    private final Label feedbackLabel = new Label();
    private final FurnaceCarouselView furnaceCarouselView = new FurnaceCarouselView();
    private final List<DiskPickEntry> diskPickEntries = new ArrayList<>();
    private final VBox compositionRankingBox = new VBox(6);
    private final VBox furnaceSuggestionsBox = new VBox(6);
    private final Label furnaceSuggestionsTitle = new Label("Blocco B · Item consigliati per forno selezionato");

    private String selectedFurnaceName;
    private Consumer<String> onFurnaceSelectionChanged;

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
            updateFurnaceSuggestionsTitle();
            if (onFurnaceSelectionChanged != null) {
                onFurnaceSelectionChanged.accept(furnaceName);
            }
        });

        VBox insightsSection = buildInsightsSection();
        getChildren().addAll(header, contentSplit, insightsSection, feedbackLabel);
    }

    public AppHeader getHeader() {
        return header;
    }

    public void setProducedDisks(List<ProductionRepository.ProducedDiskRow> rows) {
        rowsBox.getChildren().clear();
        diskPickEntries.clear();

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

    public void setOnFurnaceSelectionChanged(Consumer<String> onFurnaceSelectionChanged) {
        this.onFurnaceSelectionChanged = onFurnaceSelectionChanged;
    }

    public void setCompositionRankingRows(List<ProductionRepository.CompositionRankingRow> rows) {
        compositionRankingBox.getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            Label empty = new Label("Nessun gruppo composizione disponibile.");
            empty.setStyle("-fx-opacity: 0.85;");
            compositionRankingBox.getChildren().add(empty);
            return;
        }

        int rank = 1;
        for (ProductionRepository.CompositionRankingRow row : rows) {
            Label line = new Label(
                    "#" + rank
                            + " · Composizione " + row.compositionId()
                            + " · disponibili: " + row.availableQuantity()
                            + " · forni distinti: " + row.distinctFurnacesUsed()
                            + " · firing totali: " + row.totalFirings()
            );
            line.setWrapText(true);
            compositionRankingBox.getChildren().add(line);
            rank++;
        }
    }

    public void setFurnaceItemSuggestionRows(List<ProductionRepository.FurnaceItemSuggestionRow> rows) {
        furnaceSuggestionsBox.getChildren().clear();

        if (rows == null || rows.isEmpty()) {
            String hint = selectedFurnaceName == null || selectedFurnaceName.isBlank()
                    ? "Seleziona un forno per vedere gli item con storico disponibile."
                    : "Nessun item disponibile con storico sul forno selezionato.";
            Label empty = new Label(hint);
            empty.setStyle("-fx-opacity: 0.85;");
            furnaceSuggestionsBox.getChildren().add(empty);
            return;
        }

        for (ProductionRepository.FurnaceItemSuggestionRow row : rows) {
            String temperature = row.suggestedTemperature() == null ? "n.d." : row.suggestedTemperature() + "°C";
            String compositionAverage = row.compositionAverageTemperature() == null
                    ? "n.d."
                    : row.compositionAverageTemperature() + "°C";

            Label line = new Label(
                    row.itemCode()
                            + " · comp. " + row.compositionId()
                            + " · disp: " + row.availableQuantity()
                            + " · T ideale: " + temperature
                            + " · media comp.: " + compositionAverage
            );
            line.setWrapText(true);
            furnaceSuggestionsBox.getChildren().add(line);
        }
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
        diskPickEntries.add(new DiskPickEntry(pickField, row.totalQuantity()));

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
        int requestedDisks = diskPickEntries.stream()
                .mapToInt(entry -> {
                    String text = entry.field().getText();
                    if (text == null || text.isBlank()) {
                        return 0;
                    }

                    int requested = Integer.parseInt(text);
                    return Math.min(requested, entry.availableQuantity());
                })
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

    private VBox buildInsightsSection() {
        Label sectionTitle = new Label("Supporto decisionale presinterizza");
        sectionTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label rankingTitle = new Label("Blocco A · Classifica gruppi composizione");
        rankingTitle.setStyle("-fx-font-weight: bold;");

        compositionRankingBox.setPadding(new Insets(8));
        compositionRankingBox.setStyle(
                "-fx-background-color: rgba(226, 232, 240, 0.25);"
                        + "-fx-border-color: rgba(148, 163, 184, 0.40);"
                        + "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        furnaceSuggestionsTitle.setStyle("-fx-font-weight: bold;");
        furnaceSuggestionsBox.setPadding(new Insets(8));
        furnaceSuggestionsBox.setStyle(
                "-fx-background-color: rgba(224, 242, 254, 0.28);"
                        + "-fx-border-color: rgba(56, 189, 248, 0.40);"
                        + "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        updateFurnaceSuggestionsTitle();

        VBox leftBlock = new VBox(6, rankingTitle, compositionRankingBox);
        VBox rightBlock = new VBox(6, furnaceSuggestionsTitle, furnaceSuggestionsBox);
        HBox.setHgrow(leftBlock, Priority.ALWAYS);
        HBox.setHgrow(rightBlock, Priority.ALWAYS);

        HBox blocks = new HBox(16, leftBlock, rightBlock);
        VBox section = new VBox(8, sectionTitle, blocks);
        section.setPadding(new Insets(6, 0, 0, 0));
        return section;
    }

    private void updateFurnaceSuggestionsTitle() {
        String suffix = selectedFurnaceName == null || selectedFurnaceName.isBlank()
                ? "(nessun forno selezionato)"
                : "(" + selectedFurnaceName + ")";
        furnaceSuggestionsTitle.setText("Blocco B · Item consigliati per forno selezionato " + suffix);
    }

    private record DiskPickEntry(TextField field, int availableQuantity) {
    }
}
