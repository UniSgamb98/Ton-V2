package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.features.documents.template.service.TemplateEditorService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionDocumentParamsService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchProductionController {

    private final BatchProductionView view;
    private final ItemRepository itemRepo;
    private final LineRepository lineRepo;
    private final CompositionRepository compositionRepo;
    private final ProductRepository productRepo;
    private final ProductionRepository productionRepo;
    private final BatchProductionService service;
    private final TemplateEditorService templateEditorService;
    private final BatchProductionDocumentParamsService documentParamsService;

    private List<Item> filteredItems = List.of();

    public BatchProductionController(BatchProductionView view,
                                     ItemRepository itemRepo,
                                     LineRepository lineRepo,
                                     CompositionRepository compositionRepo,
                                     ProductRepository productRepo,
                                     ProductionRepository productionRepo,
                                     BatchProductionService service,
                                     TemplateEditorService templateEditorService,
                                     BatchProductionDocumentParamsService documentParamsService,
                                     List<Item> preselectedItems) {
        this.view = view;
        this.itemRepo = itemRepo;
        this.lineRepo = lineRepo;
        this.compositionRepo = compositionRepo;
        this.productRepo = productRepo;
        this.productionRepo = productionRepo;
        this.service = service;
        this.templateEditorService = templateEditorService;
        this.documentParamsService = documentParamsService;

        setupActions(preselectedItems);
    }

    private void setupActions(List<Item> preselectedItems) {
        List<Line> lines = lineRepo.findAll();
        view.setLines(lines);

        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.setProductSelectionHandler(this::onProductSelected);
        view.getProduceButton().setOnAction(e -> produceBatch());
        refreshTemplateSelector();

        if (preselectedItems != null && !preselectedItems.isEmpty()) {
            int productId = preselectedItems.get(0).productId();
            lines.stream()
                    .filter(l -> l.productId() == productId)
                    .findFirst()
                    .ifPresent(line -> {
                        view.getLineSelector().setValue(line);
                        filteredItems = itemRepo.findByProduct(line.productId());
                        Product preselectedProduct = productRepo.findById(line.productId());
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

        Product lineProduct = productRepo.findById(selected.productId());
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

    private void produceBatch() {
        try {
            Line line = view.getLineSelector().getValue();
            if (line == null) {
                throw new IllegalArgumentException("Seleziona una linea di produzione.");
            }

            List<BatchProductionService.ProductionRequestLine> requestLines = collectLines();
            BatchProductionService.ProductionPlan plan = service.buildPlan(requestLines, itemRepo, compositionRepo, line);
            BatchProductionService.PersistResult result = service.persistPlan(
                    plan,
                    productionRepo,
                    LocalDate.now(),
                    view.getNotesArea().getText()
            );

            String documentPath = generateDocumentIfTemplateSelected(line, plan);

            view.setFeedback(
                    "Batch salvato. Ordine #" + result.productionOrderId() +
                            " con " + plan.lines().size() + " righe, quantità totale " + result.totalQuantity() + "." +
                            (documentPath == null ? "" : " Documento generato: " + documentPath),
                    false
            );
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante il salvataggio batch.", true);
        }
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

    private void refreshTemplateSelector() {
        List<String> templateNames = templateEditorService.getSavedTemplates().stream()
                .map(TemplateEditorService.TemplateSnapshot::name)
                .collect(Collectors.toList());
        view.setTemplateNames(templateNames, templateEditorService.getLastBatchTemplateName());
        view.getTemplateSelector().valueProperty().addListener((obs, oldValue, newValue) ->
                templateEditorService.setLastBatchTemplateName(newValue)
        );
    }

    private String generateDocumentIfTemplateSelected(Line line, BatchProductionService.ProductionPlan plan) {
        String selectedTemplateName = view.getTemplateSelector().getValue();
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
                        view.getNotesArea().getText(),
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
}
