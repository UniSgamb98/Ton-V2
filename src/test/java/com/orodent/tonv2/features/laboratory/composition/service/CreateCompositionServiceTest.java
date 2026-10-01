package com.orodent.tonv2.features.laboratory.composition.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateCompositionServiceTest {
    @Test
    void invalidCompositionDoesNotOpenDatabaseConnection() {
        AtomicBoolean opened = new AtomicBoolean();
        CreateCompositionService service = new CreateCompositionService(() -> {
            opened.set(true);
            throw new AssertionError("Database must not be opened for invalid input");
        });

        assertThrows(IllegalArgumentException.class, () -> service.saveComposition(
                new CreateCompositionService.SaveCompositionRequest(null, null, null, null, null, null)));
        assertFalse(opened.get());
    }
}
