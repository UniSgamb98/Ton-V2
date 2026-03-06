package com.orodent.tonv2.features.laboratory.production.controller;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.features.laboratory.production.service.BatchProductionService;
import com.orodent.tonv2.features.laboratory.production.view.BatchProductionView;

import java.util.ArrayList;
import java.util.List;

public class BatchProductionController {

    private final BatchProductionView view;
    private final ItemRepository itemRepo;
    private final CompositionRepository compositionRepo;
    private final BatchProductionService service;

    private final List<Item> allItems;

    public BatchProductionController(BatchProductionView view,
                                     ItemRepository itemRepo,
                                     CompositionRepository compositionRepo,
                                     BatchProductionService service,
                                     List<Item> preselectedItems) {
        this.view = view;
        this.itemRepo = itemRepo;
        this.compositionRepo = compositionRepo;
        this.service = service;

        this.allItems = itemRepo.findAll();
        setupActions(preselectedItems);
    }

    private void setupActions(List<Item> preselectedItems) {
        view.getAddRowButton().setOnAction(e -> view.addRow(allItems, null));
        view.getProduceButton().setOnAction(e -> produceBatch());

        if (preselectedItems != null && !preselectedItems.isEmpty()) {
            for (Item item : preselectedItems) {
                view.addRow(allItems, item);
            }
        } else {
            view.addRow(allItems, null);
        }
    }

    private void produceBatch() {
        try {
            List<BatchProductionService.ProductionRequestLine> requestLines = collectLines();
            BatchProductionService.ProductionPlan plan = service.buildPlan(requestLines, itemRepo, compositionRepo);

            int totalRows = plan.lines().size();
            int totalQty = plan.lines().stream().mapToInt(BatchProductionService.ProductionPlanLine::quantity).sum();
            view.setFeedback("Batch pronto: " + totalRows + " item, quantità totale " + totalQty + ".", false);
        } catch (IllegalArgumentException ex) {
            view.setFeedback(ex.getMessage(), true);
        } catch (Exception ex) {
            view.setFeedback("Errore durante la preparazione batch.", true);
        }
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
