package com.orodent.tonv2.features.laboratory.presintering.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresinteringLocalPlanStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsAndClearsRecoveryPlan() {
        Path planFile = temporaryDirectory.resolve("nested/presintering.bin");
        PresinteringLocalPlanStore store = new PresinteringLocalPlanStore(planFile);
        PresinteringLocalPlanState state = new PresinteringLocalPlanState(
                Map.of(3, Map.of(11, 4)),
                Map.of(3, new PresinteringFurnaceConfig(1_530, LocalDate.of(2026, 10, 1), "LOT-1")),
                42,
                Instant.parse("2026-10-01T00:00:00Z")
        );

        store.save(state);

        assertEquals(state, store.load().orElseThrow());
        store.clear();
        assertFalse(Files.exists(planFile));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void deletesUnreadableRecoveryPlan() throws Exception {
        Path planFile = temporaryDirectory.resolve("broken.bin");
        Files.writeString(planFile, "not a serialized plan");
        PresinteringLocalPlanStore store = new PresinteringLocalPlanStore(planFile);

        assertTrue(store.load().isEmpty());
        assertFalse(Files.exists(planFile));
    }
}
