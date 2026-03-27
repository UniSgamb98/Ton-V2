package com.orodent.tonv2.features.documents.template.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TemplateStorageService {

    private static final String TEMPLATE_TABLE = "document_template";

    private final Supplier<Connection> connectionSupplier;

    public TemplateStorageService(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
        ensureTemplateTable();
    }

    public void saveTemplate(String name, String templateText, String sqlQuery, String presetCode) {
        try (Connection connection = connectionSupplier.get()) {
            Integer existingId = findTemplateIdByName(connection, name);
            if (existingId == null) {
                insertTemplate(connection, name, templateText, sqlQuery, presetCode);
            } else {
                updateTemplate(connection, existingId, templateText, sqlQuery, presetCode);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio template su database.", e);
        }
    }

    public List<TemplateSnapshot> loadTemplates() {
        String sql = """
                SELECT name, template_content, sql_query, preset_code, updated_at
                FROM %s
                ORDER BY updated_at DESC, name
                """.formatted(TEMPLATE_TABLE);

        List<TemplateSnapshot> loaded = new ArrayList<>();
        try (Connection connection = connectionSupplier.get();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                Instant instant = updatedAt == null ? Instant.now() : updatedAt.toInstant();
                loaded.add(new TemplateSnapshot(
                        resultSet.getString("name"),
                        resultSet.getString("template_content"),
                        resultSet.getString("sql_query"),
                        resultSet.getString("preset_code"),
                        instant
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template da database.", e);
        }

        return loaded;
    }

    private void ensureTemplateTable() {
        String createSql = """
                CREATE TABLE %s (
                  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                  name VARCHAR(120) NOT NULL UNIQUE,
                  template_content CLOB NOT NULL,
                  sql_query CLOB,
                  preset_code VARCHAR(120),
                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(TEMPLATE_TABLE);

        try (Connection connection = connectionSupplier.get();
             PreparedStatement statement = connection.prepareStatement(createSql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (!"X0Y32".equalsIgnoreCase(e.getSQLState())) {
                throw new RuntimeException("Errore creazione tabella template documento.", e);
            }
        }
    }

    private Integer findTemplateIdByName(Connection connection, String name) throws SQLException {
        String sql = "SELECT id FROM " + TEMPLATE_TABLE + " WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
                return null;
            }
        }
    }

    private void insertTemplate(Connection connection, String name, String templateText, String sqlQuery, String presetCode) throws SQLException {
        String sql = """
                INSERT INTO %s (name, template_content, sql_query, preset_code)
                VALUES (?, ?, ?, ?)
                """.formatted(TEMPLATE_TABLE);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, templateText == null ? "" : templateText);
            statement.setString(3, sqlQuery == null ? "" : sqlQuery);
            statement.setString(4, presetCode == null ? "" : presetCode);
            statement.executeUpdate();
        }
    }

    private void updateTemplate(Connection connection, int id, String templateText, String sqlQuery, String presetCode) throws SQLException {
        String sql = """
                UPDATE %s
                SET template_content = ?, sql_query = ?, preset_code = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """.formatted(TEMPLATE_TABLE);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, templateText == null ? "" : templateText);
            statement.setString(2, sqlQuery == null ? "" : sqlQuery);
            statement.setString(3, presetCode == null ? "" : presetCode);
            statement.setInt(4, id);
            statement.executeUpdate();
        }
    }

    public record TemplateSnapshot(String name,
                                   String templateContent,
                                   String sqlQuery,
                                   String presetCode,
                                   Instant savedAt) {
    }
}
