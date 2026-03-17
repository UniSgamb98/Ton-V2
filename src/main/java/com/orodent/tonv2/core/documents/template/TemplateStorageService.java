package com.orodent.tonv2.core.documents.template;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TemplateStorageService {
    private final Connection conn;

    public TemplateStorageService(Connection conn) {
        this.conn = conn;
        ensureSchema();
    }

    public SavedTemplateRef saveTemplate(String templateName, String templateBody, String parametersJson) {
        String sql = """
                INSERT INTO document_template (template_name, template_body, parameters_json)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, templateName == null ? "" : templateName.trim());
            ps.setString(2, templateBody == null ? "" : templateBody);
            ps.setString(3, parametersJson == null ? "" : parametersJson);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new SavedTemplateRef(id, normalizeDisplayName(templateName));
                }
            }
            throw new SQLException("Nessun ID restituito per document_template");
        } catch (SQLException e) {
            throw new RuntimeException("Errore salvataggio template su DB", e);
        }
    }

    public List<SavedTemplateRef> listTemplates() {
        String sql = """
                SELECT id, template_name
                FROM document_template
                ORDER BY created_at DESC, id DESC
                """;

        List<SavedTemplateRef> refs = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("template_name");
                refs.add(new SavedTemplateRef(id, normalizeDisplayName(name)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template da DB", e);
        }

        return refs;
    }

    public StoredTemplate loadTemplate(long id) {
        String sql = """
                SELECT id, template_name, template_body, parameters_json, created_at
                FROM document_template
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("Template non trovato: id=" + id);
                }

                return new StoredTemplate(
                        rs.getLong("id"),
                        normalizeDisplayName(rs.getString("template_name")),
                        rs.getString("template_body"),
                        rs.getString("parameters_json"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore caricamento template id=" + id, e);
        }
    }

    private void ensureSchema() {
        String ddl = """
                CREATE TABLE document_template (
                    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    template_name VARCHAR(255) NOT NULL,
                    template_body CLOB NOT NULL,
                    parameters_json CLOB NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            if (!tableAlreadyExists(e)) {
                throw new RuntimeException("Errore creazione tabella document_template", e);
            }
        }
    }

    private boolean tableAlreadyExists(SQLException e) {
        return "X0Y32".equalsIgnoreCase(e.getSQLState());
    }

    private String normalizeDisplayName(String raw) {
        return (raw == null || raw.isBlank()) ? "template" : raw.trim();
    }

    public record SavedTemplateRef(long id, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }

    public record StoredTemplate(long id, String templateName, String templateBody, String parametersJson, Timestamp createdAt) {
    }
}
