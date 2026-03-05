package com.orodent.tonv2.core.database.model;

public record Product(int id, String code, String description) {
    @Override
    public String toString() {
        if (description == null || description.isBlank()) {
            return code;
        }
        return code + " - " + description;
    }
}
