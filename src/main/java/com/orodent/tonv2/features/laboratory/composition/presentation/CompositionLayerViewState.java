package com.orodent.tonv2.features.laboratory.composition.presentation;

public record CompositionLayerViewState(
        int layerNumber,
        int ingredientCount,
        double totalPercentage,
        boolean valid,
        String metricsText
) {
    public static CompositionLayerViewState empty(int layerNumber) {
        return new CompositionLayerViewState(
                layerNumber,
                0,
                0,
                false,
                "Configura le polveri per visualizzare le metriche."
        );
    }

    public String statusSymbol() {
        if (valid) {
            return "✓";
        }
        return totalPercentage <= 0 ? "○" : "!";
    }
}
