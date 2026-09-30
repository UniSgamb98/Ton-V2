package com.orodent.tonv2.features.laboratory.diskmodel.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateDiskModelServiceTest {

    @Test
    void invalidModelDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        CreateDiskModelService service = new CreateDiskModelService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("The database must not be opened for invalid input");
        });

        CreateDiskModelService.CreateDiskModelData invalid = new CreateDiskModelService.CreateDiskModelData(
                " ", 98.0, 1.0, 1.0, 2000.0, 0.5, 1
        );
        assertThrows(IllegalArgumentException.class,
                () -> service.createDiskModel(invalid, List.of(), List.of()));
        assertFalse(connectionOpened.get());
    }
}
