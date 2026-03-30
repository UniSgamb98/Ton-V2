package com.orodent.tonv2.core.database.implementation;

import com.orodent.tonv2.core.database.model.DocumentTemplate;
import com.orodent.tonv2.core.database.repository.DocumentTemplateRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DocumentTemplateRepositoryImpl implements DocumentTemplateRepository {

    private static final String TEMPLATE_TABLE = "document_template";

    private final Connection conn;

    public DocumentTemplateRepositoryImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void ensureTable() {
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

        try (PreparedStatement statement = conn.prepareStatement(createSql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (!"X0Y32".equalsIgnoreCase(e.getSQLState())) {
                throw new RuntimeException("Errore creazione tabella template documento.", e);
            }
        }
    }

    @Override
    public void upsert(String name, String templateContent, String sqlQuery, String presetCode) {
        try {
            Integer existingId = findTemplateIdByName(name);
            if (existingId == null) {
                insertTemplate(name, templateContent, sqlQuery, presetCode);
            } else {
                updateTemplate(existingId, templateContent, sqlQuery, presetCode);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio template su database.", e);
        }
    }

    @Override
    public List<DocumentTemplate> findAll() {
        String sql = """
                SELECT id, name, template_content, sql_query, preset_code, updated_at
                FROM %s
                ORDER BY updated_at DESC, name
                """.formatted(TEMPLATE_TABLE);

        List<DocumentTemplate> loaded = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                loaded.add(mapTemplate(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template da database.", e);
        }

        return loaded;
    }

    @Override
    public List<DocumentTemplate> findByNameContaining(String nameFilter) {
        String normalizedFilter = nameFilter == null ? "" : nameFilter.trim();
        String sql = """
                SELECT id, name, template_content, sql_query, preset_code, updated_at
                FROM %s
                WHERE UPPER(name) LIKE UPPER(?)
                ORDER BY updated_at DESC, name
                """.formatted(TEMPLATE_TABLE);

        List<DocumentTemplate> loaded = new ArrayList<>();
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, "%" + normalizedFilter + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    loaded.add(mapTemplate(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template filtrati da database.", e);
        }

        return loaded;
    }

    @Override
    public DocumentTemplate findById(int id) {
        String sql = """
                SELECT id, name, template_content, sql_query, preset_code, updated_at
                FROM %s
                WHERE id = ?
                """.formatted(TEMPLATE_TABLE);

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapTemplate(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template per id da database.", e);
        }
    }

    @Override
    public void updateById(int id, String name, String templateContent, String sqlQuery, String presetCode) {
        String sql = """
                UPDATE %s
                SET name = ?, template_content = ?, sql_query = ?, preset_code = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """.formatted(TEMPLATE_TABLE);

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, templateContent == null ? "" : templateContent);
            statement.setString(3, sqlQuery == null ? "" : sqlQuery);
            statement.setString(4, presetCode == null ? "" : presetCode);
            statement.setInt(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore aggiornamento template su database.", e);
        }
    }

    private Integer findTemplateIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM " + TEMPLATE_TABLE + " WHERE name = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
                return null;
            }
        }
    }

    private void insertTemplate(String name, String templateText, String sqlQuery, String presetCode) throws SQLException {
        String sql = """
                INSERT INTO %s (name, template_content, sql_query, preset_code)
                VALUES (?, ?, ?, ?)
                """.formatted(TEMPLATE_TABLE);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, templateText == null ? "" : templateText);
            statement.setString(3, sqlQuery == null ? "" : sqlQuery);
            statement.setString(4, presetCode == null ? "" : presetCode);
            statement.executeUpdate();
        }
    }

    private DocumentTemplate mapTemplate(ResultSet resultSet) throws SQLException {
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        Instant instant = updatedAt == null ? Instant.now() : updatedAt.toInstant();
        return new DocumentTemplate(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("template_content"),
                resultSet.getString("sql_query"),
                resultSet.getString("preset_code"),
                instant
        );
    }

    private void updateTemplate(int id, String templateText, String sqlQuery, String presetCode) throws SQLException {
        String sql = """
                UPDATE %s
                SET template_content = ?, sql_query = ?, preset_code = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """.formatted(TEMPLATE_TABLE);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, templateText == null ? "" : templateText);
            statement.setString(2, sqlQuery == null ? "" : sqlQuery);
            statement.setString(3, presetCode == null ? "" : presetCode);
            statement.setInt(4, id);
            statement.executeUpdate();
        }
    }
}
