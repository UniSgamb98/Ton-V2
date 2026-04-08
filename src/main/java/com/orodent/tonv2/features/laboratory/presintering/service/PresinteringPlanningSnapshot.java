package com.orodent.tonv2.features.laboratory.presintering.service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record PresinteringPlanningSnapshot(
        Map<Integer, Integer> availableByItemId,
        Map<Integer, Map<Integer, Integer>> plannedByFurnace,
        Map<Integer, String> itemCodeById,
        Instant savedAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public PresinteringPlanningSnapshot {
        availableByItemId = availableByItemId == null ? new LinkedHashMap<>() : new LinkedHashMap<>(availableByItemId);
        plannedByFurnace = deepCopy(plannedByFurnace);
        itemCodeById = itemCodeById == null ? new LinkedHashMap<>() : new LinkedHashMap<>(itemCodeById);
        savedAt = savedAt == null ? Instant.now() : savedAt;
    }

    private static Map<Integer, Map<Integer, Integer>> deepCopy(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }
}
