package com.orodent.tonv2.core.documents.template;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentTemplateService {
    private static final Pattern EACH_BLOCK_PATTERN = Pattern.compile("(?s)\\{\\{#each\\s+([\\w.]+)\\s*}}(.*?)\\{\\{/each}}", Pattern.MULTILINE);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}", Pattern.MULTILINE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");

    private final Gson gson;

    public DocumentTemplateService() {
        this.gson = new Gson();
    }

    public Map<String, Object> parseParameters(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        JsonObject root = gson.fromJson(json, JsonObject.class);
        if (root == null) {
            return Collections.emptyMap();
        }

        return jsonObjectToMap(root);
    }

    public TemplateRenderResult render(String templateBody, Map<String, Object> parameters) {
        List<String> warnings = new ArrayList<>();
        String expandedLoops = expandEachBlocks(templateBody == null ? "" : templateBody, parameters, warnings);
        String resolvedMarkup = replacePlaceholders(expandedLoops, parameters, warnings);
        String html = markupToHtml(resolvedMarkup);
        return new TemplateRenderResult(resolvedMarkup, html, warnings);
    }

    private String expandEachBlocks(String templateBody, Map<String, Object> parameters, List<String> warnings) {
        Matcher matcher = EACH_BLOCK_PATTERN.matcher(templateBody);
        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            String collectionPath = matcher.group(1);
            String blockBody = matcher.group(2);
            Object value = resolvePath(parameters, collectionPath);
            String normalizedBlockBody = normalizeEachBlockBody(blockBody);

            String replacement = "";
            if (value instanceof List<?> list) {
                StringBuilder repeated = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    Object element = list.get(i);
                    Map<String, Object> scope = new HashMap<>(parameters);
                    scope.put("index", i + 1);
                    if (element instanceof Map<?, ?> elementMap) {
                        for (Map.Entry<?, ?> entry : elementMap.entrySet()) {
                            if (entry.getKey() instanceof String key) {
                                scope.put(key, entry.getValue());
                            }
                        }
                    } else {
                        scope.put("value", element);
                    }
                    repeated.append(replacePlaceholders(normalizedBlockBody, scope, warnings));
                    if (!repeated.isEmpty() && repeated.charAt(repeated.length() - 1) != '\n') {
                        repeated.append('\n');
                    }
                }
                replacement = repeated.toString();
            } else {
                warnings.add("Blocco each ignorato: percorso non-lista '" + collectionPath + "'.");
            }

            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private String normalizeEachBlockBody(String blockBody) {
        if (blockBody == null || blockBody.isEmpty()) {
            return "";
        }

        String normalized = blockBody;
        if (normalized.startsWith("\r\n")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("\n") || normalized.startsWith("\r")) {
            normalized = normalized.substring(1);
        }

        if (normalized.endsWith("\r\n")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith("\n") || normalized.endsWith("\r")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String replacePlaceholders(String body, Map<String, Object> scope, List<String> warnings) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(body);
        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = resolvePath(scope, path);
            if (value == null) {
                warnings.add("Placeholder senza valore: {{" + path + "}}");
                matcher.appendReplacement(output, "");
            } else {
                matcher.appendReplacement(output, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }

        matcher.appendTail(output);
        return output.toString();
    }

    public String markupToHtml(String markup) {
        String[] lines = markup.split("\\R", -1);
        StringBuilder html = new StringBuilder();
        html.append("<html>\n");
        html.append("  <head>\n");
        html.append("    <meta charset='UTF-8' />\n");
        html.append("  </head>\n");
        html.append("  <body style='font-family:Segoe UI,Roboto,sans-serif;'>\n");

        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (line.trim().equals("---")) {
                html.append("    <hr />\n");
                i++;
                continue;
            }

            if (isTableLine(line)) {
                List<String> tableLines = new ArrayList<>();
                while (i < lines.length && isTableLine(lines[i])) {
                    tableLines.add(lines[i]);
                    i++;
                }
                html.append(renderTable(tableLines));
                continue;
            }

            if (!line.isBlank()) {
                html
                        .append("    <p>")
                        .append(applyInlineFormatting(escapeHtml(line)))
                        .append("</p>\n");
            }
            i++;
        }

        html.append("  </body>\n");
        html.append("</html>");
        return html.toString();
    }

    private String renderTable(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("    <table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>\n");

        List<String> header = parseTableRow(lines.get(0));
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        for (String cell : header) {
            html
                    .append("          <th>")
                    .append(applyInlineFormatting(escapeHtml(cell)))
                    .append("</th>\n");
        }
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        int startRow = 1;
        if (lines.size() > 1 && isSeparatorRow(lines.get(1))) {
            startRow = 2;
        }

        for (int i = startRow; i < lines.size(); i++) {
            if (isSeparatorRow(lines.get(i))) {
                continue;
            }
            html.append("        <tr>\n");
            for (String cell : parseTableRow(lines.get(i))) {
                html
                        .append("          <td>")
                        .append(applyInlineFormatting(escapeHtml(cell)))
                        .append("</td>\n");
            }
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        return html.toString();
    }

    private boolean isTableLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|");
    }

    private boolean isSeparatorRow(String line) {
        String normalized = line.replace(" ", "").trim();
        return normalized.matches("\\|[-:]+(\\|[-:]+)+\\|");
    }

    private List<String> parseTableRow(String row) {
        String trimmed = row.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }

        String content = trimmed.substring(1, trimmed.length() - 1);
        String[] cells = content.split("\\|");
        List<String> parsed = new ArrayList<>();
        for (String cell : cells) {
            parsed.add(cell.trim());
        }
        return parsed;
    }

    private String applyInlineFormatting(String line) {
        Matcher matcher = BOLD_PATTERN.matcher(line);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(output, "<strong>" + matcher.group(1) + "</strong>");
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private Object resolvePath(Map<String, Object> scope, String path) {
        String[] tokens = path.split("\\.");
        Object current = scope;

        for (String token : tokens) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(token);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private Map<String, Object> jsonObjectToMap(JsonObject object) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            map.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return map;
    }

    private Object toJavaValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            }
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber();
            }
            return element.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            element.getAsJsonArray().forEach(item -> list.add(toJavaValue(item)));
            return list;
        }
        return jsonObjectToMap(element.getAsJsonObject());
    }

    public String defaultTemplate() {
        return "**Scheda Lavorazione**\n" +
                "---\n" +
                "Articolo: {{item.code}}\n" +
                "Lotto: {{lot.code}}\n" +
                "\n" +
                "| Fase | Materiale | Quantità |\n" +
                "|------|-----------|----------|\n" +
                "{{#each query.layers}}\n" +
                "| {{index}} | {{name}} | {{thickness}} mm |\n" +
                "{{/each}}\n";
    }

    public List<String> availableParameterSources() {
        return List.of("Produzione batch", "Generico");
    }

    public String parametersJsonForSource(String source) {
        if ("Produzione batch".equals(source)) {
            return """
                    {
                      "line": {"name": "Linea A"},
                      "notes": "Note di esempio",
                      "items": [
                        {"code": "ITEM-001", "quantity": 12},
                        {"code": "ITEM-002", "quantity": 5}
                      ]
                    }
                    """;
        }
        return """
                {
                }
                """;
    }

    public String defaultParametersJson() {
        return parametersJsonForSource("Generico");
    }

    private String escapeHtml(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
