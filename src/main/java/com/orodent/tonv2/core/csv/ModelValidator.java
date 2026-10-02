package com.orodent.tonv2.core.csv;

import java.util.List;

@FunctionalInterface
public interface ModelValidator<T> {
    List<ValidationError> validate(T value);

    record ValidationError(String field, String message) {
        public ValidationError {
            field = field == null ? "" : field;
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Validation error message is required.");
            }
        }
    }
}
