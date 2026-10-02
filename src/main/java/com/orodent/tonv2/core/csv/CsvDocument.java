package com.orodent.tonv2.core.csv;

import java.util.List;

public record CsvDocument(List<String> headers, List<CsvRow> rows) {
    public CsvDocument {
        headers = List.copyOf(headers);
        rows = List.copyOf(rows);
    }
}
