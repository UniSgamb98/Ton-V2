package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;

import java.util.List;

public class PresinteringController {

    private final PresinteringView view;

    private final PresinteringService service;
    private final DocumentBrowserService documentBrowserService;

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
    }

    private void loadData() {
        try {
            List<ProductionRepository.ProducedDiskRow> producedDisks = service.loadProducedDisks();
            List<Furnace> furnaces = service.loadFurnaces();
            List<ProductionRepository.CompositionRankingRow> compositionRanking = service.loadCompositionRanking();

            view.setProducedDisks(producedDisks);
            view.setFurnaces(furnaces);
            view.setCompositionRankingRows(compositionRanking);
            view.setFurnaceItemSuggestionRows(List.of());
            service.loadValidSnapshot(producedDisks).ifPresent(view::applyPlanningSnapshot);
            view.setOnFurnaceSelectionChanged(selectedFurnace -> {
                List<ProductionRepository.FurnaceItemSuggestionRow> suggestions = service.loadFurnaceItemSuggestions(selectedFurnace);
                view.setFurnaceItemSuggestionRows(suggestions);
            });
            view.setOnPlanningSnapshotChanged(service::saveSnapshot);
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
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                service.setLastTemplateName(newValue)
        );
    }

    private void confirmAllPlannedFurnaces() {
        try {
            java.util.Map<Integer, PresinteringService.FurnaceConfig> furnaceConfig = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<Integer, PresinteringView.FurnaceConfigInput> entry : view.getFurnaceConfigSnapshot().entrySet()) {
                PresinteringView.FurnaceConfigInput config = entry.getValue();
                furnaceConfig.put(entry.getKey(), new PresinteringService.FurnaceConfig(config.maxTemperature(), config.departureDate()));
            }

            List<PresinteringService.BatchConfirmationRequest> furnaceRequests = service.buildBatchConfirmationRequests(
                    view.getPlannedByFurnaceSnapshot(),
                    view.getFurnaceNameByIdSnapshot(),
                    furnaceConfig
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
}
