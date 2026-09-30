package com.orodent.tonv2.features.laboratory.presintering.controller;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.document.service.DocumentBrowserService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringPlanningSnapshot;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringReadService;
import com.orodent.tonv2.features.laboratory.presintering.service.PresinteringService;
import com.orodent.tonv2.features.laboratory.presintering.view.PresinteringView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import javafx.util.Duration;

public class PresinteringController {

    private final PresinteringView view;

    private final PresinteringService service;
    private final PresinteringReadService readService;
    private final DocumentBrowserService documentBrowserService;
    private final DebouncedTaskRunner<InitialPageData> initialDataLoader;
    private final DebouncedTaskRunner<List<ProductionRepository.FurnaceItemSuggestionRow>> suggestionsLoader;
    private final DebouncedTaskRunner<PresinteringService.ConfirmBatchResult> confirmationLoader;
    private final java.util.Map<Integer, PresinteringService.FurnaceConfig> furnaceConfigById = new java.util.LinkedHashMap<>();
    private final Map<Integer, String> furnaceNameByIdState = new LinkedHashMap<>();
    private final Map<Integer, String> productNameByItemIdState = new LinkedHashMap<>();
    private Integer latestFiringIdState;
    private String feedbackAfterReload;
    private boolean feedbackAfterReloadIsError;
    private PresinteringPlanningSnapshot planningState = new PresinteringPlanningSnapshot(
            new LinkedHashMap<>(),
            new LinkedHashMap<>(),
            new LinkedHashMap<>(),
            Instant.now()
    );

    public PresinteringController(PresinteringView view,
                                  PresinteringService service,
                                  PresinteringReadService readService,
                                  DocumentBrowserService documentBrowserService,
                                  Executor backgroundExecutor) {
        this.view = view;
        this.service = service;
        this.readService = readService;
        this.documentBrowserService = documentBrowserService;
        this.initialDataLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.suggestionsLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        this.confirmationLoader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);

