package com.orodent.tonv2.features.laboratory.production.presentation;

public record BatchProductionViewState(
        String lineText,
        String productText,
        int configuredItems,
        int totalQuantity,
        String documentText,
        String readinessText,
        boolean ready,
        boolean loading
) {
}
