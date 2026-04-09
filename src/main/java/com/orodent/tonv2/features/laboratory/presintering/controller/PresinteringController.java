package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringPlanningSnapshot;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PresinteringController {

    private final PresinteringView view;

    private final PresinteringService service;
    private final DocumentBrowserService documentBrowserService;
    private final java.util.Map<Integer, PresinteringService.FurnaceConfig> furnaceConfigById = new java.util.LinkedHashMap<>();
    private final Map<Integer, String> furnaceNameByIdState = new LinkedHashMap<>();
    private PresinteringPlanningSnapshot planningState = new PresinteringPlanningSnapshot(
            new LinkedHashMap<>(),
            new LinkedHashMap<>(),
            new LinkedHashMap<>(),
            Instant.now()
    );

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
            Map<Integer, Integer> availableByItem = new LinkedHashMap<>();
            Map<Integer, String> itemCodeById = new LinkedHashMap<>();
            for (ProductionRepository.ProducedDiskRow row : producedDisks) {
                availableByItem.put(row.itemId(), row.totalQuantity());
                itemCodeById.put(row.itemId(), row.itemCode());
            }
            planningState = new PresinteringPlanningSnapshot(
                    availableByItem,
                    new LinkedHashMap<>(),
                    itemCodeById,
                    Instant.now()
            );

            view.setProducedDisks(producedDisks);
            view.setFurnaces(furnaces);
            view.setCompositionRankingRows(compositionRanking);
            view.setFurnaceItemSuggestionRows(List.of());
            service.loadValidSnapshot(producedDisks).ifPresent(snapshot -> {
                view.applyPlanningSnapshot(snapshot);
                planningState = snapshot;
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
                    planningState.plannedByFurnace(),
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

        PresinteringService.PlanDisksResult result = service.planDisks(planningState, selectedFurnaceId, requestedByItem);
        planningState = result.state();

        if (result.insertedQuantity() <= 0) {
            view.setFeedback("Nessun disco inserito: controlla quantità disponibili.", true);
            view.clearRequestedDiskQuantities();
            return;
        }

        view.clearRequestedDiskQuantities();
        renderAndPersistPlanningState();
        view.setFeedback("Pianificati " + result.insertedQuantity() + " dischi nel " + selectedFurnaceName + ".", false);
    }

    private void removePlannedItemFromSelectedFurnace(int itemId) {
        Integer selectedFurnaceId = view.getSelectedFurnaceId();
        if (selectedFurnaceId == null) {
            return;
        }

        PresinteringPlanningSnapshot updatedState = service.removePlannedItem(planningState, selectedFurnaceId, itemId);
        if (updatedState.equals(planningState)) {
            return;
        }
        planningState = updatedState;

        renderAndPersistPlanningState();
    }

    private void renderAndPersistPlanningState() {
        view.applyPlanningSnapshot(planningState);
        service.saveSnapshot(planningState);
    }
}
