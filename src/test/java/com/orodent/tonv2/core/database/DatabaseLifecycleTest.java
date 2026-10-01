package com.orodent.tonv2.core.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseLifecycleTest {

    @Test
    void connectionCannotBeOpenedBeforeInitialization() {
        Database database = new Database();

        assertThrows(IllegalStateException.class, database::openConnection);
        assertEquals(Database.State.NEW, database.state());
    }

    @Test
    void shutdownBeforeInitializationIsIdempotent() {
        Database database = new Database();

        database.stop();
        database.stop();

        assertEquals(Database.State.STOPPED, database.state());
    }
}
