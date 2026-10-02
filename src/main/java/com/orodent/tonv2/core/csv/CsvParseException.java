package com.orodent.tonv2.core.csv;

public class CsvParseException extends IllegalArgumentException {
    private final int lineNumber;

    public CsvParseException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public int lineNumber() {
        return lineNumber;
    }
}
