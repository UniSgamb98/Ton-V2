package com.orodent.tonv2.core.database.model;

public record Line(int id, String name, int productId) {
    @Override
    public String toString() {
        return name;
    }
}
