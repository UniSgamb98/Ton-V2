package com.orodent.tonv2.core.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionProvider {

    Connection openConnection();

    default <T> T withConnection(ConnectionWork<T> work) {
        try (Connection connection = openConnection()) {
            return work.execute(connection);
        } catch (SQLException exception) {
            throw new RuntimeException("Errore durante l'operazione sul database.", exception);
        }
    }

    @FunctionalInterface
    interface ConnectionWork<T> {
        T execute(Connection connection) throws SQLException;
    }
}
