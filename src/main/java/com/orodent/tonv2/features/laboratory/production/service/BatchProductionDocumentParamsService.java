package com.orodent.tonv2.features.laboratory.production.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.BlankModelLayer;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.repository.BlankModelLayerRepository;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.PowderRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BatchProductionDocumentParamsService {

    private final CompositionRepository compositionRepo;
    private final BlankModelRepository blankModelRepo;
    private final BlankModelLayerRepository blankModelLayerRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final PowderRepository powderRepo;

    public BatchProductionDocumentParamsService(CompositionRepository compositionRepo,
                                                BlankModelRepository blankModelRepo,
                                                BlankModelLayerRepository blankModelLayerRepo,
                                                CompositionLayerIngredientRepository compositionLayerIngredientRepo,
                                                PowderRepository powderRepo) {
        this.compositionRepo = compositionRepo;
        this.blankModelRepo = blankModelRepo;
        this.blankModelLayerRepo = blankModelLayerRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.powderRepo = powderRepo;
    }

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
        Optional<Composition> composition = compositionRepo.findById(compositionId);
        params.put("composition", composition
                .<Map<String, Object>>map(value -> Map.of(
                        "version", value.version(),
                        "num_layers", value.numLayers()
                ))
                .orElseGet(Map::of));

        BlankModel blankModel = resolveBlankModel(compositionId);
        params.put("blank_model", blankModel == null ? Map.of() : buildBlankModel(blankModel));
        params.put("composition_layers", buildCompositionLayers(compositionId));
        return params;
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

    private BlankModel resolveBlankModel(int compositionId) {
        return compositionRepo.findBlankModelIdByCompositionId(compositionId)
                .map(blankModelRepo::findById)
                .orElse(null);
    }

    private Map<String, Object> buildBlankModel(BlankModel blankModel) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", blankModel.code());
        result.put("pressure_kg_cm2", blankModel.pressureKgCm2());
        result.put("grams_per_mm", blankModel.gramsPerMm());
        result.put("diameter_mm", blankModel.diameterMm());
        result.put("superior_overmaterial_default_mm", blankModel.superiorOvermaterialDefaultMm());
        result.put("inferior_overmaterial_default_mm", blankModel.inferiorOvermaterialDefaultMm());

        List<Map<String, Object>> layers = new ArrayList<>();
        for (BlankModelLayer layer : blankModelLayerRepo.findByBlankModelId(blankModel.id())) {
            layers.add(Map.of(
                    "layer_number", layer.layerNumber(),
                    "disk_percentage", layer.diskPercentage()
            ));
        }
        result.put("layers", layers);
        return result;
    }

    private List<Map<String, Object>> buildCompositionLayers(int compositionId) {
        List<CompositionLayerIngredient> ingredients = compositionLayerIngredientRepo.findByCompositionId(compositionId);
        List<Integer> powderIds = ingredients.stream()
                .map(CompositionLayerIngredient::powderId)
                .distinct()
                .toList();
        Map<Integer, Powder> powdersById = powderRepo.findByIds(powderIds).stream()
                .collect(Collectors.toMap(Powder::id, powder -> powder));

        Map<Integer, List<CompositionLayerIngredient>> grouped = new LinkedHashMap<>();
        for (CompositionLayerIngredient ingredient : ingredients) {
            grouped.computeIfAbsent(ingredient.layerNumber(), ignored -> new ArrayList<>()).add(ingredient);
        }

        List<Map<String, Object>> layers = new ArrayList<>();
        for (Map.Entry<Integer, List<CompositionLayerIngredient>> entry : grouped.entrySet()) {
            List<Map<String, Object>> layerIngredients = new ArrayList<>();
            for (CompositionLayerIngredient ingredient : entry.getValue()) {
                Powder powder = powdersById.get(ingredient.powderId());
                layerIngredients.add(Map.of(
                        "percentage", ingredient.percentage(),
                        "powder", Map.of(
                                "id", ingredient.powderId(),
                                "code", powder == null || powder.code() == null ? "" : powder.code()
                        )
                ));
            }

            layers.add(Map.of(
                    "layer_number", entry.getKey(),
                    "ingredients", layerIngredients
            ));
        }

        return layers;
    }
}
