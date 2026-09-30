package com.orodent.tonv2.features.laboratory.firingprogram.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FiringProgramServiceTest {

    @Test
    void invalidProgramDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        FiringProgramService service = new FiringProgramService(() -> {
            connectionOpened.set(true);
            throw new AssertionError("The database must not be opened for invalid input");
        });

        assertThrows(IllegalArgumentException.class, () -> service.saveProgram(" ", List.of()));
        assertFalse(connectionOpened.get());
    }
}
