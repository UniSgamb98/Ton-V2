package com.orodent.tonv2.core.database;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database implements ConnectionProvider {

    private static final String DATABASE_NAME = "TonDatabase";
    private static final String DATABASE_USER = "APP";
    private static final String DATABASE_PASSWORD = "pw";
    private static final int START_ATTEMPTS = 6;
    private static final long START_RETRY_DELAY_MILLIS = 1_000;

    private volatile State state = State.NEW;

    @Override
    public Connection openConnection() {
        if (state != State.READY) {
            throw new IllegalStateException("Database non pronto: stato " + state);
        }
        return openConnectionInternal();
    }

    private Connection openConnectionInternal() {
        try {
            String url = "jdbc:derby:" + DATABASE_NAME + ";create=true;user="
                    + DATABASE_USER + ";password=" + DATABASE_PASSWORD;
            return DriverManager.getConnection(url);
        } catch (SQLException exception) {
            throw new RuntimeException("Errore connessione DB", exception);
        }
    }

    public synchronized void start() {
        if (state == State.READY) {
            return;
        }
        if (state == State.STARTING) {
            throw new IllegalStateException("Avvio database già in corso.");
        }

        state = State.STARTING;
        try {
            configureDerby();
            Class<?> driverClass = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            driverClass.getConstructor().newInstance();
            waitForStart();
            try (Connection ignored = openConnectionInternal()) {
                // Verify that the embedded database accepts connections before publishing READY.
            }
            state = State.READY;
        } catch (Exception exception) {
            state = State.FAILED;
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DatabaseInitializationException("Impossibile avviare il database.", exception);
        }
    }

    private void configureDerby() throws Exception {
        String configuredHome = System.getProperty("ton.database.home");
        Path databaseHome = configuredHome == null || configuredHome.isBlank()
                ? defaultDatabaseHome()
                : Path.of(configuredHome);
        Files.createDirectories(databaseHome);
        System.setProperty("derby.system.home", databaseHome.toAbsolutePath().toString());
        System.setProperty("derby.drda.startNetworkServer", "true");
        System.setProperty("derby.drda.host", InetAddress.getLocalHost().getHostAddress());
    }

    private Path defaultDatabaseHome() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase();
        if (operatingSystem.contains("win")) {
            // Preserve the location used by previous releases unless explicitly overridden.
            return Path.of("C:\\");
        }
        return Path.of(System.getProperty("user.home"), ".ton", "database");
    }

    private void waitForStart() throws Exception {
        org.apache.derby.drda.NetworkServerControl server = new org.apache.derby.drda.NetworkServerControl();
        Exception lastFailure = null;
        for (int attempt = 0; attempt < START_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(START_RETRY_DELAY_MILLIS);
                server.ping();
                return;
            } catch (InterruptedException exception) {
                throw exception;
            } catch (Exception exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null ? new IllegalStateException("Timeout avvio database.") : lastFailure;
    }

    public synchronized void stop() {
        if (state == State.NEW || state == State.STOPPED) {
            state = State.STOPPED;
            return;
        }
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException expectedDerbyShutdown) {
            // Derby reports a successful engine shutdown through SQLException (XJ015).
        } finally {
            state = State.STOPPED;
        }
    }

    State state() {
        return state;
    }

    enum State {
        NEW,
        STARTING,
        READY,
        FAILED,
        STOPPED
    }

    public static class DatabaseInitializationException extends RuntimeException {
        public DatabaseInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
