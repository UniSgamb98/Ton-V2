package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringPlanningSnapshot;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PresinteringController {

    private final PresinteringView view;

    private final PresinteringService service;
    private final DocumentBrowserService documentBrowserService;
    private final java.util.Map<Integer, PresinteringService.FurnaceConfig> furnaceConfigById = new java.util.LinkedHashMap<>();
    private final Map<Integer, Integer> availableByItemState = new LinkedHashMap<>();
    private final Map<Integer, String> itemCodeByIdState = new LinkedHashMap<>();
    private final Map<Integer, Map<Integer, Integer>> plannedByFurnaceState = new LinkedHashMap<>();
    private final Map<Integer, String> furnaceNameByIdState = new LinkedHashMap<>();

    public PresinteringController(PresinteringView view,
                                  PresinteringService service,
                                  DocumentBrowserService documentBrowserService) {
        this.view = view;
        this.service = service;
        this.documentBrowserService = documentBrowserService;

        setupActions();
        loadData();
    }

    private void setupActions() {
        view.getConfirmPresinteringButton().setOnAction(e -> confirmAllPlannedFurnaces());
        view.getSelectedFurnaceMaxTemperatureField().textProperty().addListener((obs, oldValue, newValue) -> syncSelectedFurnaceConfigFromView());
        view.getSelectedFurnaceDepartureDatePicker().valueProperty().addListener((obs, oldValue, newValue) -> syncSelectedFurnaceConfigFromView());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                service.setLastTemplateName(newValue)
        );
        view.setOnInsertDisksRequested(this::insertDisksIntoSelectedFurnace);
        view.setOnRemovePlannedItemRequested(this::removePlannedItemFromSelectedFurnace);
    }

    private void loadData() {
        try {
            List<ProductionRepository.ProducedDiskRow> producedDisks = service.loadProducedDisks();
            List<Furnace> furnaces = service.loadFurnaces();
            List<ProductionRepository.CompositionRankingRow> compositionRanking = service.loadCompositionRanking();
            furnaceNameByIdState.clear();
            for (Furnace furnace : furnaces) {
                String displayNumber = furnace.number() == null || furnace.number().isBlank()
                        ? String.valueOf(furnace.id())
                        : furnace.number();
                furnaceNameByIdState.put(furnace.id(), "Forno " + displayNumber);
            }
            availableByItemState.clear();
            itemCodeByIdState.clear();
            for (ProductionRepository.ProducedDiskRow row : producedDisks) {
                availableByItemState.put(row.itemId(), row.totalQuantity());
                itemCodeByIdState.put(row.itemId(), row.itemCode());
            }
            plannedByFurnaceState.clear();

            view.setProducedDisks(producedDisks);
            view.setFurnaces(furnaces);
            view.setCompositionRankingRows(compositionRanking);
            view.setFurnaceItemSuggestionRows(List.of());
            service.loadValidSnapshot(producedDisks).ifPresent(snapshot -> {
                view.applyPlanningSnapshot(snapshot);
                syncPlanningStateFromSnapshot(snapshot);
            });
            view.setOnFurnaceSelectionChanged(selectedFurnace -> {
                List<ProductionRepository.FurnaceItemSuggestionRow> suggestions = service.loadFurnaceItemSuggestions(selectedFurnace);
                view.setFurnaceItemSuggestionRows(suggestions);
                Integer selectedFurnaceId = view.getSelectedFurnaceId();
                PresinteringService.FurnaceConfig config = selectedFurnaceId == null
                        ? null
                        : furnaceConfigById.get(selectedFurnaceId);
                view.setSelectedFurnaceParameters(
                        config == null ? null : config.maxTemperature(),
                        config == null ? null : config.departureDate()
                );
            });
            refreshTemplateSelector();
            view.setFeedback("", false);
        } catch (Exception e) {
            view.setProducedDisks(List.of());
            view.setFurnaces(List.of());
            view.setCompositionRankingRows(List.of());
            view.setFurnaceItemSuggestionRows(List.of());
            view.setFeedback("Errore durante il caricamento dati presinterizza.", true);
        }
    }

    private void refreshTemplateSelector() {
        view.setTemplateNames(service.findTemplateNames(), service.getLastTemplateName());
    }

    private void confirmAllPlannedFurnaces() {
        try {
            List<PresinteringService.BatchConfirmationRequest> furnaceRequests = service.buildBatchConfirmationRequests(
                    plannedByFurnaceState,
                    furnaceNameByIdState,
                    furnaceConfigById
            );
            PresinteringService.ConfirmBatchResult result = service.confirmBatch(
                    new PresinteringService.ConfirmBatchCommand(
                            furnaceRequests,
                            view.getTemplateSelector().getValue()
                    )
            );
            if (result.documentPath() != null) {
                documentBrowserService.openDocument(result.documentPath());
            }

            service.clearSnapshot();
            loadData();
            view.setFeedback(
                    "Presinterizzazione confermata su " + result.confirmedFurnaces() + " forni."
                            + " · firing: " + result.firingIds()
                            + " · ordini collegati: " + result.totalLinkedOrders()
                            + " · lotti creati: " + result.totalLots()
                            + (result.documentPath() == null ? "" : " · documento batch aperto: " + result.documentPath()),
                    false
            );
        } catch (Exception e) {
            view.setFeedback("Errore conferma presinterizzazione: " + e.getMessage(), true);
        }
    }

    private void syncSelectedFurnaceConfigFromView() {
        Integer selectedFurnaceId = view.getSelectedFurnaceId();
        if (selectedFurnaceId == null) {
            return;
        }

        Integer maxTemperature = null;
        String maxTemperatureText = view.getSelectedFurnaceMaxTemperatureField().getText();
        if (maxTemperatureText != null && !maxTemperatureText.isBlank()) {
            maxTemperature = Integer.parseInt(maxTemperatureText);
        }
        furnaceConfigById.put(
                selectedFurnaceId,
                new PresinteringService.FurnaceConfig(
                        maxTemperature,
                        view.getSelectedFurnaceDepartureDatePicker().getValue()
                )
        );
    }

    private void syncPlanningStateFromSnapshot(PresinteringPlanningSnapshot snapshot) {
        availableByItemState.clear();
        plannedByFurnaceState.clear();
        if (snapshot == null) {
            return;
        }
        if (snapshot.availableByItemId() != null) {
            availableByItemState.putAll(snapshot.availableByItemId());
        }
        if (snapshot.itemCodeById() != null) {
            itemCodeByIdState.clear();
            itemCodeByIdState.putAll(snapshot.itemCodeById());
        }
        if (snapshot.plannedByFurnace() == null) {
            return;
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : snapshot.plannedByFurnace().entrySet()) {
            plannedByFurnaceState.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
    }

    private void insertDisksIntoSelectedFurnace() {
        Integer selectedFurnaceId = view.getSelectedFurnaceId();
        String selectedFurnaceName = view.getSelectedFurnaceName();
        if (selectedFurnaceId == null || selectedFurnaceName == null || selectedFurnaceName.isBlank()) {
            view.setFeedback("Seleziona un forno prima di inserire i dischi.", true);
            return;
        }

        Map<Integer, Integer> requestedByItem = view.getRequestedDiskQuantitiesByItem();
        if (requestedByItem.isEmpty()) {
            view.setFeedback("Nessun disco inserito: controlla quantità disponibili.", true);
            return;
        }

        Map<Integer, Integer> targetPlan = plannedByFurnaceState.computeIfAbsent(selectedFurnaceId, ignored -> new LinkedHashMap<>());
        int inserted = 0;
        for (Map.Entry<Integer, Integer> entry : requestedByItem.entrySet()) {
            int itemId = entry.getKey();
            int requested = entry.getValue() == null ? 0 : entry.getValue();
            int available = availableByItemState.getOrDefault(itemId, 0);
            int toInsert = Math.min(requested, available);
            if (toInsert <= 0) {
                continue;
            }
            availableByItemState.put(itemId, available - toInsert);
            targetPlan.merge(itemId, toInsert, Integer::sum);
            inserted += toInsert;
        }

        if (inserted <= 0) {
            view.setFeedback("Nessun disco inserito: controlla quantità disponibili.", true);
            view.clearRequestedDiskQuantities();
            return;
        }

        view.clearRequestedDiskQuantities();
        renderAndPersistPlanningState();
        view.setFeedback("Pianificati " + inserted + " dischi nel " + selectedFurnaceName + ".", false);
    }

    private void removePlannedItemFromSelectedFurnace(int itemId) {
        Integer selectedFurnaceId = view.getSelectedFurnaceId();
        if (selectedFurnaceId == null) {
            return;
        }

        Map<Integer, Integer> plannedItems = plannedByFurnaceState.get(selectedFurnaceId);
        if (plannedItems == null) {
            return;
        }

        Integer removedQty = plannedItems.remove(itemId);
        if (removedQty == null || removedQty <= 0) {
            return;
        }

        availableByItemState.merge(itemId, removedQty, Integer::sum);
        if (plannedItems.isEmpty()) {
            plannedByFurnaceState.remove(selectedFurnaceId);
        }

        renderAndPersistPlanningState();
    }

    private void renderAndPersistPlanningState() {
        PresinteringPlanningSnapshot snapshot = new PresinteringPlanningSnapshot(
                new LinkedHashMap<>(availableByItemState),
                deepCopyPlan(plannedByFurnaceState),
                new LinkedHashMap<>(itemCodeByIdState),
                null
        );
        view.applyPlanningSnapshot(snapshot);
        service.saveSnapshot(snapshot);
    }

    private Map<Integer, Map<Integer, Integer>> deepCopyPlan(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }
}
