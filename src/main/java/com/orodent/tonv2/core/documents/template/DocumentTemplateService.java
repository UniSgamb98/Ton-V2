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
    private static final Pattern MATH_PATTERN = Pattern.compile("\\{\\{\\s*math\\s+(add|sub|mul|div|sqrt|pow)\\s+([^{}]+?)\\s*}}", Pattern.MULTILINE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern COLUMNS_START_PATTERN = Pattern.compile("\\{\\{#columns\\s+(\\d+)}}\\s*");

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
        String resolvedMarkup = renderTemplateBody(templateBody == null ? "" : templateBody, parameters, warnings);
        String html = markupToHtml(resolvedMarkup);
        return new TemplateRenderResult(resolvedMarkup, html, warnings);
    }

    private String renderTemplateBody(String templateBody, Map<String, Object> parameters, List<String> warnings) {
        String expandedLoops = expandEachBlocks(templateBody, parameters, warnings);
        return replacePlaceholders(expandedLoops, parameters, warnings);
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
                    repeated.append(renderTemplateBody(normalizedBlockBody, scope, warnings));
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
        String bodyWithMath = replaceMathExpressions(body, scope, warnings);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(bodyWithMath);
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

    private String replaceMathExpressions(String body, Map<String, Object> scope, List<String> warnings) {
        Matcher matcher = MATH_PATTERN.matcher(body);
        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            String operation = matcher.group(1);
            String[] args = matcher.group(2).trim().split("\\s+");

            try {
                double result = switch (operation) {
                    case "add" -> {
                        double[] values = requireArgs(args, 2, operation, scope);
                        yield values[0] + values[1];
                    }
                    case "sub" -> {
                        double[] values = requireArgs(args, 2, operation, scope);
                        yield values[0] - values[1];
                    }
                    case "mul" -> {
                        double[] values = requireArgs(args, 2, operation, scope);
                        yield values[0] * values[1];
                    }
                    case "div" -> {
                        double[] values = requireArgs(args, 2, operation, scope);
                        if (values[1] == 0d) {
                            throw new IllegalArgumentException("Divisione per zero");
                        }
                        yield values[0] / values[1];
                    }
                    case "sqrt" -> {
                        double value = resolveNumberToken(args, 0, scope);
                        if (value < 0d) {
                            throw new IllegalArgumentException("Radice quadrata di numero negativo");
                        }
                        yield Math.sqrt(value);
                    }
                    case "pow" -> {
                        double[] values = requireArgs(args, 2, operation, scope);
                        yield Math.pow(values[0], values[1]);
                    }
                    default -> throw new IllegalArgumentException("Operazione non supportata");
                };
                matcher.appendReplacement(output, Matcher.quoteReplacement(formatMathResult(result)));
            } catch (IllegalArgumentException ex) {
                warnings.add("Espressione math non valida: {{math " + operation + " " + String.join(" ", args) + "}} (" + ex.getMessage() + ")");
                matcher.appendReplacement(output, "");
            }
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private double[] requireArgs(String[] args, int expected, String operation, Map<String, Object> scope) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Argomenti attesi per " + operation + ": " + expected);
        }
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            values[i] = resolveNumberToken(args, i, scope);
        }
        return values;
    }

    private double resolveNumberToken(String[] args, int index, Map<String, Object> scope) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Argomento mancante");
        }

        String token = args[index];
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException ignored) {
            Object value = resolvePath(scope, token);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String stringValue) {
                try {
                    return Double.parseDouble(stringValue);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Valore non numerico per percorso '" + token + "'");
                }
            }
            throw new IllegalArgumentException("Percorso numerico non trovato: '" + token + "'");
        }
    }

    private String formatMathResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }

        java.math.BigDecimal decimal = java.math.BigDecimal.valueOf(value).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
    }

    public String markupToHtml(String markup) {
        List<String> lines = List.of(markup.split("\\R", -1));

        return "<html>\n" +
                "  <head>\n" +
                "    <meta charset='UTF-8' />\n" +
                "  </head>\n" +
                "  <body style='font-family:Segoe UI,Roboto,sans-serif;'>\n" +
                renderMarkupLines(lines, "    ") +
                "  </body>\n" +
                "</html>";
    }

    private String renderMarkupLines(List<String> lines, String baseIndent) {
        StringBuilder html = new StringBuilder();
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i);
            Matcher columnsStart = COLUMNS_START_PATTERN.matcher(line.trim());
            if (columnsStart.matches()) {
                int requestedColumns = Integer.parseInt(columnsStart.group(1));

                int end = i + 1;
                while (end < lines.size() && !lines.get(end).trim().equals("{{/columns}}")) {
                    end++;
                }

                List<String> contentBlock = lines.subList(i + 1, end);
                html.append(renderColumnsBlock(contentBlock, Math.max(1, requestedColumns), baseIndent));
                i = (end < lines.size()) ? end + 1 : end;
                continue;
            }

            if (line.trim().equals("---")) {
                html.append(baseIndent).append("<hr />\n");
                i++;
                continue;
            }

            if (isTableLine(line)) {
                List<String> tableLines = new ArrayList<>();
                while (i < lines.size() && isTableLine(lines.get(i))) {
                    tableLines.add(lines.get(i));
                    i++;
                }
                html.append(renderTable(tableLines, baseIndent));
                continue;
            }

            if (!line.isBlank()) {
                html.append(baseIndent)
                        .append("<p>")
                        .append(applyInlineFormatting(escapeHtml(line)))
                        .append("</p>\n");
            }
            i++;
        }

        return html.toString();
    }

    private String renderColumnsBlock(List<String> blockLines, int requestedColumns, String baseIndent) {
        List<List<String>> columns = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : blockLines) {
            if (line.trim().equals("{{#column}}")) {
                columns.add(current);
                current = new ArrayList<>();
            } else {
                current.add(line);
            }
        }
        columns.add(current);

        StringBuilder html = new StringBuilder();
        int gridColumns = Math.max(requestedColumns, columns.size());
        html.append(baseIndent)
                .append("<div style='display:grid;grid-template-columns:repeat(")
                .append(gridColumns)
                .append(", 1fr);gap:16px;'>\n");

        for (List<String> column : columns) {
            html.append(baseIndent).append("  <div>\n");
            html.append(renderMarkupLines(column, baseIndent + "    "));
            html.append(baseIndent).append("  </div>\n");
        }

        html.append(baseIndent).append("</div>\n");
        return html.toString();
    }

    private String renderTable(List<String> lines, String baseIndent) {
        if (lines.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append(baseIndent).append("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse:collapse;width:100%;'>\n");

        List<String> header = parseTableRow(lines.getFirst());
        html.append(baseIndent).append("  <thead>\n");
        html.append(baseIndent).append("    <tr>\n");
        for (String cell : header) {
            html
                    .append(baseIndent).append("      <th>")
                    .append(applyInlineFormatting(escapeHtml(cell)))
                    .append("</th>\n");
        }
        html.append(baseIndent).append("    </tr>\n");
        html.append(baseIndent).append("  </thead>\n");
        html.append(baseIndent).append("  <tbody>\n");

        int startRow = 1;
        if (lines.size() > 1 && isSeparatorRow(lines.get(1))) {
            startRow = 2;
        }

        for (int i = startRow; i < lines.size(); i++) {
            if (isSeparatorRow(lines.get(i))) {
                continue;
            }
            html.append(baseIndent).append("    <tr>\n");
            for (String cell : parseTableRow(lines.get(i))) {
                html
                        .append(baseIndent).append("      <td>")
                        .append(applyInlineFormatting(escapeHtml(cell)))
                        .append("</td>\n");
            }
            html.append(baseIndent).append("    </tr>\n");
        }

        html.append(baseIndent).append("  </tbody>\n");
        html.append(baseIndent).append("</table>\n");
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
        return """
                **Scheda Lavorazione**
                ---
                Articolo: {{item.code}}
                Lotto: {{lot.code}}
                
                | Fase | Materiale | Quantità |
                |------|-----------|----------|
                {{#each query.layers}}
                | {{index}} | {{name}} | {{thickness}} mm |
                {{/each}}
                """;
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
                      "composition": {
                        "id": 42,
                        "version": 7,
                        "num_layers": 4
                      },
                      "blank_model": {
                        "id": 3,
                        "code": "BM-98-A",
                        "pressure_kg_cm2": 2300,
                        "grams_per_mm": 0.55,
                        "num_layers": 4,
                        "diameter_mm": 98.0,
                        "superior_overmaterial_default_mm": 1.2,
                        "inferior_overmaterial_default_mm": 0.7,
                        "layers": [
                          {"layer_number": 1, "disk_percentage": 12.5},
                          {"layer_number": 2, "disk_percentage": 27.5},
                          {"layer_number": 3, "disk_percentage": 30.0},
                          {"layer_number": 4, "disk_percentage": 30.0}
                        ]
                      },
                      "composition_layers": [
                        {
                          "layer_number": 1,
                          "ingredients": [
                            {"percentage": 65.0, "powder": {"id": 10, "code": "PW-A1"}},
                            {"percentage": 35.0, "powder": {"id": 11, "code": "PW-B1"}}
                          ]
                        },
                        {
                          "layer_number": 2,
                          "ingredients": [
                            {"percentage": 100.0, "powder": {"id": 12, "code": "PW-C2"}}
                          ]
                        }
                      ],
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

    private String escapeHtml(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
