package com.orodent.tonv2.core.database;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
