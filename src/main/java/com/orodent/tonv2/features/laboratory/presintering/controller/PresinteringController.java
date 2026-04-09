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
            view.setOnPlanningSnapshotChanged(this::onPlanningSnapshotChanged);
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

    private void onPlanningSnapshotChanged(PresinteringPlanningSnapshot snapshot) {
        syncPlanningStateFromSnapshot(snapshot);
        service.saveSnapshot(snapshot);
    }

    private void syncPlanningStateFromSnapshot(PresinteringPlanningSnapshot snapshot) {
        plannedByFurnaceState.clear();
        if (snapshot == null || snapshot.plannedByFurnace() == null) {
            return;
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : snapshot.plannedByFurnace().entrySet()) {
            plannedByFurnaceState.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
    }
}
