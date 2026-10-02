package com.orodent.tonv2.core.csv;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record CsvReadOptions(
        char delimiter,
        char quote,
        boolean firstRowIsHeader,
        boolean trimValues,
        boolean ignoreEmptyLines,
        Charset charset
) {
    public CsvReadOptions {
        if (delimiter == quote) {
            throw new IllegalArgumentException("Delimiter and quote character must be different.");
        }
        if (delimiter == '\n' || delimiter == '\r' || quote == '\n' || quote == '\r') {
            throw new IllegalArgumentException("Delimiter and quote character cannot be a line break.");
        }
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
    }

    public static CsvReadOptions semicolonSeparated() {
        return new CsvReadOptions(';', '"', true, true, true, StandardCharsets.UTF_8);
    }
}
