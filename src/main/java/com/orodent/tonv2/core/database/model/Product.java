package com.orodent.tonv2.core.database.model;

public record Product(int id, String type, String color) {
    @Override
    public String toString() {
        if (color == null || color.isBlank()) {
            return type;
        }
        return type + " - " + color;
    }
}
