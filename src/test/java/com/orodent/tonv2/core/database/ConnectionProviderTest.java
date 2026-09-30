package com.orodent.tonv2.core.database;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionProviderTest {

    @Test
    void closesConnectionAfterSuccessfulWork() {
        AtomicBoolean closed = new AtomicBoolean();
        ConnectionProvider provider = () -> connectionTrackingClose(closed);

        String result = provider.withConnection(connection -> "ok");

        assertEquals("ok", result);
        assertTrue(closed.get());
    }

    @Test
    void closesConnectionWhenWorkFails() {
        AtomicBoolean closed = new AtomicBoolean();
        ConnectionProvider provider = () -> connectionTrackingClose(closed);

        assertThrows(IllegalStateException.class, () -> provider.withConnection(connection -> {
            throw new IllegalStateException("failure");
        }));
        assertTrue(closed.get());
    }

    @Test
    void commitsAndClosesSuccessfulTransaction() {
        TransactionState state = new TransactionState();
        ConnectionProvider provider = () -> transactionConnection(state);

        assertEquals("saved", provider.withTransaction(connection -> "saved"));

        assertEquals(1, state.commits.get());
        assertEquals(0, state.rollbacks.get());
        assertTrue(state.closed.get());
        assertFalse(state.autoCommit.get());
    }

    @Test
    void rollsBackAndClosesFailedTransaction() {
        TransactionState state = new TransactionState();
        ConnectionProvider provider = () -> transactionConnection(state);

        assertThrows(IllegalStateException.class, () -> provider.withTransaction(connection -> {
            throw new IllegalStateException("failure");
        }));

        assertEquals(0, state.commits.get());
        assertEquals(1, state.rollbacks.get());
        assertTrue(state.closed.get());
        assertFalse(state.autoCommit.get());
    }

    private Connection connectionTrackingClose(AtomicBoolean closed) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        closed.set(true);
                        return null;
                    }
                    if (method.getName().equals("isClosed")) {
                        return closed.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private Connection transactionConnection(TransactionState state) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAutoCommit" -> state.autoCommit.get();
                    case "setAutoCommit" -> {
                        state.autoCommit.set((boolean) args[0]);
                        yield null;
                    }
                    case "commit" -> {
                        state.commits.incrementAndGet();
                        yield null;
                    }
                    case "rollback" -> {
                        state.rollbacks.incrementAndGet();
                        yield null;
                    }
                    case "close" -> {
                        state.closed.set(true);
                        yield null;
                    }
                    case "isClosed" -> state.closed.get();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static final class TransactionState {
        private final AtomicBoolean autoCommit = new AtomicBoolean(true);
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
    }
}
