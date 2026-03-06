package com.orodent.tonv2.core.database.model;

public record Item(
        int id,
        String code,
        int productId,
        int blankModelId,
        double heightMm
) {
    @Override
    public String toString() {
        return code;
    }
}
