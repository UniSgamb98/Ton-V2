package com.orodent.tonv2.features.laboratory.presintering.service;

import com.orodent.tonv2.core.database.model.Furnace;
import com.orodent.tonv2.core.database.model.Firing;
import com.orodent.tonv2.core.database.repository.FiringRepository;
import com.orodent.tonv2.core.database.repository.FurnaceRepository;
import com.orodent.tonv2.core.database.repository.LotRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PresinteringService {
    private static final Path SNAPSHOT_PATH = Path.of(
            System.getProperty("user.home"),
            ".ton",
            "presintering-snapshot.bin"
    );

    private final ProductionRepository productionRepo;
    private final FurnaceRepository furnaceRepo;
    private final FiringRepository firingRepo;
    private final LotRepository lotRepo;
    private final TemplateEditorService templateEditorService;
    private final PresinteringDocumentParamsService documentParamsService;
    private final Connection conn;

    public PresinteringService(ProductionRepository productionRepo,
                               FurnaceRepository furnaceRepo,
                               FiringRepository firingRepo,
                               LotRepository lotRepo,
                               TemplateEditorService templateEditorService,
                               PresinteringDocumentParamsService documentParamsService,
                               Connection conn) {
        this.productionRepo = productionRepo;
        this.furnaceRepo = furnaceRepo;
        this.firingRepo = firingRepo;
        this.lotRepo = lotRepo;
        this.templateEditorService = templateEditorService;
        this.documentParamsService = documentParamsService;
        this.conn = conn;
    }

    public List<ProductionRepository.ProducedDiskRow> loadProducedDisks() {
        return productionRepo.findProducedDiskRows();
    }

    public List<Furnace> loadFurnaces() {
        return furnaceRepo.findAll();
    }

    public List<ProductionRepository.CompositionRankingRow> loadCompositionRanking() {
        return productionRepo.findCompositionRankingRows();
    }

    public List<ProductionRepository.FurnaceItemSuggestionRow> loadFurnaceItemSuggestions(String selectedFurnaceName) {
        if (selectedFurnaceName == null || selectedFurnaceName.isBlank()) {
            return List.of();
        }

        String normalizedFurnace = selectedFurnaceName.replaceFirst("^Forno\\s+", "").trim();
        return productionRepo.findFurnaceItemSuggestionRows(normalizedFurnace, selectedFurnaceName);
    }

    public Optional<PresinteringPlanningSnapshot> loadValidSnapshot(List<ProductionRepository.ProducedDiskRow> dbRows) {
        if (!Files.exists(SNAPSHOT_PATH)) {
            return Optional.empty();
        }

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(SNAPSHOT_PATH))) {
            Object object = in.readObject();
            if (!(object instanceof PresinteringPlanningSnapshot snapshot)) {
                return Optional.empty();
            }

            return isSnapshotValid(snapshot, dbRows) ? Optional.of(snapshot) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void saveSnapshot(PresinteringPlanningSnapshot snapshot) {
        try {
            Files.createDirectories(SNAPSHOT_PATH.getParent());
            PresinteringPlanningSnapshot snapshotToWrite = new PresinteringPlanningSnapshot(
                    snapshot.availableByItemId(),
                    snapshot.plannedByFurnace(),
                    snapshot.itemCodeById(),
                    Instant.now()
            );
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(SNAPSHOT_PATH))) {
                out.writeObject(snapshotToWrite);
            }
        } catch (Exception ignored) {
            // Best effort local snapshot.
        }
    }

    public void clearSnapshot() {
        try {
            Files.deleteIfExists(SNAPSHOT_PATH);
        } catch (Exception ignored) {
            // Best effort delete.
        }
    }

    public List<String> findTemplateNames() {
        return templateEditorService.getSavedTemplates().stream()
                .map(TemplateEditorService.TemplateSnapshot::name)
                .toList();
    }

    public String getLastTemplateName() {
        return templateEditorService.getLastPresinteringTemplateName();
    }

    public void setLastTemplateName(String templateName) {
        templateEditorService.setLastPresinteringTemplateName(templateName);
    }

    private List<BatchConfirmationRequest> buildBatchConfirmationRequests(Map<Integer, Map<Integer, Integer>> plannedByFurnace,
                                                                          Map<Integer, String> furnaceNameById,
                                                                          Map<Integer, FurnaceConfig> furnaceConfigById) {
        if (plannedByFurnace == null || plannedByFurnace.isEmpty()) {
            throw new IllegalArgumentException("Nessun forno con nuovi item da confermare.");
        }

        List<BatchConfirmationRequest> requests = new java.util.ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> furnaceEntry : plannedByFurnace.entrySet()) {
            int furnaceId = furnaceEntry.getKey();
            Map<Integer, Integer> plannedItems = furnaceEntry.getValue();
            if (plannedItems == null || plannedItems.isEmpty()) {
                continue;
            }

            FurnaceConfig config = furnaceConfigById == null ? null : furnaceConfigById.get(furnaceId);
            String furnaceName = furnaceNameById == null
                    ? "Forno " + furnaceId
                    : furnaceNameById.getOrDefault(furnaceId, "Forno " + furnaceId);

            if (config == null || config.maxTemperature() == null || config.maxTemperature() <= 0) {
                throw new IllegalArgumentException("Inserisci la max temperature per " + furnaceName + " prima di confermare tutti i forni.");
            }
            if (config.departureDate() == null) {
                throw new IllegalArgumentException("Inserisci la data di partenza per " + furnaceName + " prima di confermare tutti i forni.");
            }

            requests.add(new BatchConfirmationRequest(
                    furnaceId,
                    furnaceName,
                    config.maxTemperature(),
                    config.departureDate(),
                    new LinkedHashMap<>(plannedItems)
            ));
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Nessun forno con nuovi item da confermare.");
        }
        return requests;
    }

    private ConfirmationResult confirmPresinteringInCurrentTransaction(int furnaceId,
                                                                       String furnaceName,
                                                                       LocalDate firingDate,
                                                                       Integer maxTemperature,
                                                                       Map<Integer, Integer> plannedItemsByItemId) {
        if (furnaceName == null || furnaceName.isBlank()) {
            throw new IllegalArgumentException("Forno non valido.");
        }
        if (firingDate == null) {
            throw new IllegalArgumentException("Data partenza obbligatoria.");
        }
        if (maxTemperature == null || maxTemperature <= 0) {
            throw new IllegalArgumentException("Temperatura massima non valida.");
        }
        if (plannedItemsByItemId == null || plannedItemsByItemId.isEmpty()) {
            throw new IllegalArgumentException("Nessun item pianificato da confermare.");
        }

        Firing firing = firingRepo.insert(firingDate, furnaceName, maxTemperature, "Presinterizzazione forno id=" + furnaceId);
        Set<Integer> productionOrdersToLink = new LinkedHashSet<>();

        for (Map.Entry<Integer, Integer> plannedEntry : plannedItemsByItemId.entrySet()) {
            int itemId = plannedEntry.getKey();
            int requestedQty = plannedEntry.getValue() == null ? 0 : plannedEntry.getValue();
            if (requestedQty <= 0) {
                continue;
            }

            List<ProductionRepository.OpenProductionOrderLineRow> openOrderLines = productionRepo.findOpenProductionOrderLinesByItem(itemId);
            int coveredQty = 0;
            for (ProductionRepository.OpenProductionOrderLineRow orderLine : openOrderLines) {
                if (coveredQty >= requestedQty) {
                    break;
                }
                productionOrdersToLink.add(orderLine.productionOrderId());
                coveredQty += orderLine.quantity();
            }

            if (coveredQty < requestedQty) {
                throw new IllegalStateException("Quantità pianificata non coerente per item " + itemId + ".");
            }

            String lotCode = buildRandomLotCode(firing.id(), itemId);
            lotRepo.insert(lotCode, firing.id());
        }

        for (Integer productionOrderId : productionOrdersToLink) {
            productionRepo.insertProductionOrderFiring(productionOrderId, firing.id());
        }

        return new ConfirmationResult(firing.id(), productionOrdersToLink.size(), plannedItemsByItemId.size());
    }

    public String generateDocumentIfTemplateSelected(String selectedTemplateName,
                                                     ConfirmationResult confirmationResult,
                                                     String furnaceName,
                                                     LocalDate firingDate,
                                                     Integer maxTemperature,
                                                     Map<Integer, Integer> plannedItemsByItemId) {
        if (selectedTemplateName == null || selectedTemplateName.isBlank()) {
            return null;
        }

        String templateText = templateEditorService.getTemplateContentByName(selectedTemplateName);
        if (templateText == null || templateText.isBlank()) {
            throw new IllegalArgumentException("Template selezionato non trovato: " + selectedTemplateName);
        }

        templateEditorService.setLastPresinteringTemplateName(selectedTemplateName);
        Map<String, Object> params = documentParamsService.buildParams(
                new PresinteringDocumentParamsService.ParamsRequest(
                        confirmationResult.firingId(),
                        firingDate,
                        furnaceName,
                        maxTemperature,
                        plannedItemsByItemId
                )
        );

        String payloadJson = templateEditorService.toJson(params);
        TemplateEditorService.PreviewResult renderResult = templateEditorService.previewTemplate(templateText, payloadJson);
        if (!renderResult.success()) {
            throw new IllegalArgumentException("Errore generazione documento: " + renderResult.htmlOrError());
        }

        try {
            Path outputFile = Files.createTempFile("ton-presintering-document-", ".html");
            Files.writeString(outputFile, renderResult.htmlOrError());
            return outputFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Documento generato ma non salvabile su file temporaneo.");
        }
    }

    public String generateBatchDocumentIfTemplateSelected(String selectedTemplateName,
                                                          List<PresinteringDocumentParamsService.FurnaceBatchRequest> furnaces) {
        if (selectedTemplateName == null || selectedTemplateName.isBlank()) {
            return null;
        }

        String templateText = templateEditorService.getTemplateContentByName(selectedTemplateName);
        if (templateText == null || templateText.isBlank()) {
            throw new IllegalArgumentException("Template selezionato non trovato: " + selectedTemplateName);
        }

        templateEditorService.setLastPresinteringTemplateName(selectedTemplateName);
        Map<String, Object> params = documentParamsService.buildBatchParams(furnaces);

        String payloadJson = templateEditorService.toJson(params);
        TemplateEditorService.PreviewResult renderResult = templateEditorService.previewTemplate(templateText, payloadJson);
        if (!renderResult.success()) {
            throw new IllegalArgumentException("Errore generazione documento: " + renderResult.htmlOrError());
        }

        try {
            Path outputFile = Files.createTempFile("ton-presintering-batch-document-", ".html");
            Files.writeString(outputFile, renderResult.htmlOrError());
            return outputFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Documento batch generato ma non salvabile su file temporaneo.");
        }
    }

    public ConfirmBatchResult confirmBatch(ConfirmBatchCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Nessun forno con nuovi item da confermare.");
        }
        List<BatchConfirmationRequest> requests = buildBatchConfirmationRequests(
                command.plannedByFurnace(),
                command.furnaceNameById(),
                command.furnaceConfigById()
        );
        validateBatchDemandAgainstOpenOrders(requests);

        boolean previousAutoCommit;
        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile iniziare la transazione di conferma presinterizzazione.", e);
        }

        try {
            int confirmedFurnaces = 0;
            int totalLinkedOrders = 0;
            int totalLots = 0;
            List<Integer> firingIds = new java.util.ArrayList<>();
            List<PresinteringDocumentParamsService.FurnaceBatchRequest> furnacePayloads = new java.util.ArrayList<>();

            for (BatchConfirmationRequest furnaceRequest : requests) {
                ConfirmationResult result;
                try {
                    result = confirmPresinteringInCurrentTransaction(
                            furnaceRequest.furnaceId(),
                            furnaceRequest.furnaceName(),
                            furnaceRequest.departureDate(),
                            furnaceRequest.maxTemperature(),
                            furnaceRequest.plannedItemsByItemId()
                    );
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Errore nel " + furnaceRequest.furnaceName() + ": " + extractMostSpecificMessage(e),
                            e
                    );
                }

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

            conn.commit();
            conn.setAutoCommit(previousAutoCommit);

            String documentPath = generateBatchDocumentIfTemplateSelected(
                    command.selectedTemplateName(),
                    furnacePayloads
            );

            return new ConfirmBatchResult(
                    confirmedFurnaces,
                    firingIds,
                    totalLinkedOrders,
                    totalLots,
                    documentPath
            );
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
                // best effort rollback
            }
            try {
                conn.setAutoCommit(previousAutoCommit);
            } catch (Exception ignored) {
                // ignore
            }
            throw new RuntimeException("Errore durante conferma presinterizzazione batch: " + extractMostSpecificMessage(e), e);
        }
    }

    private String extractMostSpecificMessage(Throwable throwable) {
        if (throwable == null) {
            return "Errore sconosciuto.";
        }
        Throwable cursor = throwable;
        String message = null;
        while (cursor != null) {
            if (cursor.getMessage() != null && !cursor.getMessage().isBlank()) {
                message = cursor.getMessage();
            }
            cursor = cursor.getCause();
        }
        return message == null ? "Errore sconosciuto." : message;
    }

    private void validateBatchDemandAgainstOpenOrders(List<BatchConfirmationRequest> requests) {
        Map<Integer, Integer> requestedByItem = new LinkedHashMap<>();
        for (BatchConfirmationRequest request : requests) {
            for (Map.Entry<Integer, Integer> plannedEntry : request.plannedItemsByItemId().entrySet()) {
                int itemId = plannedEntry.getKey();
                int quantity = plannedEntry.getValue() == null ? 0 : Math.max(0, plannedEntry.getValue());
                if (quantity <= 0) {
                    continue;
                }
                requestedByItem.merge(itemId, quantity, Integer::sum);
            }
        }

        for (Map.Entry<Integer, Integer> requestedEntry : requestedByItem.entrySet()) {
            int itemId = requestedEntry.getKey();
            int requested = requestedEntry.getValue();
            int available = productionRepo.findOpenProductionOrderLinesByItem(itemId).stream()
                    .mapToInt(ProductionRepository.OpenProductionOrderLineRow::quantity)
                    .sum();

            if (requested > available) {
                throw new IllegalStateException(
                        "Quantità pianificata non coerente per item " + itemId
                                + ": pianificati " + requested
                                + ", copertura ordini aperti " + available
                                + ". Riduci la quantità o aggiorna gli ordini."
                );
            }
        }
    }

    public PlanDisksResult planDisks(PresinteringPlanningSnapshot currentState,
                                     int furnaceId,
                                     Map<Integer, Integer> requestedByItem) {
        if (currentState == null) {
            throw new IllegalArgumentException("Stato pianificazione non disponibile.");
        }
        if (furnaceId <= 0) {
            throw new IllegalArgumentException("Forno non valido.");
        }
        if (requestedByItem == null || requestedByItem.isEmpty()) {
            return new PlanDisksResult(currentState, 0);
        }

        Map<Integer, Integer> availableByItem = new LinkedHashMap<>(currentState.availableByItemId());
        Map<Integer, Map<Integer, Integer>> plannedByFurnace = deepCopyPlan(currentState.plannedByFurnace());
        Map<Integer, Integer> targetPlan = plannedByFurnace.computeIfAbsent(furnaceId, ignored -> new LinkedHashMap<>());

        int inserted = 0;
        for (Map.Entry<Integer, Integer> entry : requestedByItem.entrySet()) {
            int itemId = entry.getKey();
            int requested = entry.getValue() == null ? 0 : entry.getValue();
            int available = availableByItem.getOrDefault(itemId, 0);
            int toInsert = Math.min(requested, available);
            if (toInsert <= 0) {
                continue;
            }
            availableByItem.put(itemId, available - toInsert);
            targetPlan.merge(itemId, toInsert, Integer::sum);
            inserted += toInsert;
        }

        PresinteringPlanningSnapshot updatedState = new PresinteringPlanningSnapshot(
                availableByItem,
                plannedByFurnace,
                new LinkedHashMap<>(currentState.itemCodeById()),
                null
        );
        return new PlanDisksResult(updatedState, inserted);
    }

    public PresinteringPlanningSnapshot removePlannedItem(PresinteringPlanningSnapshot currentState,
                                                          int furnaceId,
                                                          int itemId) {
        if (currentState == null) {
            throw new IllegalArgumentException("Stato pianificazione non disponibile.");
        }
        if (furnaceId <= 0 || itemId <= 0) {
            return currentState;
        }

        Map<Integer, Integer> availableByItem = new LinkedHashMap<>(currentState.availableByItemId());
        Map<Integer, Map<Integer, Integer>> plannedByFurnace = deepCopyPlan(currentState.plannedByFurnace());
        Map<Integer, Integer> plannedItems = plannedByFurnace.get(furnaceId);
        if (plannedItems == null) {
            return currentState;
        }

        Integer removedQty = plannedItems.remove(itemId);
        if (removedQty == null || removedQty <= 0) {
            return currentState;
        }

        availableByItem.merge(itemId, removedQty, Integer::sum);
        if (plannedItems.isEmpty()) {
            plannedByFurnace.remove(furnaceId);
        }

        return new PresinteringPlanningSnapshot(
                availableByItem,
                plannedByFurnace,
                new LinkedHashMap<>(currentState.itemCodeById()),
                null
        );
    }

    private String buildRandomLotCode(int firingId, int itemId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "LOT-F" + firingId + "-I" + itemId + "-" + suffix;
    }

    private Map<Integer, Map<Integer, Integer>> deepCopyPlan(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    private boolean isSnapshotValid(PresinteringPlanningSnapshot snapshot,
                                    List<ProductionRepository.ProducedDiskRow> dbRows) {
        Map<Integer, Integer> dbTotals = new LinkedHashMap<>();
        for (ProductionRepository.ProducedDiskRow row : dbRows) {
            dbTotals.put(row.itemId(), row.totalQuantity());
        }

        for (Map.Entry<Integer, Integer> dbEntry : dbTotals.entrySet()) {
            int itemId = dbEntry.getKey();
            int dbQty = dbEntry.getValue();
            int availableQty = snapshot.availableByItemId().getOrDefault(itemId, 0);
            int plannedQty = snapshot.plannedByFurnace().values().stream()
                    .mapToInt(byItem -> byItem.getOrDefault(itemId, 0))
                    .sum();
            if (dbQty != availableQty + plannedQty) {
                return false;
            }
        }

        for (Map.Entry<Integer, Integer> entry : snapshot.availableByItemId().entrySet()) {
            if (!dbTotals.containsKey(entry.getKey()) && entry.getValue() > 0) {
                return false;
            }
        }
        return true;
    }

    public record ConfirmationResult(int firingId, int linkedProductionOrders, int lotCount) {
    }

    public record FurnaceConfig(Integer maxTemperature, LocalDate departureDate) {
    }

    public record BatchConfirmationRequest(int furnaceId,
                                           String furnaceName,
                                           int maxTemperature,
                                           LocalDate departureDate,
                                           Map<Integer, Integer> plannedItemsByItemId) {
    }

    public record ConfirmBatchCommand(Map<Integer, Map<Integer, Integer>> plannedByFurnace,
                                      Map<Integer, String> furnaceNameById,
                                      Map<Integer, FurnaceConfig> furnaceConfigById,
                                      String selectedTemplateName) {
    }

    public record ConfirmBatchResult(int confirmedFurnaces,
                                     List<Integer> firingIds,
                                     int totalLinkedOrders,
                                     int totalLots,
                                     String documentPath) {
    }

    public record PlanDisksResult(PresinteringPlanningSnapshot state, int insertedQuantity) {
    }
}
