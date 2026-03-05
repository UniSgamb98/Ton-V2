package com.orodent.tonv2.core.database.model;

public record Item(
        int id,
        int productId,
        int blankModelId,
        double heightMm
) {
    public String code() {
        return "P" + productId + "-H" + String.format(java.util.Locale.ROOT, "%.2f", heightMm);
    }

    @Override
    public String toString() {
        return code();
    }
}
