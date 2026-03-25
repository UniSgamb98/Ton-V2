package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import freemarker.core.ParseException;
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

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<TemplateSnapshot> savedTemplates = new ArrayList<>();
    private String lastBatchTemplateName;

    public ValidationResult validateTemplate(String templateText) {
        try {
            compileTemplate(templateText);
            return ValidationResult.ok("Template valido.");
        } catch (IOException e) {
            ErrorDetails details = extractErrorDetails(e, "Errore validazione");
            return ValidationResult.error(details.message(), details.line(), details.column());
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
            ErrorDetails details = extractErrorDetails(e, "Errore anteprima");
            return PreviewResult.error(details.message(), details.line(), details.column());
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

    public QueryVariablesResult extractVariablesFromQuery(String sqlQuery, Connection connection) {
        validateSelectQuery(sqlQuery);

        VariableBuilder root = new VariableBuilder("root");
        Map<String, Object> sampleData = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sqlQuery)) {
            statement.setMaxRows(1);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metadata = rs.getMetaData();
                boolean hasRow = rs.next();

                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    String label = metadata.getColumnLabel(i);
                    Object value = hasRow ? rs.getObject(i) : null;
                    root.appendPath(label, stringifyValue(value));
                    putPathValue(sampleData, label, value);
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("Errore esecuzione query: " + e.getMessage(), e);
        }

        return new QueryVariablesResult(root.toNodes(), gson.toJson(sampleData));
    }

    public List<VariableNode> extractVariablesFromParamsMap(Map<String, Object> paramsMap) {
        return mapToVariableNodes(paramsMap);
    }

    public String toJson(Map<String, Object> payload) {
        return gson.toJson(payload == null ? Map.of() : payload);
    }

    public List<TemplateSnapshot> getSavedTemplates() {
        return List.copyOf(savedTemplates);
    }

    public String getTemplateContentByName(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return null;
        }
        for (TemplateSnapshot snapshot : savedTemplates) {
            if (snapshot.name().equals(templateName)) {
                return snapshot.templateContent();
            }
        }
        return null;
    }

    public String getLastBatchTemplateName() {
        return lastBatchTemplateName;
    }

    public void setLastBatchTemplateName(String templateName) {
        lastBatchTemplateName = templateName == null || templateName.isBlank() ? null : templateName.trim();
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

    private void putPathValue(Map<String, Object> target, String rawPath, Object value) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }

        String[] tokens = rawPath.split("\\.");
        Map<String, Object> current = target;
        for (int i = 0; i < tokens.length; i++) {
            String part = tokens[i].trim();
            if (part.isBlank()) {
                continue;
            }

            if (i == tokens.length - 1) {
                current.put(part, value);
            } else {
                Object existing = current.get(part);
                if (!(existing instanceof Map<?, ?>)) {
                    Map<String, Object> child = new LinkedHashMap<>();
                    current.put(part, child);
                    current = child;
                } else {
                    current = (Map<String, Object>) existing;
                }
            }
        }
    }

    private String stringifyValue(Object value) {
        return value == null ? null : value.toString();
    }

    private List<VariableNode> mapToVariableNodes(Map<String, Object> map) {
        if (map == null) {
            return List.of();
        }

        List<VariableNode> nodes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            nodes.add(buildVariableNode(entry.getKey(), entry.getValue()));
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private VariableNode buildVariableNode(String name, Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            List<VariableNode> children = mapToVariableNodes((Map<String, Object>) nestedMap);
            return new VariableNode(name, null, children);
        }

        if (value instanceof List<?> listValue) {
            if (listValue.isEmpty()) {
                return new VariableNode(name + "[]", null, List.of());
            }

            Object first = listValue.getFirst();
            if (first instanceof Map<?, ?> firstMap) {
                List<VariableNode> children = mapToVariableNodes((Map<String, Object>) firstMap);
                return new VariableNode(name + "[]", null, children);
            }

            return new VariableNode(name + "[]", first.toString(), List.of());
        }

        return new VariableNode(name, stringifyValue(value), List.of());
    }

    public record ValidationResult(boolean valid, String message) {
        static ValidationResult ok(String message) { return new ValidationResult(true, message); }
        static ValidationResult error(String message, Integer line, Integer column) {
            return new ValidationResult(false, appendLineColumn(message, line, column));
        }
    }

    public record PreviewResult(boolean success, String htmlOrError, Integer errorLine) {
        static PreviewResult ok(String html) { return new PreviewResult(true, html, null); }
        static PreviewResult error(String error, Integer line, Integer column) {
            return new PreviewResult(false, appendLineColumn(error, line, column), line);
        }
    }

    public record SaveResult(boolean success, String message) {
        static SaveResult ok(String message) { return new SaveResult(true, message); }
        static SaveResult error(String message) { return new SaveResult(false, message); }
    }

    public record VariableNode(String name, String sampleValue, List<VariableNode> children) {}

    public record QueryVariablesResult(List<VariableNode> variables, String sampleJsonPayload) {}

    public record TemplateSnapshot(String name, String templateContent, String sqlQuery, String presetCode, Instant savedAt) {}

    private ErrorDetails extractErrorDetails(Exception exception, String prefix) {
        if (exception instanceof ParseException parseException) {
            String description = safeMessage(parseException.getEditorMessage(), parseException.getMessage());
            return new ErrorDetails(prefix + ": " + description, parseException.getLineNumber(), parseException.getColumnNumber());
        }

        if (exception instanceof TemplateException templateException) {
            String description = safeMessage(templateException.getBlamedExpressionString(), templateException.getMessage());
            return new ErrorDetails(prefix + ": " + description, templateException.getLineNumber(), templateException.getColumnNumber());
        }

        return new ErrorDetails(prefix + ": " + safeMessage(exception.getMessage(), exception.toString()), null, null);
    }

    private String safeMessage(String preferred, String fallback) {
        String normalizedPreferred = preferred == null ? "" : preferred.trim();
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        return fallback == null ? "Errore sconosciuto." : fallback.trim();
    }

    private static String appendLineColumn(String message, Integer line, Integer column) {
        if (line == null || line <= 0) {
            return message;
        }
        if (column != null && column > 0) {
            return message + System.lineSeparator() + "Riga: " + line + ", Colonna: " + column;
        }
        return message + System.lineSeparator() + "Riga: " + line;
    }

    private record ErrorDetails(String message, Integer line, Integer column) {}

    private static final class VariableBuilder {
        private final String name;
        private String sampleValue;
        private final Map<String, VariableBuilder> children = new LinkedHashMap<>();

        private VariableBuilder(String name) {
            this.name = name;
        }

        private void appendPath(String rawPath, String value) {
            if (rawPath == null || rawPath.isBlank()) {
                return;
            }

            String[] tokens = rawPath.split("\\.");
            VariableBuilder current = this;
            for (int i = 0; i < tokens.length; i++) {
                String part = tokens[i].trim();
                if (part.isBlank()) {
                    continue;
                }

                current = current.children.computeIfAbsent(part, VariableBuilder::new);
                if (i == tokens.length - 1) {
                    current.sampleValue = value;
                }
            }
        }

        private List<VariableNode> toNodes() {
            List<VariableNode> out = new ArrayList<>();
            for (VariableBuilder child : children.values()) {
                out.add(new VariableNode(child.name, child.sampleValue, child.toNodes()));
            }
            return out;
        }
    }
}
