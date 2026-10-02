package com.orodent.tonv2.core.csv;

import java.util.List;

public record CsvImportResult<T>(
        int totalRows,
        List<ImportedRow<T>> validRows,
        List<RowError> rowErrors,
        List<DocumentError> documentErrors
) {
    public CsvImportResult {
        validRows = List.copyOf(validRows);
        rowErrors = List.copyOf(rowErrors);
        documentErrors = List.copyOf(documentErrors);
    }

    public boolean valid() {
        return documentErrors.isEmpty() && rowErrors.isEmpty();
    }

    public record ImportedRow<T>(int lineNumber, T value) {}
    public record RowError(int lineNumber, String field, String rawValue, String message) {}
    public record DocumentError(int lineNumber, String message) {}
}
