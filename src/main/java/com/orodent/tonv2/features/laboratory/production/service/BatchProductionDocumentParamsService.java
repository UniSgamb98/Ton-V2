package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.*;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.PowderRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds document params for batch production templates using repository APIs.
 */
public class BatchProductionDocumentParamsService {

    private final CompositionRepository compositionRepo;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final PowderRepository powderRepo;
    private final ItemRepository itemRepo;
    private final LineRepository lineRepo;

    public BatchProductionDocumentParamsService(CompositionRepository compositionRepo,
                                                BlankModelRepository blankModelRepo,
                                                BlankModelLayerRepository blankModelLayerRepo,
                                                CompositionLayerIngredientRepository compositionLayerIngredientRepo,
                                                PowderRepository powderRepo,
                                                ItemRepository itemRepo,
                                                LineRepository lineRepo) {
        this.compositionRepo = compositionRepo;
        this.blankModelRepo = blankModelRepo;
        this.blankModelLayerRepo = blankModelLayerRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.powderRepo = powderRepo;
        this.itemRepo = itemRepo;
        this.lineRepo = lineRepo;
    }

    public Map<String, Object> buildParams(ParamsRequest request) {
        ResolvedContext context = resolveContext(request);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("line", Map.of("name", context.line() == null || context.line().name() == null ? "" : context.line().name()));
        params.put("notes", context.notes() == null ? "" : context.notes());
        params.put("items", buildItems(context.planLines()));

        if (context.planLines().isEmpty()) {
            params.put("blank_model", Map.of());
            return params;
        }

        int compositionId = context.planLines().getFirst().compositionId();
        int blankModelId = context.planLines().getFirst().item().blankModelId();
        params.put("blank_model", buildBlankModelPayload(blankModelId, compositionId));
        return params;
    }

    private ResolvedContext resolveContext(ParamsRequest request) {
        if (request != null && request.presetMode()) {
            ResolvedContext presetContext = buildPresetContextFromDb(request.presetNotes(), request.presetQuantity());
            if (!presetContext.planLines().isEmpty()) {
                return presetContext;
            }
            return new ResolvedContext(new Line(0, "", 0), "", List.of());
        }

        if (request == null) {
            return new ResolvedContext(new Line(0, "", 0), "", List.of());
        }

        return new ResolvedContext(
                request.line() == null ? new Line(0, "", 0) : request.line(),
                request.notes() == null ? "" : request.notes(),
                request.planLines() == null ? List.of() : request.planLines()
        );
    }

    private ResolvedContext buildPresetContextFromDb(String notes, int quantity) {
        int safeQuantity = Math.max(1, quantity);
        for (Item item : itemRepo.findAll()) {
            Optional<Integer> activeCompositionId = compositionRepo.findActiveCompositionId(item.productId());
            if (activeCompositionId.isEmpty()) {
                continue;
            }

            Line line = lineRepo.findByProductId(item.productId()).stream().findFirst()
                    .orElse(new Line(0, "Linea", item.productId()));

            List<BatchProductionService.ProductionPlanLine> lines = List.of(
                    new BatchProductionService.ProductionPlanLine(item, safeQuantity, activeCompositionId.get())
            );
            return new ResolvedContext(line, notes == null ? "" : notes, lines);
        }
        return new ResolvedContext(new Line(0, "", 0), "", List.of());
    }

