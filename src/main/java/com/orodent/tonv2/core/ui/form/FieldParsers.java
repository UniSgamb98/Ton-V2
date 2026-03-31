package com.orodent.tonv2.core.ui.form;

public final class FieldParsers {

    private FieldParsers() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Double parseDouble(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un numero valido.");
        }
    }

    public static Integer parseInteger(String raw, String fieldName) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " deve essere un intero valido.");
        }
    }
}
