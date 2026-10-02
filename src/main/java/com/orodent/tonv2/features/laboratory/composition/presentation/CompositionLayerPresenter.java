package com.orodent.tonv2.features.laboratory.composition.presentation;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.composition.service.LayerMetricsService;

import java.util.List;
import java.util.Locale;

public final class CompositionLayerPresenter {
    private final LayerMetricsService metricsService;

    public CompositionLayerPresenter(LayerMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public CompositionLayerViewState present(LayerDraft layer, List<Powder> availablePowders) {
        double total = layer.ingredients().stream()
                .mapToDouble(IngredientDraft::percentage)
                .filter(value -> value > 0)
                .sum();
        LayerMetricsService.LayerMetrics metrics = metricsService.calculate(layer, availablePowders);

        String metricsText = "Traslucenza: " + formatDecimal(metrics.weightedTranslucency(), 2)
                + "\nResistenza: " + formatDecimal(metrics.weightedStrength(), 0) + " MPa"
                + "\nIttria: " + metrics.yttriaSummary();

        return new CompositionLayerViewState(
                layer.layerNumber(),
                layer.ingredients().size(),
                total,
                Math.abs(total - 100.0) < 0.0001,
                metricsText
        );
    }

    private String formatDecimal(Double value, int decimals) {
        if (value == null) {
            return "n/d";
        }
        return String.format(Locale.US, "%1$." + decimals + "f", value);
    }
}
