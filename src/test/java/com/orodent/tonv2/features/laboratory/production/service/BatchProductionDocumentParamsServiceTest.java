package com.orodent.tonv2.features.laboratory.production.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProductionDocumentParamsServiceTest {

    @Test
    void nullRequestDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        BatchProductionDocumentParamsService service = new BatchProductionDocumentParamsService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("A null request must not open a connection");
        });

        assertTrue(service.buildParams(null).get("items") instanceof java.util.List<?>);
        assertFalse(connectionOpened.get());
    }
}
