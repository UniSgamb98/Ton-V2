package com.orodent.tonv2.core.csv;

public class CsvMappingException extends IllegalArgumentException {
    private final String column;
    private final String rawValue;

    public CsvMappingException(String column, String rawValue, String message) {
        super(message);
        this.column = column;
        this.rawValue = rawValue;
    }

    public String column() { return column; }
    public String rawValue() { return rawValue; }
}