        setupActions();
    }

    private void setupActions() {
        view.getConfirmPresinteringButton().setOnAction(e -> confirmAllPlannedFurnaces());
        view.getSelectedFurnaceMaxTemperatureField().textProperty().addListener((obs, oldValue, newValue) -> syncSelectedFurnaceConfigFromView());
        view.getSelectedFurnaceDepartureDatePicker().valueProperty().addListener((obs, oldValue, newValue) -> syncSelectedFurnaceConfigFromView());
        view.getSelectedFurnaceLotCodeField().textProperty().addListener((obs, oldValue, newValue) -> syncSelectedFurnaceConfigFromView());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                service.setLastTemplateName(newValue)
        );
        view.setOnInsertDisksRequested(this::insertDisksIntoSelectedFurnace);
        view.setOnRemovePlannedItemRequested(this::removePlannedItemFromSelectedFurnace);
    }

    public void loadInitialData() {
        suggestionsLoader.cancel();
        view.setOnFurnaceSelectionChanged(null);
        initialDataLoader.runNow(
                () -> new InitialPageData(readService.loadInitialData(), service.loadLocalPlanState()),
                view::showInitialLoading,
                this::showInitialData,
                error -> {
                    clearLoadedData();
                    view.showLoadError("Errore durante il caricamento dati presinterizza.");
                }
        );
    }

    private void showInitialData(InitialPageData pageData) {
        PresinteringReadService.InitialData data = pageData.data();
        List<ProductionRepository.ProducedDiskRow> producedDisks = data.producedDisks();
        List<Furnace> furnaces = data.furnaces();
        latestFiringIdState = data.latestFiringId();
        furnaceConfigById.clear();
        furnaceNameByIdState.clear();
        for (Furnace furnace : furnaces) {
            String displayNumber = furnace.number() == null || furnace.number().isBlank()
                    ? String.valueOf(furnace.id())
                    : furnace.number();
            furnaceNameByIdState.put(furnace.id(), "Forno " + displayNumber);
        }
        Map<Integer, Integer> availableByItem = new LinkedHashMap<>();
        Map<Integer, String> itemCodeById = new LinkedHashMap<>();
        productNameByItemIdState.clear();
        for (ProductionRepository.ProducedDiskRow row : producedDisks) {
            availableByItem.put(row.itemId(), row.totalQuantity());
            itemCodeById.put(row.itemId(), row.itemCode());
            productNameByItemIdState.put(row.itemId(), row.productName());
        }
        planningState = new PresinteringPlanningSnapshot(
                availableByItem,
                new LinkedHashMap<>(),
                itemCodeById,
                Instant.now()
        );

        view.setProducedDisks(producedDisks);
        view.setFurnaces(furnaces);
        view.setCompositionRankingRows(data.compositionRanking());
        view.setFurnaceItemSuggestionRows(List.of());
        restoreLocalPlanIfStillValid(pageData.localPlanState(), latestFiringIdState);
        view.renderPlanning(planningState, productNameByItemIdState);
        view.setOnFurnaceSelectionChanged(this::onFurnaceSelected);
        view.setTemplateNames(data.templateNames(), data.selectedTemplateName());
        view.showLoadSuccess();
        if (feedbackAfterReload != null) {
            view.setFeedback(feedbackAfterReload, feedbackAfterReloadIsError);
            feedbackAfterReload = null;
            feedbackAfterReloadIsError = false;
        }
    }

    private void clearLoadedData() {
        productNameByItemIdState.clear();
        view.setProducedDisks(List.of());
        view.setFurnaces(List.of());
        view.setCompositionRankingRows(List.of());
        view.setFurnaceItemSuggestionRows(List.of());
    }

    private void onFurnaceSelected(String selectedFurnace) {
        Integer selectedFurnaceId = view.getSelectedFurnaceId();
        PresinteringService.FurnaceConfig config = selectedFurnaceId == null
                ? null
                : furnaceConfigById.get(selectedFurnaceId);
        view.setSelectedFurnaceParameters(
                config == null ? null : config.maxTemperature(),
                config == null ? null : config.departureDate(),
                config == null ? null : config.lotCode()
        );
        view.renderPlanning(planningState, productNameByItemIdState);

        suggestionsLoader.runNow(
                () -> readService.loadFurnaceItemSuggestions(selectedFurnace),
                view::showSuggestionsLoading,
                suggestions -> {
                    view.setFurnaceItemSuggestionRows(suggestions);
                    view.showLoadSuccess();
                },
                error -> view.showLoadError("Errore durante il caricamento dei suggerimenti forno.")
        );
    }

    private void confirmAllPlannedFurnaces() {
        PresinteringService.ConfirmBatchCommand command = new PresinteringService.ConfirmBatchCommand(
                copyPlan(planningState.plannedByFurnace()),
                new LinkedHashMap<>(furnaceNameByIdState),
                new LinkedHashMap<>(furnaceConfigById),
                view.getTemplateSelector().getValue()
        );

        confirmationLoader.runNow(
                () -> service.confirmBatch(command),
                view::showConfirmationSaving,
                this::showConfirmationSuccess,
                error -> view.showLoadError("Errore conferma presinterizzazione: " + error.getMessage())
        );
    }

    private void showConfirmationSuccess(PresinteringService.ConfirmBatchResult result) {
        if (result.documentPath() != null) {
            documentBrowserService.openDocument(result.documentPath());
        }
        service.clearLocalPlanState();

        String documentMessage = result.documentError() != null
                ? " · conferma salvata, documento non generato: " + result.documentError()
                : (result.documentPath() == null ? "" : " · documento batch aperto: " + result.documentPath());
        feedbackAfterReload = "Presinterizzazione confermata su " + result.confirmedFurnaces() + " forni."
                + " · firing: " + result.firingIds()
                + " · ordini collegati: " + result.totalLinkedOrders()
                + " · lotti creati: " + result.totalLots()
                + documentMessage;
        feedbackAfterReloadIsError = result.documentError() != null;
        loadInitialData();
    }

    private Map<Integer, Map<Integer, Integer>> copyPlan(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        source.forEach((furnaceId, items) -> copy.put(furnaceId, new LinkedHashMap<>(items)));
        return copy;
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
        String lotCode = view.getSelectedFurnaceLotCodeField().getText();
        furnaceConfigById.put(
                selectedFurnaceId,
                new PresinteringService.FurnaceConfig(
                        maxTemperature,
                        view.getSelectedFurnaceDepartureDatePicker().getValue(),
                        lotCode == null ? null : lotCode.trim()
                )
        );
        service.saveLocalPlanState(new PresinteringService.LocalPlanState(
                planningState.plannedByFurnace(),
                furnaceConfigById,
                latestFiringIdState,
                Instant.now()
        ));
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
        view.renderPlanning(planningState, productNameByItemIdState);
        service.saveLocalPlanState(new PresinteringService.LocalPlanState(
                planningState.plannedByFurnace(),
                furnaceConfigById,
                latestFiringIdState,
                Instant.now()
        ));
    }

    private void restoreLocalPlanIfStillValid(Optional<PresinteringService.LocalPlanState> savedState,
                                              Integer latestFiringId) {
        PresinteringService.LocalPlanState localState = savedState.orElse(null);
        if (localState == null) {
            return;
        }

        if (!java.util.Objects.equals(localState.lastKnownFiringId(), latestFiringId)) {
            service.clearLocalPlanState();
            return;
        }

        Map<Integer, Integer> restoredAvailableByItem = new LinkedHashMap<>(planningState.availableByItemId());
        Map<Integer, Map<Integer, Integer>> restoredPlanByFurnace = new LinkedHashMap<>();
        boolean trimmed = false;

        for (Map.Entry<Integer, Map<Integer, Integer>> furnaceEntry : localState.plannedByFurnace().entrySet()) {
            Integer furnaceId = furnaceEntry.getKey();
            if (!furnaceNameByIdState.containsKey(furnaceId)) {
                trimmed = true;
                continue;
            }

            Map<Integer, Integer> restoredByItem = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> itemEntry : furnaceEntry.getValue().entrySet()) {
                Integer itemId = itemEntry.getKey();
                int requestedQty = itemEntry.getValue() == null ? 0 : itemEntry.getValue();
                if (requestedQty <= 0) {
                    trimmed = true;
                    continue;
                }
                int availableQty = restoredAvailableByItem.getOrDefault(itemId, 0);
                int acceptedQty = Math.min(requestedQty, availableQty);
                if (acceptedQty <= 0) {
                    trimmed = true;
                    continue;
                }
                restoredByItem.put(itemId, acceptedQty);
                restoredAvailableByItem.put(itemId, availableQty - acceptedQty);
                if (acceptedQty != requestedQty) {
                    trimmed = true;
                }
            }

            if (!restoredByItem.isEmpty()) {
                restoredPlanByFurnace.put(furnaceId, restoredByItem);
            } else {
                trimmed = true;
            }
        }

        Map<Integer, PresinteringService.FurnaceConfig> restoredConfigByFurnace = new LinkedHashMap<>();
        for (Map.Entry<Integer, PresinteringService.FurnaceConfig> configEntry : localState.furnaceConfigById().entrySet()) {
            Integer furnaceId = configEntry.getKey();
            if (!furnaceNameByIdState.containsKey(furnaceId)) {
                trimmed = true;
                continue;
            }
            PresinteringService.FurnaceConfig config = configEntry.getValue();
            if (config == null) {
                trimmed = true;
                continue;
            }
            LocalDate departureDate = config.departureDate();
            Integer maxTemperature = config.maxTemperature();
            restoredConfigByFurnace.put(furnaceId, new PresinteringService.FurnaceConfig(maxTemperature, departureDate, config.lotCode()));
        }

        if (restoredPlanByFurnace.isEmpty()) {
            service.clearLocalPlanState();
            return;
        }

        planningState = new PresinteringPlanningSnapshot(
                restoredAvailableByItem,
                restoredPlanByFurnace,
                planningState.itemCodeById(),
                Instant.now()
        );
        furnaceConfigById.clear();
        furnaceConfigById.putAll(restoredConfigByFurnace);

        if (trimmed) {
            service.saveLocalPlanState(new PresinteringService.LocalPlanState(
                    restoredPlanByFurnace,
                    restoredConfigByFurnace,
                    latestFiringId,
                    Instant.now()
            ));
        }
    }

    public void dispose() {
        initialDataLoader.cancel();
        suggestionsLoader.cancel();
        confirmationLoader.cancel();
    }

    private record InitialPageData(PresinteringReadService.InitialData data,
                                   Optional<PresinteringService.LocalPlanState> localPlanState) {
    }
}
