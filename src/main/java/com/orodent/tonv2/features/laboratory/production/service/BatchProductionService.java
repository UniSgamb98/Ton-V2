package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BatchProductionService {

    public ProductionPlan buildPlan(List<ProductionRequestLine> requestLines,
                                    ItemRepository itemRepo,
                                    CompositionRepository compositionRepo) {
        if (requestLines == null || requestLines.isEmpty()) {
            throw new IllegalArgumentException("Aggiungi almeno una riga da produrre.");
        }

        Map<Integer, Integer> mergedQtyByItem = new LinkedHashMap<>();

        for (ProductionRequestLine line : requestLines) {
            if (line.itemId() <= 0) {
                throw new IllegalArgumentException("Item non valido in una delle righe.");
            }
            if (line.quantity() <= 0) {
                throw new IllegalArgumentException("La quantità deve essere maggiore di zero.");
            }
            mergedQtyByItem.merge(line.itemId(), line.quantity(), Integer::sum);
        }

        List<ProductionPlanLine> planLines = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : mergedQtyByItem.entrySet()) {
            Item item = itemRepo.findById(entry.getKey());
            if (item == null) {
                throw new IllegalArgumentException("Item con id " + entry.getKey() + " non trovato.");
            }

            Optional<Integer> activeCompositionId = compositionRepo.findActiveCompositionId(item.productId());
            if (activeCompositionId.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nessuna composizione attiva trovata per il prodotto dell'item " + item.code() + "."
                );
            }

            planLines.add(new ProductionPlanLine(item, entry.getValue(), activeCompositionId.get()));
        }

        return new ProductionPlan(planLines);
    }

    public record ProductionRequestLine(int itemId, int quantity) {}

    public record ProductionPlan(List<ProductionPlanLine> lines) {}

    public record ProductionPlanLine(Item item, int quantity, int compositionId) {}
}
