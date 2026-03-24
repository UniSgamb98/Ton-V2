package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateEditorService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final String RUNTIME_TEMPLATE_KEY = "runtime-template";

    private final Gson gson = new Gson();
    private final List<TemplateSnapshot> savedTemplates = new ArrayList<>();

    public ValidationResult validateTemplate(String templateText) {
        try {
            compileTemplate(templateText);
            return ValidationResult.ok("Template valido.");
        } catch (IOException e) {
            return ValidationResult.error("Errore validazione: " + e.getMessage());
        }
    }

    public PreviewResult previewTemplate(String templateText, String sampleJsonData) {
        try {
            Template template = compileTemplate(templateText);
            Map<String, Object> dataModel = parseJsonData(sampleJsonData);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            return PreviewResult.ok(writer.toString());
        } catch (IOException | TemplateException e) {
            return PreviewResult.error("Errore anteprima: " + e.getMessage());
        }
    }

    public SaveResult saveTemplate(String templateName, String templateText, String sqlQuery, String presetCode) {
        String normalizedName = templateName == null ? "" : templateName.trim();
        if (normalizedName.isBlank()) {
            return SaveResult.error("Nome template obbligatorio.");
        }

        ValidationResult validation = validateTemplate(templateText);
        if (!validation.valid()) {
            return SaveResult.error("Impossibile salvare: " + validation.message());
        }

        savedTemplates.add(new TemplateSnapshot(
                normalizedName,
                templateText == null ? "" : templateText,
                sqlQuery == null ? "" : sqlQuery,
                presetCode == null ? "" : presetCode,
                Instant.now()
        ));

        return SaveResult.ok("Template salvato in memoria applicativa.");
    }

    public List<VariableNode> extractVariablesFromQuery(String sqlQuery, Connection connection) {
        validateSelectQuery(sqlQuery);

        VariableBuilder root = new VariableBuilder("root");
        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            statement.setMaxRows(1);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metadata = rs.getMetaData();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    String label = metadata.getColumnLabel(i);
                    root.appendPath(label);
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Errore esecuzione query: " + e.getMessage(), e);
        }

        return root.toNodes();
    }

    public List<TemplateSnapshot> getSavedTemplates() {
        return List.copyOf(savedTemplates);
    }

    private void validateSelectQuery(String sqlQuery) {
        String normalized = sqlQuery == null ? "" : sqlQuery.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Inserisci una query SQL.");
        }

        if (!(normalized.startsWith("select") || normalized.startsWith("with"))) {
            throw new IllegalArgumentException("Sono consentite solo query SELECT/CTE.");
        }

        String[] blocked = {" insert ", " update ", " delete ", " drop ", " alter ", " truncate ", " merge ", " call "};
        String padded = " " + normalized.replaceAll("\\s+", " ") + " ";
        for (String keyword : blocked) {
            if (padded.contains(keyword)) {
                throw new IllegalArgumentException("La query contiene operazioni non consentite: " + keyword.trim());
            }
        }
    }

    private Template compileTemplate(String templateText) throws IOException {
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate(RUNTIME_TEMPLATE_KEY, templateText == null ? "" : templateText);

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setTemplateLoader(loader);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);

        return cfg.getTemplate(RUNTIME_TEMPLATE_KEY);
    }

    private Map<String, Object> parseJsonData(String sampleJsonData) {
        String payload = sampleJsonData == null ? "" : sampleJsonData.trim();
        if (payload.isBlank()) {
            return new HashMap<>();
        }
        Map<String, Object> parsed = gson.fromJson(payload, MAP_TYPE);
        return parsed == null ? new HashMap<>() : parsed;
    }

    public record ValidationResult(boolean valid, String message) {
        static ValidationResult ok(String message) { return new ValidationResult(true, message); }
        static ValidationResult error(String message) { return new ValidationResult(false, message); }
    }

    public record PreviewResult(boolean success, String htmlOrError) {
        static PreviewResult ok(String html) { return new PreviewResult(true, html); }
        static PreviewResult error(String error) { return new PreviewResult(false, error); }
    }

    public record SaveResult(boolean success, String message) {
        static SaveResult ok(String message) { return new SaveResult(true, message); }
        static SaveResult error(String message) { return new SaveResult(false, message); }
    }

    public record VariableNode(String name, List<VariableNode> children) {}

    public record TemplateSnapshot(String name, String templateContent, String sqlQuery, String presetCode, Instant savedAt) {}

    private static final class VariableBuilder {
        private final String name;
        private final Map<String, VariableBuilder> children = new LinkedHashMap<>();

        private VariableBuilder(String name) {
            this.name = name;
        }

        private void appendPath(String rawPath) {
            if (rawPath == null || rawPath.isBlank()) {
                return;
            }

            String[] tokens = rawPath.split("\\.");
            VariableBuilder current = this;
            for (String token : tokens) {
                String part = token.trim();
                if (part.isBlank()) {
                    continue;
                }
                current = current.children.computeIfAbsent(part, VariableBuilder::new);
            }
        }

        private List<VariableNode> toNodes() {
            List<VariableNode> out = new ArrayList<>();
            for (VariableBuilder child : children.values()) {
                out.add(new VariableNode(child.name, child.toNodes()));
            }
            return out;
        }
    }
}
