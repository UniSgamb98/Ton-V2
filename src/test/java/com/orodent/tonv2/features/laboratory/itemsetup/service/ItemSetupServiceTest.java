package com.orodent.tonv2.features.laboratory.itemsetup.service;

import com.orodent.tonv2.features.laboratory.itemsetup.model.ProductSetupStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemSetupServiceTest {

    @Test
    void invalidItemDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        ItemSetupService service = new ItemSetupService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("The database must not be opened for invalid input");
        });

        assertThrows(IllegalArgumentException.class,
                () -> service.createItemForActiveComposition(" ", 1, 10));
        assertFalse(connectionOpened.get());
    }

    @Test
    void invalidProductActivationDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        ItemSetupService service = new ItemSetupService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("The database must not be opened for invalid input");
        });

        assertThrows(IllegalArgumentException.class, () -> service.activateLatestComposition(0));
        assertFalse(connectionOpened.get());
    }

    @Test
    void invalidProductSetupStatusDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        ItemSetupService service = new ItemSetupService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("The database must not be opened for an invalid product");
        });

        ProductSetupStatus status = service.findProductSetupStatus(0);

        assertFalse(status.hasActiveComposition());
        assertFalse(status.hasBlankModel());
        assertFalse(connectionOpened.get());
    }
}
