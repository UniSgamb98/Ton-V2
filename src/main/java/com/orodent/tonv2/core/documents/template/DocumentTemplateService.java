package com.orodent.tonv2.core.documents.template;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentTemplateService {
    private static final Pattern EACH_OPEN_PATTERN = Pattern.compile("\\{\\{#each\\s+([\\w.\\[\\]]+)(?:\\s+([A-Za-z_][A-Za-z0-9_]*))?\\s*}}", Pattern.MULTILINE);
    private static final Pattern EACH_CLOSE_PATTERN = Pattern.compile("\\{\\{/each}}", Pattern.MULTILINE);
    private static final Pattern HEAD_OPEN_PATTERN = Pattern.compile("\\{\\{head}}", Pattern.MULTILINE);
    private static final Pattern HEAD_CLOSE_PATTERN = Pattern.compile("\\{\\{/head}}", Pattern.MULTILINE);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.\\[\\]]+)\\s*}}", Pattern.MULTILINE);
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("^(.*?)\\s+as\\s+([A-Za-z_][A-Za-z0-9_]*)$");
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
        Map<String, Object> mutableParameters = new HashMap<>(parameters == null ? Collections.emptyMap() : parameters);
        List<String> warnings = new ArrayList<>();
        String resolvedMarkup = renderTemplateBody(templateBody == null ? "" : templateBody, mutableParameters, warnings);
        String html = markupToHtml(resolvedMarkup);
        return new TemplateRenderResult(resolvedMarkup, html, warnings);
    }

    private String renderTemplateBody(String templateBody, Map<String, Object> parameters, List<String> warnings) {
        String withoutHeadBlocks = processHeadBlocks(templateBody, parameters, warnings);
        String expandedLoops = expandEachBlocks(withoutHeadBlocks, parameters, warnings);
        return replacePlaceholders(expandedLoops, parameters, warnings);
    }

    private String processHeadBlocks(String templateBody, Map<String, Object> parameters, List<String> warnings) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;

        while (cursor < templateBody.length()) {
            Matcher openMatcher = HEAD_OPEN_PATTERN.matcher(templateBody);
            if (!openMatcher.find(cursor)) {
                output.append(templateBody.substring(cursor));
                break;
            }

            output.append(templateBody, cursor, openMatcher.start());

            Matcher closeMatcher = HEAD_CLOSE_PATTERN.matcher(templateBody);
            if (!closeMatcher.find(openMatcher.end())) {
                warnings.add("Blocco head non chiuso correttamente.");
                break;
            }

            String headBody = templateBody.substring(openMatcher.end(), closeMatcher.start());
            renderTemplateBody(headBody, parameters, warnings);
            cursor = closeMatcher.end();
        }

        return output.toString();
    }

    private String expandEachBlocks(String templateBody, Map<String, Object> parameters, List<String> warnings) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;

        while (cursor < templateBody.length()) {
            Matcher openMatcher = EACH_OPEN_PATTERN.matcher(templateBody);
            if (!openMatcher.find(cursor)) {
                output.append(templateBody.substring(cursor));
                break;
            }

            output.append(templateBody, cursor, openMatcher.start());

            EachBlockMatch blockMatch = findMatchingEachBlock(templateBody, openMatcher);
            if (blockMatch == null) {
                warnings.add("Blocco each non chiuso correttamente.");
                output.append(templateBody.substring(openMatcher.start()));
                break;
            }

            Object value = resolvePath(parameters, blockMatch.collectionPath());
            String normalizedBlockBody = normalizeEachBlockBody(blockMatch.blockBody());

            if (value instanceof List<?> list) {
                StringBuilder repeated = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    Object element = list.get(i);
                    Map<String, Object> scope = new HashMap<>(parameters);
                    scope.put("index", i + 1);
                    if (blockMatch.alias() != null && !blockMatch.alias().isBlank()) {
                        scope.put(blockMatch.alias(), i);
                    }
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
                output.append(repeated);
            } else {
                warnings.add("Blocco each ignorato: percorso non-lista '" + blockMatch.collectionPath() + "'.");
            }

            cursor = blockMatch.afterEnd();
        }

        return output.toString();
    }

    private EachBlockMatch findMatchingEachBlock(String templateBody, Matcher firstOpenMatcher) {
        int depth = 1;
        int searchFrom = firstOpenMatcher.end();

        while (searchFrom < templateBody.length()) {
            Matcher nextOpen = EACH_OPEN_PATTERN.matcher(templateBody);
            boolean foundOpen = nextOpen.find(searchFrom);

            Matcher nextClose = EACH_CLOSE_PATTERN.matcher(templateBody);
            boolean foundClose = nextClose.find(searchFrom);

            if (!foundClose) {
                return null;
            }

            if (foundOpen && nextOpen.start() < nextClose.start()) {
                depth++;
                searchFrom = nextOpen.end();
                continue;
            }

            depth--;
            if (depth == 0) {
                return new EachBlockMatch(
                        firstOpenMatcher.group(1),
                        firstOpenMatcher.group(2),
                        templateBody.substring(firstOpenMatcher.end(), nextClose.start()),
                        nextClose.end()
                );
            }
            searchFrom = nextClose.end();
        }

        return null;
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
        String bodyWithAssignments = replaceAssignments(bodyWithMath, scope, warnings);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(bodyWithAssignments);
        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = resolvePath(scope, path);
            if (value == null) {
                warnings.add("Placeholder senza valore: {{" + path + "}}");
                matcher.appendReplacement(output, "");
            } else {
                matcher.appendReplacement(output, Matcher.quoteReplacement(formatRenderedValue(value)));
            }
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private String replaceMathExpressions(String body, Map<String, Object> scope, List<String> warnings) {
        return replaceCustomTags(body, scope, warnings, "math", rawContent -> {
            MathExpression mathExpression = parseMathExpression(rawContent);
            double result = evaluateMathExpression(interpolateInlinePlaceholders(mathExpression.expression(), scope, warnings), scope);
            Object numericResult = normalizeNumericValue(result);
            if (mathExpression.alias() != null && !mathExpression.alias().isBlank()) {
                scope.put(mathExpression.alias(), numericResult);
            }
            return formatRenderedValue(numericResult);
        }, "Espressione math non valida: {{math %s}} (%s)");
    }

    private String replaceAssignments(String body, Map<String, Object> scope, List<String> warnings) {
        return replaceCustomTags(body, scope, warnings, null, rawContent -> {
            Matcher assignmentMatcher = ASSIGNMENT_PATTERN.matcher(rawContent);
            if (!assignmentMatcher.matches()) {
                return null;
            }

            String expression = assignmentMatcher.group(1) == null ? "" : assignmentMatcher.group(1).trim();
            String alias = assignmentMatcher.group(2);
            if (expression.isBlank() || alias == null || alias.isBlank()) {
                return null;
            }

            Object value = resolveAssignmentValue(expression, scope, warnings);
            scope.put(alias, value);
            return formatRenderedValue(value);
        }, "Assegnazione non valida: {{%s}} (%s)");
    }

    private String replaceCustomTags(String body,
                                     Map<String, Object> scope,
                                     List<String> warnings,
                                     String requiredPrefix,
                                     TagResolver resolver,
                                     String warningTemplate) {
        StringBuilder output = new StringBuilder();
        int cursor = 0;

        while (cursor < body.length()) {
            int start = body.indexOf("{{", cursor);
            if (start < 0) {
                output.append(body.substring(cursor));
                break;
            }

            output.append(body, cursor, start);
            int tagEnd = findTagEnd(body, start);
            if (tagEnd < 0) {
                output.append(body.substring(start));
                break;
            }

            String rawTag = body.substring(start + 2, tagEnd).trim();
            String tagBody = rawTag;
            if (requiredPrefix != null) {
                if (!rawTag.startsWith(requiredPrefix) || (rawTag.length() > requiredPrefix.length() && !Character.isWhitespace(rawTag.charAt(requiredPrefix.length())))) {
                    output.append(body, start, tagEnd + 2);
                    cursor = tagEnd + 2;
                    continue;
                }
                tagBody = rawTag.substring(requiredPrefix.length()).trim();
            } else if (isNonAssignmentTag(rawTag)) {
                output.append(body, start, tagEnd + 2);
                cursor = tagEnd + 2;
                continue;
            }

            try {
                String replacement = resolver.resolve(tagBody);
                if (replacement == null) {
                    output.append(body, start, tagEnd + 2);
                } else {
                    output.append(replacement);
                }
            } catch (IllegalArgumentException ex) {
                warnings.add(warningTemplate.formatted(tagBody, ex.getMessage()));
            }

            cursor = tagEnd + 2;
        }

        return output.toString();
    }

    private int findTagEnd(String body, int start) {
        int depth = 1;
        int cursor = start + 2;

        while (cursor < body.length() - 1) {
            if (body.startsWith("{{", cursor)) {
                depth++;
                cursor += 2;
                continue;
            }
            if (body.startsWith("}}", cursor)) {
                depth--;
                if (depth == 0) {
                    return cursor;
                }
                cursor += 2;
                continue;
            }
            cursor++;
        }

        return -1;
    }

    private boolean isNonAssignmentTag(String rawTag) {
        return rawTag.startsWith("#")
                || rawTag.startsWith("/")
                || rawTag.startsWith("head")
                || rawTag.startsWith("math ")
                || rawTag.equals("math")
                || rawTag.startsWith("#each")
                || rawTag.startsWith("#columns")
                || rawTag.startsWith("#column");
    }

    private String interpolateInlinePlaceholders(String expression, Map<String, Object> scope, List<String> warnings) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(expression == null ? "" : expression);
        StringBuilder output = new StringBuilder();

        while (matcher.find()) {
            String path = matcher.group(1);
            Object value = resolvePath(scope, path);
            if (value == null) {
                warnings.add("Placeholder senza valore: {{" + path + "}}");
                matcher.appendReplacement(output, "");
            } else {
                matcher.appendReplacement(output, Matcher.quoteReplacement(formatExpressionValue(value)));
            }
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private Object resolveAssignmentValue(String expression, Map<String, Object> scope, List<String> warnings) {
        String resolvedExpression = interpolateInlinePlaceholders(expression, scope, warnings).trim();
        if (resolvedExpression.isBlank()) {
            throw new IllegalArgumentException("Espressione vuota");
        }

        Object pathValue = resolvePath(scope, resolvedExpression);
        if (pathValue != null) {
            return pathValue;
        }

        try {
            return normalizeNumericValue(new BigDecimal(resolvedExpression));
        } catch (NumberFormatException ignored) {
            return resolvedExpression;
        }
    }

    private MathExpression parseMathExpression(String rawExpression) {
        Matcher aliasMatcher = Pattern.compile("^(.*?)(?:\\s+as\\s+([A-Za-z_][A-Za-z0-9_]*))?$").matcher(rawExpression);
        if (!aliasMatcher.matches()) {
            return new MathExpression(rawExpression, null);
        }

        String expressionPart = aliasMatcher.group(1) == null ? "" : aliasMatcher.group(1).trim();
        String alias = aliasMatcher.group(2);
        return new MathExpression(expressionPart, alias);
    }

    private double evaluateMathExpression(String expression, Map<String, Object> scope) {
        String trimmed = expression == null ? "" : expression.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Espressione vuota");
        }

        String[] parts = trimmed.split("\\s+");
        if (isLegacyOperation(parts[0])) {
            return evaluateLegacyOperation(parts, scope);
        }

        ExpressionParser parser = new ExpressionParser(trimmed, scope);
        double result = parser.parseExpression();
        parser.ensureFullyConsumed();
        return result;
    }

    private boolean isLegacyOperation(String token) {
        return "add".equals(token) || "sub".equals(token) || "mul".equals(token)
                || "div".equals(token) || "sqrt".equals(token) || "pow".equals(token);
    }

    private double evaluateLegacyOperation(String[] args, Map<String, Object> scope) {
        String operation = args[0];
        return switch (operation) {
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
                if (args.length != 2) {
                    throw new IllegalArgumentException("Argomenti attesi per sqrt: 1");
                }
                double value = resolveNumberToken(args, 1, scope);
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
    }

    private double[] requireArgs(String[] args, int expected, String operation, Map<String, Object> scope) {
        if (args.length != expected + 1) {
            throw new IllegalArgumentException("Argomenti attesi per " + operation + ": " + expected);
        }
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            values[i] = resolveNumberToken(args, i + 1, scope);
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

    private Object normalizeNumericValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        return normalizeNumericValue(BigDecimal.valueOf(value));
    }

    private Object normalizeNumericValue(BigDecimal value) {
        BigDecimal decimal = value.stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0) : decimal;
    }

    private String formatRenderedValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ITALY);
            formatter.setGroupingUsed(true);
            formatter.setMaximumFractionDigits(340);
            return formatter.format(number);
        }
        return String.valueOf(value);
    }

    private String formatExpressionValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            BigDecimal normalized = decimal.stripTrailingZeros();
            return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
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

        int bodyStartIndex = 0;
        if (lines.size() > 1 && isSeparatorRow(lines.get(1))) {
            html.append(baseIndent).append("  <thead>\n");
            appendTableRow(html, parseTableRow(lines.getFirst()), baseIndent, "    ", true);
            html.append(baseIndent).append("  </thead>\n");
            bodyStartIndex = 2;
        }

        html.append(baseIndent).append("  <tbody>\n");
        for (int i = bodyStartIndex; i < lines.size(); i++) {
            if (isSeparatorRow(lines.get(i))) {
                continue;
            }

            boolean headerStyleRow = i + 1 < lines.size() && isSeparatorRow(lines.get(i + 1));
            appendTableRow(html, parseTableRow(lines.get(i)), baseIndent, "    ", headerStyleRow);
        }

        html.append(baseIndent).append("  </tbody>\n");
        html.append(baseIndent).append("</table>\n");
        return html.toString();
    }

    private void appendTableRow(StringBuilder html,
                                List<String> cells,
                                String baseIndent,
                                String rowIndent,
                                boolean headerStyleRow) {
        html.append(baseIndent).append(rowIndent).append("<tr>\n");
        String cellTag = headerStyleRow ? "th" : "td";
        String extraStyle = headerStyleRow ? " style='font-weight:700;background:#f5f5f5;'" : "";

        for (String cell : cells) {
            html.append(baseIndent)
                    .append(rowIndent)
                    .append("  <")
                    .append(cellTag)
                    .append(extraStyle)
                    .append(">")
                    .append(applyInlineFormatting(escapeHtml(cell)))
                    .append("</")
                    .append(cellTag)
                    .append(">\n");
        }

        html.append(baseIndent).append(rowIndent).append("</tr>\n");
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
        Object current = scope;
        for (String token : path.split("\\.")) {
            String baseToken = token;
            int bracketStart = token.indexOf('[');
            if (bracketStart >= 0) {
                baseToken = token.substring(0, bracketStart);
            }

            if (!baseToken.isBlank()) {
                if (current instanceof Map<?, ?> map) {
                    current = map.get(baseToken);
                } else {
                    return null;
                }
                if (current == null) {
                    return null;
                }
            }

            int cursor = bracketStart;
            while (cursor >= 0 && cursor < token.length()) {
                int bracketEnd = token.indexOf(']', cursor);
                if (bracketEnd < 0) {
                    return null;
                }

                String indexToken = token.substring(cursor + 1, bracketEnd).trim();
                Integer index = resolveIndex(scope, indexToken);
                if (index == null || index < 0) {
                    return null;
                }

                if (current instanceof List<?> list) {
                    if (index >= list.size()) {
                        return null;
                    }
                    current = list.get(index);
                } else {
                    return null;
                }

                cursor = token.indexOf('[', bracketEnd + 1);
            }
        }

        return current;
    }

    private Integer resolveIndex(Map<String, Object> scope, String token) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ignored) {
            Object value = resolvePath(scope, token);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String stringValue) {
                try {
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
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
                        "version": 7,
                        "num_layers": 4
                      },
                      "blank_model": {
                        "code": "BM-98-A",
                        "pressure_kg_cm2": 2300,
                        "grams_per_mm": 0.55,
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
                        {"code": "ITEM-001", "quantity": 12, "height_mm": 18.5},
                        {"code": "ITEM-002", "quantity": 5, "height_mm": 22.0}
                      ],
                      "calculated_example": "{{head}}\n{{math mul 3 6 as pippo}}\n{{/head}}\nValore: {{pippo}}"
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

    @FunctionalInterface
    private interface TagResolver {
        String resolve(String rawContent);
    }

    private record EachBlockMatch(String collectionPath, String alias, String blockBody, int afterEnd) {}

    private record MathExpression(String expression, String alias) {}

    private final class ExpressionParser {
        private final String expression;
        private final Map<String, Object> scope;
        private int position;

        private ExpressionParser(String expression, Map<String, Object> scope) {
            this.expression = expression;
            this.scope = scope;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parsePower();
                } else if (match('/')) {
                    double divisor = parsePower();
                    if (divisor == 0d) {
                        throw new IllegalArgumentException("Divisione per zero");
                    }
                    value /= divisor;
                } else {
                    return value;
                }
            }
        }

        private double parsePower() {
            double base = parseUnary();
            skipWhitespace();
            if (match('^')) {
                return Math.pow(base, parsePower());
            }
            return base;
        }

        private double parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-')) {
                return -parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                double value = parseExpression();
                skipWhitespace();
                expect(')');
                return value;
            }

            if (peekIdentifierStart()) {
                String identifier = parseIdentifier();
                skipWhitespace();
                if ("sqrt".equals(identifier) && match('(')) {
                    double value = parseExpression();
                    skipWhitespace();
                    expect(')');
                    if (value < 0d) {
                        throw new IllegalArgumentException("Radice quadrata di numero negativo");
                    }
                    return Math.sqrt(value);
                }
                if ("pow".equals(identifier) && match('(')) {
                    double left = parseExpression();
                    skipWhitespace();
                    expect(',');
                    double right = parseExpression();
                    skipWhitespace();
                    expect(')');
                    return Math.pow(left, right);
                }

                Object value = resolvePath(scope, identifier);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                if (value instanceof String stringValue) {
                    try {
                        return Double.parseDouble(stringValue);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Valore non numerico per percorso '" + identifier + "'");
                    }
                }
                throw new IllegalArgumentException("Percorso numerico non trovato: '" + identifier + "'");
            }

            return parseNumber();
        }

        private double parseNumber() {
            skipWhitespace();
            int start = position;
            boolean dotSeen = false;

            while (position < expression.length()) {
                char current = expression.charAt(position);
                if (Character.isDigit(current)) {
                    position++;
                } else if (current == '.' && !dotSeen) {
                    dotSeen = true;
                    position++;
                } else {
                    break;
                }
            }

            if (start == position) {
                throw new IllegalArgumentException("Numero atteso alla posizione " + position);
            }

            return Double.parseDouble(expression.substring(start, position));
        }

        private String parseIdentifier() {
            int start = position;
            while (position < expression.length()) {
                char current = expression.charAt(position);
                if (Character.isLetterOrDigit(current) || current == '_' || current == '.' || current == '[' || current == ']') {
                    position++;
                } else {
                    break;
                }
            }
            return expression.substring(start, position);
        }

        private void ensureFullyConsumed() {
            skipWhitespace();
            if (position != expression.length()) {
                throw new IllegalArgumentException("Token inatteso alla posizione " + position);
            }
        }

        private boolean peekIdentifierStart() {
            return position < expression.length()
                    && (Character.isLetter(expression.charAt(position)) || expression.charAt(position) == '_');
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (position < expression.length() && expression.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw new IllegalArgumentException("Atteso '" + expected + "' alla posizione " + position);
            }
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }
    }
}
