package com.orodent.tonv2.features.laboratory.diskmodel.presentation;

import java.util.ArrayList;
import java.util.List;

public final class DiskModelEditorState {

    public int parseLayerCount(String rawValue) {
        try {
            return Math.max(0, Integer.parseInt(normalize(rawValue)));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    public List<Double> distributeLayers(int layerCount) {
        if (layerCount <= 0) {
            return List.of();
        }
        double percentage = 100.0 / layerCount;
        List<Double> values = new ArrayList<>(layerCount);
        for (int index = 0; index < layerCount; index++) {
            values.add(percentage);
        }
        return List.copyOf(values);
    }

    public LayerSummary summarizeLayers(List<String> rawPercentages) {
        List<Double> values = rawPercentages.stream().map(this::parseDecimal).toList();
        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        return new LayerSummary(values, total, Math.abs(total - 100.0) < 0.0001);
    }

    public PreviewData createPreview(String rawSuperior, String rawInferior, List<String> rawPercentages) {
        List<Double> percentages = summarizeLayers(rawPercentages).percentages();
        if (percentages.isEmpty()) {
            percentages = List.of(100.0);
        }
        return new PreviewData(parseDecimal(rawSuperior), parseDecimal(rawInferior), percentages);
    }

    private double parseDecimal(String rawValue) {
        try {
            return Double.parseDouble(normalize(rawValue).replace(',', '.'));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record LayerSummary(List<Double> percentages, double total, boolean valid) {
    }

    public record PreviewData(double superiorOvermaterial, double inferiorOvermaterial, List<Double> percentages) {
    }
}
