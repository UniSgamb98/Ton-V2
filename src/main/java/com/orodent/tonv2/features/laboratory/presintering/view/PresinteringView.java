package com.orodent.tonv2.features.laboratory.presintering.view;

import com.orodent.tonv2.core.components.AppHeader;
import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringPlanningSnapshot;
import com.orodent.tonv2.features.laboratory.presintering.view.partial.FurnaceCarouselView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.function.Consumer;

public class PresinteringView extends VBox {

    private final AppHeader header = new AppHeader("Laboratorio - Presinterizza");
    private final VBox rowsBox = new VBox(8);
    private final ScrollPane rowsScrollPane = new ScrollPane(rowsBox);
    private final Button insertDisksButton = new Button();
    private final Label feedbackLabel = new Label();
    private final FurnaceCarouselView furnaceCarouselView = new FurnaceCarouselView();
    private final List<DiskPickEntry> diskPickEntries = new ArrayList<>();
    private final Map<Integer, DiskPickEntry> diskEntriesByItemId = new LinkedHashMap<>();
    private final Map<Integer, Map<Integer, Integer>> plannedByFurnace = new LinkedHashMap<>();
    private final Map<Integer, String> itemCodeById = new LinkedHashMap<>();
    private final Map<Integer, String> productNameByItemId = new LinkedHashMap<>();
    private final Map<Integer, String> furnaceNameById = new LinkedHashMap<>();
    private final VBox compositionRankingBox = new VBox(6);
    private final VBox furnaceSuggestionsBox = new VBox(6);
    private final Label furnaceSuggestionsTitle = new Label("Item consigliati per forno selezionato");
    private final VBox selectedFurnaceCard = new VBox(8);
    private final HBox selectedFurnaceSection = new HBox(12);
    private final Region selectedFurnaceSectionSpacer = new Region();
    private final Label selectedFurnaceCardTitle = new Label();
    private final TextField selectedFurnaceMaxTemperatureField = new TextField();
    private final DatePicker selectedFurnaceDepartureDatePicker = new DatePicker();
    private final VBox selectedFurnaceItemsBox = new VBox(6);
    private final Button confirmPresinteringButton = new Button("Conferma tutti\ni forni");
    private Runnable onInsertDisksRequested;
    private Consumer<Integer> onRemovePlannedItemRequested;

    private String selectedFurnaceName;
    private Integer selectedFurnaceId;
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
        insertDisksButton.setOnAction(e -> {
            if (onInsertDisksRequested != null) {
                onInsertDisksRequested.run();
            }
        });

        Label leftTitle = new Label("Dischi prodotti");
        leftTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox leftColumn = new VBox(10, leftTitle, rowsScrollPane, insertDisksButton);
        leftColumn.setMinWidth(260);
        leftColumn.setPrefWidth(320);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(leftColumn, Priority.ALWAYS);

        buildSelectedFurnaceCard();

        VBox rightColumn = new VBox(10, furnaceCarouselView, selectedFurnaceSection);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(selectedFurnaceSection, Priority.ALWAYS);
        VBox.setVgrow(selectedFurnaceSection, Priority.ALWAYS);

        HBox contentSplit = new HBox(20, leftColumn, rightColumn);
        contentSplit.setFillHeight(true);
        VBox.setVgrow(contentSplit, Priority.ALWAYS);

