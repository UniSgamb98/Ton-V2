package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.ProductionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BatchProductionService {

    public ProductionPlan buildPlan(List<ProductionRequestLine> requestLines,
                                    ItemRepository itemRepo,
                                    CompositionRepository compositionRepo,
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
            if (item.productId() != line.productId()) {
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

    public PersistResult persistPlan(ProductionPlan plan,
                                     ProductionRepository productionRepo,
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
}
