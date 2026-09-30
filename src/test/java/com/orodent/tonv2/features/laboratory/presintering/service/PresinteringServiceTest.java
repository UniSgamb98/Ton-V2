package com.orodent.tonv2.features.laboratory.presintering.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PresinteringServiceTest {

    @Test
    void nullConfirmationDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        PresinteringService service = new PresinteringService(
                () -> {
                    connectionOpened.set(true);
                    throw new AssertionError("The database must not be opened for an invalid command");
                },
                null
        );

        assertThrows(IllegalArgumentException.class, () -> service.confirmBatch(null));
        assertFalse(connectionOpened.get());
    }
}
