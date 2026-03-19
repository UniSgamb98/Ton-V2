package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.core.documents.template.DocumentGenerationService;
import com.orodent.tonv2.core.documents.template.DocumentTemplateService;
import com.orodent.tonv2.core.documents.template.TemplateStorageService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BatchProductionController {

    private final BatchProductionView view;
    private final ItemRepository itemRepo;
    private final LineRepository lineRepo;
    private final CompositionRepository compositionRepo;
    private final ProductRepository productRepo;
    private final ProductionRepository productionRepo;
    private final BatchProductionService service;
    private final TemplateStorageService templateStorageService;
    private final DocumentGenerationService documentGenerationService;

    private List<Item> filteredItems = List.of();

    public BatchProductionController(BatchProductionView view,
                                     ItemRepository itemRepo,
                                     LineRepository lineRepo,
                                     CompositionRepository compositionRepo,
                                     ProductRepository productRepo,
                                     ProductionRepository productionRepo,
                                     BatchProductionService service,
                                     TemplateStorageService templateStorageService,
                                     List<Item> preselectedItems) {
        this.view = view;
        this.itemRepo = itemRepo;
        this.lineRepo = lineRepo;
        this.compositionRepo = compositionRepo;
        this.productRepo = productRepo;
        this.productionRepo = productionRepo;
        this.service = service;
        this.templateStorageService = templateStorageService;
        this.documentGenerationService = new DocumentGenerationService(
                new DocumentTemplateService(),
                templateStorageService,
                java.nio.file.Path.of("generated-documents")
        );

        setupActions(preselectedItems);
    }

    private void setupActions(List<Item> preselectedItems) {
        List<Line> lines = lineRepo.findAll();
        view.setLines(lines);
        view.setTemplates(templateStorageService.listTemplates());
        templateStorageService.findLastUsedTemplate().ifPresent(lastUsed -> {
            view.getTemplateSelector().getItems().stream()
                    .filter(t -> t.id() == lastUsed.id())
                    .findFirst()
                    .ifPresent(view.getTemplateSelector()::setValue);
        });

        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.setProductSelectionHandler(this::onProductSelected);
        view.getProduceButton().setOnAction(e -> produceBatch());

        if (preselectedItems != null && !preselectedItems.isEmpty()) {
            int productId = preselectedItems.getFirst().productId();
            lines.stream()
                    .filter(l -> l.productId() == productId)
                    .findFirst()
                    .ifPresent(line -> {
                        view.getLineSelector().setValue(line);
                        filteredItems = itemRepo.findByProduct(line.productId());
                        Product preselectedProduct = findProductById(line.productId());
                        view.setSelectableProducts(preselectedProduct == null ? List.of() : List.of(preselectedProduct), preselectedProduct);
                        view.setItemRows(filteredItems);
                    });
        }
    }

    private void onLineChanged() {
        Line selected = view.getLineSelector().getValue();
        if (selected == null) {
            filteredItems = List.of();
            view.clearProducts();
            view.setItemRows(List.of());
            return;
        }

        Product lineProduct = findProductById(selected.productId());
        view.setSelectableProducts(lineProduct == null ? List.of() : List.of(lineProduct), null);
        filteredItems = List.of();
        view.setItemRows(List.of());
        view.setFeedback("", false);
    }


    private void onProductSelected(Product product) {
        filteredItems = itemRepo.findByProduct(product.id());
        view.setItemRows(filteredItems);
        view.setFeedback("", false);
    }

    private Product findProductById(int productId) {
        return productRepo.findAll().stream()
                .filter(product -> product.id() == productId)
                .findFirst()
                .orElse(null);
    }

    private void produceBatch() {
        try {
            Line line = view.getLineSelector().getValue();
            if (line == null) {
                throw new IllegalArgumentException("Seleziona una linea di produzione.");
            }

            TemplateStorageService.SavedTemplateRef selectedTemplate = view.getTemplateSelector().getValue();
            if (selectedTemplate == null) {
                throw new IllegalArgumentException("Seleziona un template documento prima di produrre batch.");
            }

            List<BatchProductionService.ProductionRequestLine> requestLines = collectLines();
            BatchProductionService.ProductionPlan plan = service.buildPlan(requestLines, itemRepo, compositionRepo, line);
            BatchProductionService.PersistResult result = service.persistPlan(
                    plan,
                    productionRepo,
                    LocalDate.now(),
                    view.getNotesArea().getText()
            );

            String generatedDocumentPath = documentGenerationService.generateForBatchProduction(
                    selectedTemplate,
                    line.name(),
                    view.getNotesArea().getText(),
                    toBatchItemParams(plan.lines()),
                    result.productionOrderId()
            ).toAbsolutePath().toString();

            view.setFeedback(
                    "Batch salvato. Ordine #" + result.productionOrderId() +
                            " con " + plan.lines().size() + " righe, quantità totale " + result.totalQuantity() +
                            ". Documento: " + generatedDocumentPath,
                    false
            );
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante il salvataggio batch.", true);
        }
    }

    private List<DocumentGenerationService.BatchItemParam> toBatchItemParams(List<BatchProductionService.ProductionPlanLine> planLines) {
        List<DocumentGenerationService.BatchItemParam> items = new ArrayList<>();
        for (BatchProductionService.ProductionPlanLine line : planLines) {
            items.add(new DocumentGenerationService.BatchItemParam(line.item().code(), line.quantity()));
        }
        return items;
    }

    private List<BatchProductionService.ProductionRequestLine> collectLines() {
        List<BatchProductionService.ProductionRequestLine> lines = new ArrayList<>();

        for (BatchProductionView.BatchRow row : view.getRows()) {
            Item item = row.getItem();
            String qtyRaw = row.getQuantityField().getText();

            int qty;
            if (qtyRaw == null || qtyRaw.isBlank()) {
                qty = 0;
            } else {
                try {
                    qty = Integer.parseInt(qtyRaw.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Quantità non valida per l'item " + item.code() + ".");
                }
            }

            lines.add(new BatchProductionService.ProductionRequestLine(item.id(), qty));
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Seleziona un prodotto con item disponibili prima di produrre.");
        }

        return lines;
    }
}
