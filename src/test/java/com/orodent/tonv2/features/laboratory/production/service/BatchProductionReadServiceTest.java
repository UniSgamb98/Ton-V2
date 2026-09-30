package com.orodent.tonv2.features.laboratory.production.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProductionReadServiceTest {

    @Test
    void blankLineDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        BatchProductionReadService service = new BatchProductionReadService(
                () -> {
                    connectionOpened.set(true);
                    throw new AssertionError("The database must not be queried for a blank line");
                },
                null
        );

        assertTrue(service.findProductsByLineName(" ").isEmpty());
        assertFalse(connectionOpened.get());
    }
}
