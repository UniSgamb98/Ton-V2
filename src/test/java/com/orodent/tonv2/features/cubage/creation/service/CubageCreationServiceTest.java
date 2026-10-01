package com.orodent.tonv2.features.cubage.creation.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubageCreationServiceTest {

    @Test
    void validationWithoutPayloadDoesNotOpenAConnection() {
        AtomicInteger openedConnections = new AtomicInteger();
        ConnectionProvider provider = () -> {
            openedConnections.incrementAndGet();
            throw new AssertionError("La validazione incompleta non deve accedere al database");
        };

        CubageCreationService service = new CubageCreationService(provider);

        CubageCreationService.FormulaValidationResult result =
                service.validateAndBuildFormulaSet("set", "volume = width", null);

        assertFalse(result.valid());
        assertTrue(result.message().contains("Seleziona un payload"));
        assertTrue(openedConnections.get() == 0);
    }

    @Test
    void blankPayloadCodeDoesNotOpenAConnection() {
        AtomicInteger openedConnections = new AtomicInteger();
        ConnectionProvider provider = () -> {
            openedConnections.incrementAndGet();
            throw new AssertionError("Un codice vuoto non deve accedere al database");
        };

        CubageCreationService service = new CubageCreationService(provider);

        assertTrue(service.getAllVersionsForPayload("  ").isEmpty());
        assertTrue(openedConnections.get() == 0);
    }
}
