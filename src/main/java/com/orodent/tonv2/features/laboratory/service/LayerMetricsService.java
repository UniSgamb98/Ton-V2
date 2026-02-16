package com.orodent.tonv2.features.laboratory.service;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class LayerMetricsService {

    public LayerMetrics calculate(LayerDraft layerDraft, List<Powder> availablePowders) {
        Double weightedTranslucency = weightedAverage(layerDraft, availablePowders, Powder::translucency);
        Double weightedStrength = weightedAverage(layerDraft, availablePowders, Powder::strength);
        String yttriaSummary = buildYttriaSummary(layerDraft, availablePowders);

        return new LayerMetrics(weightedTranslucency, weightedStrength, yttriaSummary);
    }

    private Double weightedAverage(LayerDraft layerDraft, List<Powder> powders, Function<Powder, Double> valueExtractor) {
        double total = 0;
        double totalPct = 0;

        for (IngredientDraft ingredient : layerDraft.ingredients()) {
            if (ingredient.percentage() <= 0) {
                continue;
            }

            Powder powder = findPowderById(powders, ingredient.powderId());
            if (powder == null) {
                continue;
            }

            Double value = valueExtractor.apply(powder);
            if (value == null) {
                continue;
            }

            total += value * ingredient.percentage();
            totalPct += ingredient.percentage();
        }

        if (totalPct <= 0) {
            return null;
        }

        return total / totalPct;
    }

    private String buildYttriaSummary(LayerDraft layerDraft, List<Powder> powders) {
        double totalPct = totalPercentage(layerDraft);
        if (totalPct <= 0) {
            return "Y non definita";
        }

        Map<Integer, Double> yttriaByMoles = new LinkedHashMap<>();

        for (IngredientDraft ingredient : layerDraft.ingredients()) {
            Powder powder = findPowderById(powders, ingredient.powderId());
            if (powder == null || powder.yttria() <= 0 || ingredient.percentage() <= 0) {
                continue;
            }

            yttriaByMoles.merge(powder.yttria(), ingredient.percentage(), Double::sum);
        }

        if (yttriaByMoles.isEmpty()) {
            return "Y non definita";
        }

        return yttriaByMoles.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> String.format(Locale.US, "%dY %.0f%%", entry.getKey(), (entry.getValue() / totalPct) * 100.0))
                .reduce((left, right) -> left + " - " + right)
                .orElse("Y non definita");
    }

    private Powder findPowderById(List<Powder> powders, int powderId) {
        for (Powder powder : powders) {
            if (powder.id() == powderId) {
                return powder;
            }
        }
        return null;
    }

    private double totalPercentage(LayerDraft layerDraft) {
        return layerDraft.ingredients().stream()
                .mapToDouble(IngredientDraft::percentage)
                .filter(value -> value > 0)
                .sum();
    }

    public record LayerMetrics(
            Double weightedTranslucency,
            Double weightedStrength,
            String yttriaSummary
    ) {
    }
}
