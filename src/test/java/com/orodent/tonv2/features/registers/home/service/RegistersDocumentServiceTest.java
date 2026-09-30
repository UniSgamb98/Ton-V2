package com.orodent.tonv2.features.registers.home.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistersDocumentServiceTest {

    @Test
    void invalidInputDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        RegistersDocumentService service = new RegistersDocumentService(
                () -> {
                    connectionOpened.set(true);
                    throw new AssertionError("The database must not be queried for invalid input");
                },
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.generateCompositionDocument("", "LOT-1")
        );
        assertFalse(connectionOpened.get());
    }
}
