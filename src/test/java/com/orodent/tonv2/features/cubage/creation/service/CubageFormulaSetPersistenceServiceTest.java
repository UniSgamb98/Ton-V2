package com.orodent.tonv2.features.cubage.creation.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubageFormulaSetPersistenceServiceTest {

    @Test
    void missingCompilationDoesNotOpenAConnection() {
        AtomicInteger openedConnections = new AtomicInteger();
        ConnectionProvider provider = () -> {
            openedConnections.incrementAndGet();
            throw new AssertionError("Una richiesta non valida non deve aprire transazioni");
        };
        CubageFormulaSetPersistenceService service = new CubageFormulaSetPersistenceService(provider);

        assertThrows(IllegalArgumentException.class, () -> service.save(null));
        assertTrue(openedConnections.get() == 0);
    }
}
