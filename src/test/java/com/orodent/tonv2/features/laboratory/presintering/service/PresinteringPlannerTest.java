package com.orodent.tonv2.features.laboratory.presintering.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PresinteringPlannerTest {
    private final PresinteringPlanner planner = new PresinteringPlanner();

    @Test
    void planDisksCapsRequestsAtAvailableQuantityWithoutChangingInput() {
        PresinteringPlanningSnapshot initial = snapshot(Map.of(10, 3), Map.of());

        PresinteringPlanner.PlanResult result = planner.planDisks(initial, 7, Map.of(10, 5));

        assertEquals(3, result.insertedQuantity());
        assertEquals(0, result.state().availableByItemId().get(10));
        assertEquals(3, result.state().plannedByFurnace().get(7).get(10));
        assertEquals(3, initial.availableByItemId().get(10));
        assertEquals(Map.of(), initial.plannedByFurnace());
    }

    @Test
    void removePlannedItemReturnsQuantityToAvailability() {
        PresinteringPlanningSnapshot initial = snapshot(Map.of(10, 1), Map.of(7, Map.of(10, 2)));

        PresinteringPlanningSnapshot result = planner.removePlannedItem(initial, 7, 10);

        assertEquals(3, result.availableByItemId().get(10));
        assertEquals(Map.of(), result.plannedByFurnace());
    }

    @Test
    void invalidRemovalLeavesSnapshotUntouched() {
        PresinteringPlanningSnapshot initial = snapshot(Map.of(10, 1), Map.of());

        assertSame(initial, planner.removePlannedItem(initial, 0, 10));
    }

    private PresinteringPlanningSnapshot snapshot(Map<Integer, Integer> available,
                                                   Map<Integer, Map<Integer, Integer>> planned) {
        return new PresinteringPlanningSnapshot(
                new LinkedHashMap<>(available),
                new LinkedHashMap<>(planned),
                Map.of(10, "ITEM-10"),
                Instant.now()
        );
    }
}
