package com.orodent.tonv2.features.laboratory.presintering.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Applies in-memory changes to a presintering plan without performing I/O. */
public final class PresinteringPlanner {

    public PlanResult planDisks(PresinteringPlanningSnapshot currentState,
                                int furnaceId,
                                Map<Integer, Integer> requestedByItem) {
        if (currentState == null) {
            throw new IllegalArgumentException("Stato pianificazione non disponibile.");
        }
        if (furnaceId <= 0) {
            throw new IllegalArgumentException("Forno non valido.");
        }
        if (requestedByItem == null || requestedByItem.isEmpty()) {
            return new PlanResult(currentState, 0);
        }

        Map<Integer, Integer> availableByItem = new LinkedHashMap<>(currentState.availableByItemId());
        Map<Integer, Map<Integer, Integer>> plannedByFurnace = copyPlan(currentState.plannedByFurnace());
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

        return new PlanResult(snapshot(currentState, availableByItem, plannedByFurnace), inserted);
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
        Map<Integer, Map<Integer, Integer>> plannedByFurnace = copyPlan(currentState.plannedByFurnace());
        Map<Integer, Integer> plannedItems = plannedByFurnace.get(furnaceId);
        if (plannedItems == null) {
            return currentState;
        }

        Integer removedQuantity = plannedItems.remove(itemId);
        if (removedQuantity == null || removedQuantity <= 0) {
            return currentState;
        }
        availableByItem.merge(itemId, removedQuantity, Integer::sum);
        if (plannedItems.isEmpty()) {
            plannedByFurnace.remove(furnaceId);
        }
        return snapshot(currentState, availableByItem, plannedByFurnace);
    }

    private PresinteringPlanningSnapshot snapshot(PresinteringPlanningSnapshot source,
                                                   Map<Integer, Integer> availableByItem,
                                                   Map<Integer, Map<Integer, Integer>> plannedByFurnace) {
        return new PresinteringPlanningSnapshot(
                availableByItem,
                plannedByFurnace,
                new LinkedHashMap<>(source.itemCodeById()),
                null
        );
    }

    private Map<Integer, Map<Integer, Integer>> copyPlan(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((furnaceId, items) -> copy.put(furnaceId, new LinkedHashMap<>(items)));
        }
        return copy;
    }

    public record PlanResult(PresinteringPlanningSnapshot state, int insertedQuantity) {
    }
}
