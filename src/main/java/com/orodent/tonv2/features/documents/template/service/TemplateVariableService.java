package com.orodent.tonv2.features.documents.template.service;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateVariableService {

    private final Gson gson;

    public TemplateVariableService(Gson gson) {
        this.gson = gson;
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

    public List<VariableNode> mapToTree(Map<String, Object> paramsMap) {
        return mapToVariableNodes(paramsMap);
    }

    public String toJson(Map<String, Object> payload) {
        return gson.toJson(payload == null ? Map.of() : payload);
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

    public record VariableNode(String name, String sampleValue, List<VariableNode> children) {}

    public record QueryVariablesResult(List<VariableNode> variables, String sampleJsonPayload) {}

    private static class VariableBuilder {
        private final String name;
        private String sampleValue;
        private final LinkedHashMap<String, VariableBuilder> children = new LinkedHashMap<>();

        private VariableBuilder(String name) {
            this.name = name;
        }

        private void appendPath(String path, String value) {
            if (path == null || path.isBlank()) {
                return;
            }

            String[] tokens = path.split("\\.");
            VariableBuilder current = this;

            for (String token : tokens) {
                String normalized = token == null ? "" : token.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                current = current.children.computeIfAbsent(normalized, VariableBuilder::new);
            }

            if (current != this) {
                current.sampleValue = value;
            }
        }

        private List<VariableNode> toNodes() {
            List<VariableNode> nodes = new ArrayList<>();
            for (VariableBuilder child : children.values()) {
                nodes.add(child.toNode());
            }
            return nodes;
        }

        private VariableNode toNode() {
            return new VariableNode(name, sampleValue, toNodes());
        }
    }
}
