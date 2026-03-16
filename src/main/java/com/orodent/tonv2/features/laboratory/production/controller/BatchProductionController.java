package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;
import com.orodent.tonv2.features.documents.template.service.DocumentTemplateService;
import com.orodent.tonv2.features.documents.template.service.TemplateStorageService;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchProductionController {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final BatchProductionView view;
    private final ItemRepository itemRepo;
    private final LineRepository lineRepo;
    private final CompositionRepository compositionRepo;
    private final ProductionRepository productionRepo;
    private final BatchProductionService service;
    private final DocumentTemplateService templateService;
    private final TemplateStorageService templateStorageService;

    private List<Item> filteredItems = List.of();

    public BatchProductionController(BatchProductionView view,
                                     ItemRepository itemRepo,
                                     LineRepository lineRepo,
                                     CompositionRepository compositionRepo,
                                     ProductionRepository productionRepo,
                                     BatchProductionService service,
                                     List<Item> preselectedItems) {
        this.view = view;
        this.itemRepo = itemRepo;
        this.lineRepo = lineRepo;
        this.compositionRepo = compositionRepo;
        this.productionRepo = productionRepo;
        this.service = service;
        this.templateService = new DocumentTemplateService();
        this.templateStorageService = new TemplateStorageService(Path.of("saved-templates"));

        setupActions(preselectedItems);
    }

    private void setupActions(List<Item> preselectedItems) {
        List<Line> lines = lineRepo.findAll();
        view.setLines(lines);
        view.setTemplates(templateStorageService.listTemplates());

        view.getLineSelector().setOnAction(e -> onLineChanged());
        view.getAddRowButton().setOnAction(e -> {
            if (filteredItems.isEmpty()) {
                view.setFeedback("Seleziona prima una linea con item disponibili.", true);
                return;
            }
            view.addRow(filteredItems, null);
        });
        view.getProduceButton().setOnAction(e -> produceBatch());

        if (preselectedItems != null && !preselectedItems.isEmpty()) {
            int productId = preselectedItems.get(0).productId();
            lines.stream()
                    .filter(l -> l.productId() == productId)
                    .findFirst()
                    .ifPresent(line -> {
                        view.getLineSelector().setValue(line);
                        filteredItems = itemRepo.findByProduct(line.productId());
                        view.replaceRows(filteredItems);
                        if (!view.getRows().isEmpty()) {
                            view.getRows().get(0).getItemSelector().setValue(preselectedItems.get(0));
                        }
                        for (int i = 1; i < preselectedItems.size(); i++) {
                            view.addRow(filteredItems, preselectedItems.get(i));
                        }
                    });
        }
    }

    private void onLineChanged() {
        Line selected = view.getLineSelector().getValue();
        if (selected == null) {
            filteredItems = List.of();
            view.getRows().clear();
            view.replaceRows(List.of());
            return;
        }

        filteredItems = itemRepo.findByProduct(selected.productId());
        view.replaceRows(filteredItems);
        view.setFeedback("", false);
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

            Path generatedDocument = generateBatchDocument(selectedTemplate, plan, result.productionOrderId());

            view.setFeedback(
                    "Batch salvato. Ordine #" + result.productionOrderId() +
                            " con " + plan.lines().size() + " righe, quantità totale " + result.totalQuantity() +
                            ". Documento: " + generatedDocument.toAbsolutePath(),
                    false
            );
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante il salvataggio batch.", true);
        }
    }

    private Path generateBatchDocument(TemplateStorageService.SavedTemplateRef selectedTemplate,
                                       BatchProductionService.ProductionPlan plan,
                                       int productionOrderId) throws IOException {
        TemplateStorageService.StoredTemplate template = templateStorageService.loadTemplate(selectedTemplate.path());

        Map<String, Object> params = new HashMap<>(templateService.parseParameters(template.parametersJson()));
        List<Map<String, Object>> items = new ArrayList<>();
        for (BatchProductionService.ProductionPlanLine line : plan.lines()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", line.item().code());
            item.put("quantity", line.quantity());
            items.add(item);
        }

        Map<String, Object> rootItem = items.isEmpty() ? Map.of("code", "", "quantity", 0) : items.get(0);
        params.put("item", rootItem);
        params.put("items", items);

        String html = templateService.render(template.templateBody(), params).html();

        Path outputDir = Path.of("generated-documents");
        Files.createDirectories(outputDir);

        String baseName = template.templateName().toLowerCase()
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (baseName.isBlank()) {
            baseName = "documento-batch";
        }

        Path output = outputDir.resolve(baseName + "-order-" + productionOrderId + "-" + LocalDateTime.now().format(FILE_TS) + ".html");
        Files.writeString(output, html, StandardCharsets.UTF_8);
        return output;
    }

    private List<BatchProductionService.ProductionRequestLine> collectLines() {
        List<BatchProductionService.ProductionRequestLine> lines = new ArrayList<>();

        for (BatchProductionView.BatchRow row : view.getRows()) {
            Item item = row.getItemSelector().getValue();
            String qtyRaw = row.getQuantityField().getText();

            if (item == null && (qtyRaw == null || qtyRaw.isBlank())) {
                continue;
            }
            if (item == null) {
                throw new IllegalArgumentException("Seleziona un item in tutte le righe compilate.");
            }

            int qty;
            try {
                qty = Integer.parseInt((qtyRaw == null ? "" : qtyRaw).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Quantità non valida per l'item " + item.code() + ".");
            }

            lines.add(new BatchProductionService.ProductionRequestLine(item.id(), qty));
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Inserisci almeno una riga valida per produrre.");
        }

        return lines;
    }
}