        furnaceCarouselView.setOnFurnaceSelectionChanged(selection -> {
            selectedFurnaceName = selection == null ? null : selection.furnaceName();
            selectedFurnaceId = selection == null ? null : selection.furnaceId();
            updateInsertButton();
            updateFurnaceSuggestionsTitle();
            refreshSelectedFurnaceCard();
            if (onFurnaceSelectionChanged != null) {
                onFurnaceSelectionChanged.accept(selectedFurnaceName);
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
        diskEntriesByItemId.clear();
        itemCodeById.clear();
        productNameByItemId.clear();
        plannedByFurnace.clear();

        if (rows == null || rows.isEmpty()) {
            rowsBox.getChildren().add(new Label("Nessun disco prodotto disponibile."));
            furnaceCarouselView.setPlannedItems(plannedByFurnace, itemCodeById, productNameByItemId);
            updateInsertButton();
            return;
        }

        for (ProductionRepository.ProducedDiskRow row : rows) {
            rowsBox.getChildren().add(buildDiskRow(row));
        }

        furnaceCarouselView.setPlannedItems(plannedByFurnace, itemCodeById, productNameByItemId);
        refreshSelectedFurnaceCard();
        updateInsertButton();
    }

    public void setFurnaces(List<Furnace> furnaces) {
        furnaceNameById.clear();
        if (furnaces != null) {
            for (Furnace furnace : furnaces) {
                String displayNumber = furnace.number() == null || furnace.number().isBlank()
                        ? String.valueOf(furnace.id())
                        : furnace.number();
                furnaceNameById.put(furnace.id(), "Forno " + displayNumber);
            }
        }
        furnaceCarouselView.setFurnaces(furnaces);
        furnaceCarouselView.setPlannedItems(plannedByFurnace, itemCodeById, productNameByItemId);
        refreshSelectedFurnaceCard();
    }

    public void setFeedback(String text, boolean error) {
        feedbackLabel.setText(text);
        feedbackLabel.setStyle(error ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #166534;");
    }

    public void setOnFurnaceSelectionChanged(Consumer<String> onFurnaceSelectionChanged) {
        this.onFurnaceSelectionChanged = onFurnaceSelectionChanged;
    }

    public void setOnInsertDisksRequested(Runnable onInsertDisksRequested) {
        this.onInsertDisksRequested = onInsertDisksRequested;
    }

    public void setOnRemovePlannedItemRequested(Consumer<Integer> onRemovePlannedItemRequested) {
        this.onRemovePlannedItemRequested = onRemovePlannedItemRequested;
    }

    public ComboBox<String> getTemplateSelector() {
        return furnaceCarouselView.getTemplateSelector();
    }

    public void setTemplateNames(List<String> names, String preselectedName) {
        furnaceCarouselView.setTemplateNames(names, preselectedName);
    }

    public void applyPlanningSnapshot(PresinteringPlanningSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        plannedByFurnace.clear();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : snapshot.plannedByFurnace().entrySet()) {
            plannedByFurnace.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }

        for (Map.Entry<Integer, DiskPickEntry> entry : diskEntriesByItemId.entrySet()) {
            int itemId = entry.getKey();
            DiskPickEntry diskEntry = entry.getValue();
            int available = snapshot.availableByItemId().getOrDefault(itemId, diskEntry.availableQuantity);
            diskEntry.availableQuantity = Math.max(0, available);
            diskEntry.quantityLabel.setText(diskEntry.availableQuantity + " pz");
            diskEntry.pickField.clear();
        }

        furnaceCarouselView.setPlannedItems(plannedByFurnace, itemCodeById, productNameByItemId);
        refreshSelectedFurnaceCard();
        updateInsertButton();
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
                            + " · " + row.productName()
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
            String sanitized = newValue == null ? "" : newValue.replaceAll("\\D", "");
            if (!sanitized.equals(newValue)) {
                pickField.setText(sanitized);
                return;
            }
            updateInsertButton();
        });
        DiskPickEntry entry = new DiskPickEntry(
                row.itemId(),
                pickField,
                quantityLabel,
                row.totalQuantity()
        );
        diskPickEntries.add(entry);
        diskEntriesByItemId.put(row.itemId(), entry);
        itemCodeById.put(row.itemId(), row.itemCode());
        productNameByItemId.put(row.itemId(), row.productName());

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
                    String text = entry.pickField.getText();
                    if (text == null || text.isBlank()) {
                        return 0;
                    }

                    int requested = Integer.parseInt(text);
                    return Math.min(requested, entry.availableQuantity);
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

    private void buildSelectedFurnaceCard() {
        selectedFurnaceCardTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        selectedFurnaceMaxTemperatureField.setPromptText("Max temperature (°C)");
        selectedFurnaceMaxTemperatureField.textProperty().addListener((obs, oldValue, newValue) -> {
            String sanitized = newValue == null ? "" : newValue.replaceAll("\\D", "");
            if (!sanitized.equals(newValue)) {
                selectedFurnaceMaxTemperatureField.setText(sanitized);
            }
        });
        selectedFurnaceDepartureDatePicker.setPromptText("Partenza");
        selectedFurnaceDepartureDatePicker.setValue(LocalDate.now());

        Label fieldsLabel = new Label("Parametri firing");
        fieldsLabel.setStyle("-fx-font-weight: bold;");
        HBox firingFieldsRow = new HBox(8,
                selectedFurnaceMaxTemperatureField,
                selectedFurnaceDepartureDatePicker
        );
        selectedFurnaceMaxTemperatureField.setPrefWidth(180);
        selectedFurnaceDepartureDatePicker.setPrefWidth(170);

        Label itemListLabel = new Label("Item pianificati");
        itemListLabel.setStyle("-fx-font-weight: bold;");

        VBox cardContent = new VBox(8, selectedFurnaceCardTitle, fieldsLabel, firingFieldsRow, itemListLabel, selectedFurnaceItemsBox);
        HBox.setHgrow(cardContent, Priority.ALWAYS);

        confirmPresinteringButton.setPrefWidth(120);
        confirmPresinteringButton.setMinWidth(120);
        confirmPresinteringButton.setPrefHeight(240);
        confirmPresinteringButton.setMinHeight(220);
        confirmPresinteringButton.setWrapText(true);
        confirmPresinteringButton.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        confirmPresinteringButton.setVisible(true);
        confirmPresinteringButton.setManaged(true);

        selectedFurnaceCard.setPadding(new Insets(10));
        selectedFurnaceCard.setStyle(
                "-fx-background-color: rgba(224, 242, 254, 0.20);"
                        + "-fx-border-color: rgba(56, 189, 248, 0.45);"
                        + "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        selectedFurnaceCard.getChildren().add(cardContent);
        selectedFurnaceCard.setVisible(false);
        selectedFurnaceCard.setManaged(false);

        selectedFurnaceSection.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(selectedFurnaceCard, Priority.ALWAYS);
        HBox.setHgrow(selectedFurnaceSectionSpacer, Priority.ALWAYS);
        selectedFurnaceSection.getChildren().addAll(selectedFurnaceCard, selectedFurnaceSectionSpacer, confirmPresinteringButton);
    }

    private void refreshSelectedFurnaceCard() {
        selectedFurnaceItemsBox.getChildren().clear();
        boolean hasPlannedFurnaces = plannedByFurnace.values().stream().anyMatch(items -> items != null && !items.isEmpty());

        if (selectedFurnaceId == null || selectedFurnaceName == null || selectedFurnaceName.isBlank()) {
            selectedFurnaceCard.setVisible(false);
            selectedFurnaceCard.setManaged(false);
            confirmPresinteringButton.setDisable(!hasPlannedFurnaces);
            return;
        }

        selectedFurnaceCard.setVisible(true);
        selectedFurnaceCard.setManaged(true);
        confirmPresinteringButton.setDisable(!hasPlannedFurnaces);
        selectedFurnaceCardTitle.setText(selectedFurnaceName);
        if (selectedFurnaceDepartureDatePicker.getValue() == null) {
            selectedFurnaceDepartureDatePicker.setValue(LocalDate.now());
        }

        Map<Integer, Integer> plannedItems = plannedByFurnace.getOrDefault(selectedFurnaceId, Map.of());
        if (plannedItems.isEmpty()) {
            Label empty = new Label("Nessun item pianificato in questo forno.");
            empty.setStyle("-fx-opacity: 0.80;");
            selectedFurnaceItemsBox.getChildren().add(empty);
            return;
        }

        for (Map.Entry<Integer, Integer> entry : plannedItems.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();

            Button removeButton = new Button("🗑");
            removeButton.setFocusTraversable(false);
            removeButton.setOnAction(e -> {
                if (onRemovePlannedItemRequested != null) {
                    onRemovePlannedItemRequested.accept(itemId);
                }
            });

            String itemCode = itemCodeById.getOrDefault(itemId, "Item " + itemId);
            Label rowLabel = new Label(itemCode + " — " + quantity + " pz");

            HBox row = new HBox(8, removeButton, rowLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            selectedFurnaceItemsBox.getChildren().add(row);
        }
    }

    public Map<Integer, Integer> getRequestedDiskQuantitiesByItem() {
        Map<Integer, Integer> requested = new LinkedHashMap<>();
        for (DiskPickEntry entry : diskPickEntries) {
            String text = entry.pickField.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            requested.put(entry.itemId, Integer.parseInt(text));
        }
        return requested;
    }

    public void clearRequestedDiskQuantities() {
        for (DiskPickEntry entry : diskPickEntries) {
            entry.pickField.clear();
        }
        updateInsertButton();
    }

    public Button getConfirmPresinteringButton() {
        return confirmPresinteringButton;
    }

    public TextField getSelectedFurnaceMaxTemperatureField() {
        return selectedFurnaceMaxTemperatureField;
    }

    public DatePicker getSelectedFurnaceDepartureDatePicker() {
        return selectedFurnaceDepartureDatePicker;
    }

    public Integer getSelectedFurnaceId() {
        return selectedFurnaceId;
    }

    public String getSelectedFurnaceName() {
        return selectedFurnaceName;
    }

    public void setSelectedFurnaceParameters(Integer maxTemperature, LocalDate departureDate) {
        selectedFurnaceMaxTemperatureField.setText(maxTemperature == null ? "" : String.valueOf(maxTemperature));
        selectedFurnaceDepartureDatePicker.setValue(departureDate == null ? LocalDate.now() : departureDate);
    }

    private VBox buildInsightsSection() {
        Label sectionTitle = new Label("Supporto decisionale presinterizza");
        sectionTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label rankingTitle = new Label("Classifica gruppi composizione");
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
        furnaceSuggestionsTitle.setText("Item consigliati per forno selezionato " + suffix);
    }

    private static final class DiskPickEntry {
        private final int itemId;
        private final TextField pickField;
        private final Label quantityLabel;
        private int availableQuantity;

        private DiskPickEntry(int itemId,
                              TextField pickField,
                              Label quantityLabel,
                              int availableQuantity) {
            this.itemId = itemId;
            this.pickField = pickField;
            this.quantityLabel = quantityLabel;
            this.availableQuantity = availableQuantity;
        }
    }

}
