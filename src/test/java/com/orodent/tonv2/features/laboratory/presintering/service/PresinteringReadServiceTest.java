package com.orodent.tonv2.features.laboratory.presintering.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresinteringReadServiceTest {

    @Test
    void blankFurnaceDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        PresinteringReadService service = new PresinteringReadService(
                () -> {
                    connectionOpened.set(true);
                    throw new AssertionError("The database must not be queried for a blank furnace");
                },
                null
        );

        assertTrue(service.loadFurnaceItemSuggestions(" ").isEmpty());
        assertFalse(connectionOpened.get());
    }
}
