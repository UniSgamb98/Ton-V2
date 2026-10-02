package com.orodent.tonv2.core.csv;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record CsvRow(int lineNumber, Map<String, String> values) {
    public CsvRow {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("CSV line number must be positive.");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Optional<String> value(String column) {
        return Optional.ofNullable(values.get(column));
    }

    public String requiredValue(String column) {
        if (!values.containsKey(column)) {
            throw new CsvMappingException(column, null, "Required CSV column is missing.");
        }
        return values.get(column);
    }
}
