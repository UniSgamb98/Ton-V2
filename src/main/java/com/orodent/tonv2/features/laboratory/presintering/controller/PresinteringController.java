package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringDocumentParamsService;
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

        loadData();
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
            view.setOnConfirmRequested(this::confirmAllPlannedFurnaces);
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

            int confirmedFurnaces = 0;
            int totalLinkedOrders = 0;
            int totalLots = 0;
            List<Integer> firingIds = new java.util.ArrayList<>();
            List<PresinteringDocumentParamsService.FurnaceBatchRequest> furnacePayloads = new java.util.ArrayList<>();

            for (PresinteringService.BatchConfirmationRequest furnaceRequest : furnaceRequests) {
                PresinteringService.ConfirmationResult result = service.confirmPresintering(
                        furnaceRequest.furnaceId(),
                        furnaceRequest.furnaceName(),
                        furnaceRequest.departureDate(),
                        furnaceRequest.maxTemperature(),
                        furnaceRequest.plannedItemsByItemId()
                );
                confirmedFurnaces++;
                totalLinkedOrders += result.linkedProductionOrders();
                totalLots += result.lotCount();
                firingIds.add(result.firingId());
                furnacePayloads.add(new PresinteringDocumentParamsService.FurnaceBatchRequest(
                        result.firingId(),
                        furnaceRequest.departureDate(),
                        furnaceRequest.furnaceName(),
                        furnaceRequest.maxTemperature(),
                        furnaceRequest.plannedItemsByItemId()
                ));
            }

            String documentPath = service.generateBatchDocumentIfTemplateSelected(
                    view.getTemplateSelector().getValue(),
                    furnacePayloads
            );
            if (documentPath != null) {
                documentBrowserService.openDocument(documentPath);
            }

            service.clearSnapshot();
            loadData();
            view.setFeedback(
                    "Presinterizzazione confermata su " + confirmedFurnaces + " forni."
                            + " · firing: " + firingIds
                            + " · ordini collegati: " + totalLinkedOrders
                            + " · lotti creati: " + totalLots
                            + (documentPath == null ? "" : " · documento batch aperto: " + documentPath),
                    false
            );
        } catch (Exception e) {
            view.setFeedback("Errore conferma presinterizzazione: " + e.getMessage(), true);
        }
    }
}
