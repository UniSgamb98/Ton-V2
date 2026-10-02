package com.orodent.tonv2.features.registers.home.service;

import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegistersSearchServiceTest {

    @Test
    void incompleteSearchDoesNotOpenDatabaseConnection() {
        AtomicBoolean connectionOpened = new AtomicBoolean();
        RegistersSearchService service = new RegistersSearchService(
                () -> {
                    connectionOpened.set(true);
                    throw new AssertionError("The database must not be queried for incomplete input");
                },
                null
        );

        RegisterSearchResult result = service.search("", "");

        assertFalse(result.success());
        assertEquals(RegisterSearchResult.FailureReason.INCOMPLETE_CRITERIA, result.failureReason());
        assertNull(result.identity());
        assertFalse(connectionOpened.get());
    }
}
