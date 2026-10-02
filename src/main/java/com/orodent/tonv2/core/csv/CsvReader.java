package com.orodent.tonv2.core.csv;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CsvReader {

    public CsvDocument read(Path path, CsvReadOptions options) {
        try (Reader reader = Files.newBufferedReader(path, options.charset())) {
            return read(reader, options);
        } catch (IOException exception) {
            throw new CsvParseException(1, "Unable to read CSV file: " + exception.getMessage());
        }
    }

    public CsvDocument read(Reader reader, CsvReadOptions options) {
        if (reader == null) throw new IllegalArgumentException("CSV reader is required.");
        if (options == null) throw new IllegalArgumentException("CSV read options are required.");

        String content;
        try {
            StringBuilder source = new StringBuilder();
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) source.append(buffer, 0, count);
            content = source.toString();
        } catch (IOException exception) {
            throw new CsvParseException(1, "Unable to read CSV content: " + exception.getMessage());
        }

        List<RawRecord> records = parseRecords(stripBom(content), options);
        if (options.ignoreEmptyLines()) {
            records = records.stream().filter(record -> !isEmpty(record.values())).toList();
        }
        if (records.isEmpty()) return new CsvDocument(List.of(), List.of());

        List<String> headers;
        int dataStart;
        if (options.firstRowIsHeader()) {
            headers = normalizeHeaders(records.getFirst(), options.trimValues());
            dataStart = 1;
        } else {
            int columns = records.getFirst().values().size();
            headers = java.util.stream.IntStream.range(0, columns)
                    .mapToObj(index -> "column" + (index + 1)).toList();
            dataStart = 0;
        }

        List<CsvRow> rows = new ArrayList<>();
        for (int index = dataStart; index < records.size(); index++) {
            RawRecord record = records.get(index);
            if (record.values().size() != headers.size()) {
                throw new CsvParseException(record.lineNumber(),
                        "Expected " + headers.size() + " columns but found " + record.values().size() + ".");
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                String value = record.values().get(column);
                values.put(headers.get(column), options.trimValues() ? value.trim() : value);
            }
            rows.add(new CsvRow(record.lineNumber(), values));
        }
        return new CsvDocument(headers, rows);
    }

    private List<RawRecord> parseRecords(String content, CsvReadOptions options) {
        List<RawRecord> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        int line = 1;
        int recordLine = 1;

        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (quoted) {
                if (current == options.quote()) {
                    if (index + 1 < content.length() && content.charAt(index + 1) == options.quote()) {
                        field.append(options.quote());
                        index++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(current);
                    if (current == '\n') line++;
                    else if (current == '\r' && (index + 1 >= content.length() || content.charAt(index + 1) != '\n')) line++;
                }
                continue;
            }

            if (current == options.quote()) {
                if (!field.toString().isBlank() || quoteClosed) {
                    throw new CsvParseException(line, "Unexpected quote in an unquoted field.");
                }
                field.setLength(0);
                quoted = true;
            } else if (current == options.delimiter()) {
                fields.add(field.toString());
                field.setLength(0);
                quoteClosed = false;
            } else if (current == '\n' || current == '\r') {
                fields.add(field.toString());
                records.add(new RawRecord(recordLine, List.copyOf(fields)));
                fields.clear();
                field.setLength(0);
                quoteClosed = false;
                if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') index++;
                line++;
                recordLine = line;
            } else {
                if (quoteClosed && !Character.isWhitespace(current)) {
                    throw new CsvParseException(line, "Unexpected character after a quoted field.");
                }
                if (!quoteClosed) field.append(current);
            }
        }

        if (quoted) throw new CsvParseException(recordLine, "Unclosed quoted field.");
        if (field.length() > 0 || !fields.isEmpty() || (!content.isEmpty() && content.charAt(content.length() - 1) == options.delimiter())) {
            fields.add(field.toString());
            records.add(new RawRecord(recordLine, List.copyOf(fields)));
        }
        return records;
    }

    private List<String> normalizeHeaders(RawRecord header, boolean trim) {
        List<String> headers = header.values().stream().map(value -> trim ? value.trim() : value).toList();
        Set<String> unique = new LinkedHashSet<>();
        for (String value : headers) {
            if (value.isBlank()) throw new CsvParseException(header.lineNumber(), "CSV header contains an empty column name.");
            if (!unique.add(value)) throw new CsvParseException(header.lineNumber(), "Duplicate CSV column: " + value);
        }
        return headers;
    }

    private boolean isEmpty(List<String> values) {
        return values.stream().allMatch(String::isBlank);
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private record RawRecord(int lineNumber, List<String> values) {}
}
