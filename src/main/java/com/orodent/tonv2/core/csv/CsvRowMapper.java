package com.orodent.tonv2.core.csv;

import java.util.Set;

public interface CsvRowMapper<T> {
    T map(CsvRow row);

    default Set<String> requiredColumns() {
        return Set.of();
    }
}
