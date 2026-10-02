package com.orodent.tonv2.core.csv;

import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CsvImporter<T> {
    private final CsvReader reader;
    private final CsvRowMapper<T> mapper;
    private final ModelValidator<T> validator;

    public CsvImporter(CsvReader reader, CsvRowMapper<T> mapper, ModelValidator<T> validator) {
        this.reader = reader;
        this.mapper = mapper;
        this.validator = validator;
    }

    public CsvImportResult<T> importFile(Path path, CsvReadOptions options) {
        try {
            return importDocument(reader.read(path, options));
        } catch (CsvParseException exception) {
            return documentFailure(exception);
        }
    }

    public CsvImportResult<T> importReader(Reader source, CsvReadOptions options) {
        try {
            return importDocument(reader.read(source, options));
        } catch (CsvParseException exception) {
            return documentFailure(exception);
        }
    }

    private CsvImportResult<T> importDocument(CsvDocument document) {
        Set<String> missingColumns = new LinkedHashSet<>(mapper.requiredColumns());
        missingColumns.removeAll(document.headers());
        if (!missingColumns.isEmpty()) {
            return new CsvImportResult<>(document.rows().size(), List.of(), List.of(),
                    List.of(new CsvImportResult.DocumentError(1,
                            "Missing required CSV columns: " + String.join(", ", missingColumns))));
        }

        List<CsvImportResult.ImportedRow<T>> validRows = new ArrayList<>();
        List<CsvImportResult.RowError> rowErrors = new ArrayList<>();
        for (CsvRow row : document.rows()) {
            try {
                T value = mapper.map(row);
                List<ModelValidator.ValidationError> validationErrors = validator.validate(value);
                if (validationErrors.isEmpty()) {
                    validRows.add(new CsvImportResult.ImportedRow<>(row.lineNumber(), value));
                } else {
                    validationErrors.forEach(error -> rowErrors.add(new CsvImportResult.RowError(
                            row.lineNumber(), error.field(), row.values().get(error.field()), error.message())));
                }
            } catch (CsvMappingException exception) {
                rowErrors.add(new CsvImportResult.RowError(row.lineNumber(), exception.column(),
                        exception.rawValue(), exception.getMessage()));
            }
        }
        return new CsvImportResult<>(document.rows().size(), validRows, rowErrors, List.of());
    }

    private CsvImportResult<T> documentFailure(CsvParseException exception) {
        return new CsvImportResult<>(0, List.of(), List.of(),
                List.of(new CsvImportResult.DocumentError(exception.lineNumber(), exception.getMessage())));
    }
}
