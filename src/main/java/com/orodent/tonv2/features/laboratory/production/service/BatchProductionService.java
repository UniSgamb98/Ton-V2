package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.documents.template.service.TemplatePresetCodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BatchProductionService {

    private final ItemRepository itemRepo;
    private final LineRepository lineRepo;
    private final CompositionRepository compositionRepo;
    private final ProductRepository productRepo;
    private final ProductionRepository productionRepo;
    private final TemplateEditorService templateEditorService;
    private final BatchProductionDocumentParamsService documentParamsService;

    public BatchProductionService(ItemRepository itemRepo,
                                  LineRepository lineRepo,
                                  CompositionRepository compositionRepo,
                                  ProductRepository productRepo,
                                  ProductionRepository productionRepo,
                                  TemplateEditorService templateEditorService,
                                  BatchProductionDocumentParamsService documentParamsService) {
        this.itemRepo = itemRepo;
        this.lineRepo = lineRepo;
        this.compositionRepo = compositionRepo;
        this.productRepo = productRepo;
        this.productionRepo = productionRepo;
        this.templateEditorService = templateEditorService;
        this.documentParamsService = documentParamsService;
    }

    public List<Line> findAllLines() {
        Map<String, Line> byName = new LinkedHashMap<>();
        for (Line line : lineRepo.findAll()) {
            byName.putIfAbsent(line.name(), line);
        }
        return new ArrayList<>(byName.values());
    }

    public List<Product> findProductsByLineName(String lineName) {
        if (lineName == null || lineName.isBlank()) {
            return List.of();
        }

        Map<Integer, Product> productsById = new LinkedHashMap<>();
        for (Line line : lineRepo.findAll()) {
            if (!lineName.equals(line.name())) {
                continue;
            }

            Product product = productRepo.findById(line.productId());
            if (product != null) {
                productsById.putIfAbsent(product.id(), product);
            }
        }

        return new ArrayList<>(productsById.values());
    }

    public Product findProductById(int productId) {
        return productRepo.findById(productId);
    }

    public List<Item> findItemsByProduct(int productId) {
        return itemRepo.findByProduct(productId);
    }

    public BatchResult produce(Line line, List<ProductionRequestLine> requestLines, String notes) {
        ProductionPlan plan = buildPlan(requestLines, line);
        PersistResult persistResult = persistPlan(plan, LocalDate.now(), notes);
        return new BatchResult(plan, persistResult);
    }

    public String generateDocumentIfTemplateSelected(String selectedTemplateName,
                                                     Line line,
                                                     String notes,
                                                     ProductionPlan plan) {
        if (selectedTemplateName == null || selectedTemplateName.isBlank()) {
            return null;
        }

        String templateText = templateEditorService.getTemplateContentByName(selectedTemplateName);
        if (templateText == null || templateText.isBlank()) {
            throw new IllegalArgumentException("Template selezionato non trovato: " + selectedTemplateName);
        }

        templateEditorService.setLastBatchTemplateName(selectedTemplateName);
        Map<String, Object> params = documentParamsService.buildParams(
                BatchProductionDocumentParamsService.ParamsRequest.real(
                        line,
                        notes,
                        plan.lines()
                )
        );

        String payloadJson = templateEditorService.toJson(params);
        TemplateEditorService.PreviewResult renderResult = templateEditorService.previewTemplate(templateText, payloadJson);
        if (!renderResult.success()) {
            throw new IllegalArgumentException("Errore generazione documento: " + renderResult.htmlOrError());
        }

        try {
            Path outputFile = Files.createTempFile("ton-batch-document-", ".html");
            Files.writeString(outputFile, renderResult.htmlOrError());
            return outputFile.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Documento generato ma non salvabile su file temporaneo.");
        }
    }

    public List<String> findTemplateNames() {
        return templateEditorService.getSavedTemplates().stream()
                .filter(template -> TemplatePresetCodes.PRODUCTION.equals(template.presetCode()))
                .map(TemplateEditorService.TemplateSnapshot::name)
                .toList();
    }

    public String getLastTemplateName() {
        return templateEditorService.getLastBatchTemplateName();
    }

    public void setLastTemplateName(String templateName) {
        templateEditorService.setLastBatchTemplateName(templateName);
    }

    private ProductionPlan buildPlan(List<ProductionRequestLine> requestLines,
                                     Line line) {
        if (line == null) {
            throw new IllegalArgumentException("Linea di produzione non selezionata.");
        }
        if (requestLines == null || requestLines.isEmpty()) {
            throw new IllegalArgumentException("Aggiungi almeno una riga da produrre.");
        }

        Map<Integer, Integer> mergedQtyByItem = new LinkedHashMap<>();

        for (ProductionRequestLine reqLine : requestLines) {
            if (reqLine.itemId() <= 0) {
                throw new IllegalArgumentException("Item non valido in una delle righe.");
            }
            if (reqLine.quantity() < 0) {
                throw new IllegalArgumentException("La quantità non può essere negativa.");
            }
            if (reqLine.quantity() == 0) {
                continue;
            }
            mergedQtyByItem.merge(reqLine.itemId(), reqLine.quantity(), Integer::sum);
        }

        if (mergedQtyByItem.isEmpty()) {
            throw new IllegalArgumentException("Inserisci almeno una quantità maggiore di zero per produrre.");
        }

        List<ProductionPlanLine> planLines = new ArrayList<>();
        Integer compositionId = null;
        Integer blankModelId = null;

        for (Map.Entry<Integer, Integer> entry : mergedQtyByItem.entrySet()) {
            Item item = itemRepo.findById(entry.getKey());
            if (item == null) {
                throw new IllegalArgumentException("Item con id " + entry.getKey() + " non trovato.");
            }
            boolean itemBelongsToSelectedLine = lineRepo.findByProductId(item.productId()).stream()
                    .anyMatch(productLine -> productLine.name().equals(line.name()));
            if (!itemBelongsToSelectedLine) {
                throw new IllegalArgumentException("L'item " + item.code() + " non appartiene alla linea selezionata.");
            }

            Optional<Integer> activeCompositionId = compositionRepo.findActiveCompositionId(item.productId());
            if (activeCompositionId.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nessuna composizione attiva trovata per il prodotto dell'item " + item.code() + "."
                );
            }

            int currentCompositionId = activeCompositionId.get();
            if (compositionId == null) {
                compositionId = currentCompositionId;
                blankModelId = item.blankModelId();
            } else if (compositionId != currentCompositionId || blankModelId != item.blankModelId()) {
                throw new IllegalArgumentException("Gli item selezionati non sono coerenti per composizione/modello.");
            }

            planLines.add(new ProductionPlanLine(item, entry.getValue(), currentCompositionId));
        }

        return new ProductionPlan(line, compositionId, blankModelId, planLines);
    }

    private PersistResult persistPlan(ProductionPlan plan,
                                      LocalDate productionDate,
                                      String notes) {
        int orderId = productionRepo.insertProductionOrder(
                plan.line().productId(),
                plan.compositionId(),
                plan.blankModelId(),
                productionDate,
                notes
        );

        int totalQty = 0;
        for (ProductionPlanLine line : plan.lines()) {
            productionRepo.insertProductionOrderLine(orderId, line.item().id(), line.quantity());
            totalQty += line.quantity();
        }

        return new PersistResult(orderId, totalQty);
    }

    public record ProductionRequestLine(int itemId, int quantity) {}

    public record ProductionPlan(Line line, int compositionId, int blankModelId, List<ProductionPlanLine> lines) {}

    public record ProductionPlanLine(Item item, int quantity, int compositionId) {}

    public record PersistResult(int productionOrderId, int totalQuantity) {}

    public record BatchResult(ProductionPlan plan, PersistResult persistResult) {}
}