    private Map<String, Object> buildBlankModelPayload(int blankModelId, int compositionId) {
        BlankModel blankModel = blankModelRepo.findById(blankModelId);
        if (blankModel == null) {
            return Map.of();
        }

        int version = compositionRepo.findById(compositionId)
                .map(Composition::version)
                .orElse(0);

        List<Map<String, Object>> blankLayers = blankModelLayerRepo.findByBlankModelId(blankModelId).stream()
                .map(layer -> Map.<String, Object>of(
                        "layer_number", layer.layerNumber(),
                        "disk_percentage", layer.diskPercentage()
                ))
                .toList();

        List<Map<String, Object>> compositionLayers = buildCompositionLayers(compositionId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", blankModel.code());
        payload.put("pressure_kg_cm2", blankModel.pressureKgCm2());
        payload.put("grams_per_mm", blankModel.gramsPerMm());
        payload.put("diameter_mm", blankModel.diameterMm());
        payload.put("superior_overmaterial_default_mm", blankModel.superiorOvermaterialDefaultMm());
        payload.put("inferior_overmaterial_default_mm", blankModel.inferiorOvermaterialDefaultMm());
        payload.put("version", version);
        payload.put("layers", mergeBlankAndCompositionLayers(blankLayers, compositionLayers));
        return payload;
    }

    private List<Map<String, Object>> buildCompositionLayers(int compositionId) {
        List<CompositionLayerIngredient> ingredients = compositionLayerIngredientRepo.findByCompositionId(compositionId);

        Map<Integer, List<Map<String, Object>>> groupedIngredients = new LinkedHashMap<>();

        for (CompositionLayerIngredient ingredient : ingredients) {
            Powder powder = powderRepo.findById(ingredient.powderId());

            groupedIngredients
                    .computeIfAbsent(ingredient.layerNumber(), ignored -> new ArrayList<>())
                    .add(Map.of(
                            "powder", Map.of(
                                    "code", powder == null || powder.code() == null ? "" : powder.code()
                            ),
                            "percentage", ingredient.percentage()
                    ));
        }

        List<Map<String, Object>> layers = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : groupedIngredients.entrySet()) {
            layers.add(Map.of(
                    "layer_number", entry.getKey(),
                    "ingredients", entry.getValue()
            ));
        }

        return layers;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mergeBlankAndCompositionLayers(List<Map<String, Object>> blankLayers,
                                                                     List<Map<String, Object>> compositionLayers) {
        Map<Integer, Map<String, Object>> compositionByLayer = new LinkedHashMap<>();
        for (Map<String, Object> compositionLayer : compositionLayers) {
            Object layerNumber = compositionLayer.get("layer_number");
            if (layerNumber instanceof Integer layer) {
                compositionByLayer.put(layer, compositionLayer);
            }
        }

        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> blankLayer : blankLayers) {
            int layerNumber = ((Number) blankLayer.getOrDefault("layer_number", 0)).intValue();
            double diskPercentage = ((Number) blankLayer.getOrDefault("disk_percentage", 0.0)).doubleValue();

            Map<String, Object> compositionLayer = compositionByLayer.get(layerNumber);

            List<Map<String, Object>> compositionIngredients = compositionLayer == null
                    ? List.of()
                    : (List<Map<String, Object>>) compositionLayer.getOrDefault("ingredients", List.of());

            List<Map<String, Object>> ingredients = new ArrayList<>();
            for (Map<String, Object> ingredient : compositionIngredients) {
                ingredients.add(Map.of(
                        "powder", ingredient.get("powder"),
                        "percentage", ingredient.get("percentage")
                ));
            }

            merged.add(Map.of(
                    "layer_number", layerNumber,
                    "disk_percentage", diskPercentage,
                    "ingredients", ingredients
            ));
        }

        return merged;
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

    public record ParamsRequest(Line line,
                                String notes,
                                List<BatchProductionService.ProductionPlanLine> planLines,
                                boolean presetMode,
                                String presetNotes,
                                int presetQuantity) {

        public static ParamsRequest real(Line line, String notes, List<BatchProductionService.ProductionPlanLine> planLines) {
            return new ParamsRequest(line, notes, planLines, false, "", 1);
        }

        public static ParamsRequest preset(String notes, int quantity) {
            return new ParamsRequest(null, "", Collections.emptyList(), true, notes, quantity);
        }
    }

    private record ResolvedContext(Line line, String notes, List<BatchProductionService.ProductionPlanLine> planLines) {}
}
