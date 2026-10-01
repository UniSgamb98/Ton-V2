package com.orodent.tonv2.features.laboratory.presintering.service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record PresinteringLocalPlanState(
        Map<Integer, Map<Integer, Integer>> plannedByFurnace,
        Map<Integer, PresinteringFurnaceConfig> furnaceConfigById,
        Integer lastKnownFiringId,
        Instant savedAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public PresinteringLocalPlanState {
        plannedByFurnace = copyPlan(plannedByFurnace);
        furnaceConfigById = furnaceConfigById == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(furnaceConfigById);
        savedAt = savedAt == null ? Instant.now() : savedAt;
    }

    private static Map<Integer, Map<Integer, Integer>> copyPlan(Map<Integer, Map<Integer, Integer>> source) {
        Map<Integer, Map<Integer, Integer>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((furnaceId, items) -> copy.put(furnaceId, new LinkedHashMap<>(items)));
        }
        return copy;
    }
}
