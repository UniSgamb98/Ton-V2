package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.Line;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preset params builder for document templates.
 * <p>
 * This is intentionally lightweight and focused on the JSON contract used by the Template Editor presets.
 */
public class BatchProductionDocumentParamsService {

    public Map<String, Object> buildParams(Line line,
                                           String notes,
                                           List<BatchProductionService.ProductionPlanLine> planLines) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("line", Map.of("name", line == null || line.name() == null ? "" : line.name()));
        params.put("notes", notes == null ? "" : notes);
        params.put("items", buildItems(planLines));

        if (planLines == null || planLines.isEmpty()) {
            params.put("composition", Map.of());
            params.put("blank_model", Map.of());
            params.put("composition_layers", List.of());
            return params;
        }

        int compositionId = planLines.getFirst().compositionId();
        params.put("composition", Map.of(
                "id", compositionId,
                "version", 1,
                "num_layers", 1
        ));

        params.put("blank_model", Map.of(
                "code", "BM-STD",
                "pressure_kg_cm2", 100,
                "grams_per_mm", 2.1,
                "diameter_mm", 98,
                "superior_overmaterial_default_mm", 0.2,
                "inferior_overmaterial_default_mm", 0.2,
                "layers", List.of(Map.of("layer_number", 1, "disk_percentage", 100))
        ));

        params.put("composition_layers", List.of(
                Map.of(
                        "layer_number", 1,
                        "ingredients", List.of(
                                Map.of("percentage", 60, "powder", Map.of("id", 1, "code", "PW-A")),
                                Map.of("percentage", 40, "powder", Map.of("id", 2, "code", "PW-B"))
                        )
                )
        ));

        return params;
    }

    public Map<String, Object> buildSamplePreset() {
        List<BatchProductionService.ProductionPlanLine> planLines = List.of(
                new BatchProductionService.ProductionPlanLine(
                        new com.orodent.tonv2.core.database.model.Item(1, "A01", 10, 7, 12.5),
                        3,
                        7
                )
        );
        return buildParams(new Line(1, "Linea A", 10), "Note batch", planLines);
    }

    private List<Map<String, Object>> buildItems(List<BatchProductionService.ProductionPlanLine> planLines) {
        if (planLines == null) {
            return List.of();
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (BatchProductionService.ProductionPlanLine line : planLines) {
            items.add(Map.of(
                    "code", line.item().code(),
                    "quantity", line.quantity(),
                    "height_mm", line.item().heightMm()
            ));
        }
        return items;
    }
}
