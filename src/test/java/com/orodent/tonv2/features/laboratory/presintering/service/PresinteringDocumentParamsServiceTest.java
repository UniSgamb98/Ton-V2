package com.orodent.tonv2.features.laboratory.presintering.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresinteringDocumentParamsServiceTest {

    @Test
    void nullRequestDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        PresinteringDocumentParamsService service = new PresinteringDocumentParamsService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("A null request must not open a connection");
        });

        assertTrue(service.buildParams(null).isEmpty());
        assertFalse(connectionOpened.get());
    }
}
