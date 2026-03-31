package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.PowderRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class CreateCompositionService {
    private final PowderRepository powderRepo;
    private final CompositionRepository compositionRepo;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepo;
    private final ProductRepository productRepo;
    private final LineRepository lineRepo;
    private final BlankModelRepository blankModelRepo;

    public CreateCompositionService(PowderRepository powderRepo,
                                    CompositionRepository compositionRepo,
                                    CompositionLayerIngredientRepository compositionLayerIngredientRepo,
                                    ProductRepository productRepo,
                                    LineRepository lineRepo,
                                    BlankModelRepository blankModelRepo) {
        this.powderRepo = powderRepo;
        this.compositionRepo = compositionRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.productRepo = productRepo;
        this.lineRepo = lineRepo;
        this.blankModelRepo = blankModelRepo;
    }

    public List<Product> findAllProducts() {
        return productRepo.findAll();
    }

    public List<BlankModel> findAllBlankModels() {
        return blankModelRepo.findAll();
    }

    public List<Powder> findAllPowders() {
        return powderRepo.findAll();
    }

    public List<String> findAllLineNames() {
        return lineRepo.findDistinctNames();
    }


    public List<String> findLineNamesByProductId(int productId) {
        return lineRepo.findByProductId(productId).stream()
                .map(com.orodent.tonv2.core.database.model.Line::name)
                .distinct()
                .toList();
    }

    public Optional<LatestCompositionData> loadLatestComposition(int productId) {
        Optional<Composition> latestComposition = compositionRepo.findLatestByProduct(productId);
        if (latestComposition.isEmpty()) {
            return Optional.empty();
        }

        Composition composition = latestComposition.get();
        Integer blankModelId = compositionRepo.findBlankModelIdByCompositionId(composition.id()).orElse(null);

        Map<Integer, LayerDraft> byLayer = new TreeMap<>();

        for (CompositionLayerIngredient ingredient : compositionLayerIngredientRepo.findByCompositionId(composition.id())) {
            LayerDraft draft = byLayer.computeIfAbsent(ingredient.layerNumber(), LayerDraft::new);
            draft.ingredients().add(new IngredientDraft(
                    ingredient.powderId(),
                    ingredient.percentage()
            ));
        }

        List<LayerDraft> layerDrafts = new ArrayList<>(byLayer.values());
        return Optional.of(new LatestCompositionData(composition.notes(), blankModelId, layerDrafts));
    }

    public void saveComposition(SaveCompositionRequest request) {
        validateRequest(request);

        List<CompositionLayerIngredient> ingredients = new ArrayList<>();
        for (LayerDraft layerDraft : request.layers()) {
            for (IngredientDraft ing : layerDraft.ingredients()) {
                ingredients.add(new CompositionLayerIngredient(
                        0,
                        layerDraft.layerNumber(),
                        ing.powderId(),
                        ing.percentage()
                ));
            }
        }

        Integer existingProductId = request.product() == null ? null : request.product().id();
        String newProductCode = request.newProductCode() == null ? null : request.newProductCode().trim();
        compositionRepo.createVersionWithModelAndActivateForLine(
                existingProductId,
                newProductCode,
                request.lineName().trim(),
                request.blankModel().id(),
                request.layers().size(),
                request.notes(),
                ingredients
        );
    }

    private void validateRequest(SaveCompositionRequest request) {
        if (request.product() == null && (request.newProductCode() == null || request.newProductCode().isBlank())) {
            throw new IllegalArgumentException("Seleziona o crea un prodotto prima di salvare la composizione.");
        }

        if (request.lineName() == null || request.lineName().isBlank()) {
            throw new IllegalArgumentException("Seleziona o crea una linea prima di salvare la composizione.");
        }

        if (request.blankModel() == null) {
            throw new IllegalArgumentException("Seleziona un modello blank prima di salvare la composizione.");
        }

        if (request.layers() == null || request.layers().isEmpty()) {
            throw new IllegalArgumentException("Aggiungi almeno uno strato alla composizione.");
        }

        int numLayers = request.layers().size();
        if (numLayers != request.blankModel().numLayers()) {
            throw new IllegalArgumentException(
                    "La composizione ha " + numLayers + " layer, ma il modello blank selezionato richiede "
                            + request.blankModel().numLayers() + " layer."
            );
        }

        for (LayerDraft layer : request.layers()) {
            double totalPercentage = layer.ingredients().stream()
                    .mapToDouble(IngredientDraft::percentage)
                    .sum();
            if (totalPercentage != 100.0) {
                throw new IllegalArgumentException(
                        "La somma delle percentuali del layer " + layer.layerNumber()
                                + " deve essere pari a 100%. Valore attuale: " + totalPercentage + "%."
                );
            }
        }
    }

    public record LatestCompositionData(String notes,
                                        Integer blankModelId,
                                        List<LayerDraft> layerDrafts) {
    }

    public record SaveCompositionRequest(Product product,
                                         String newProductCode,
                                         String lineName,
                                         BlankModel blankModel,
                                         List<LayerDraft> layers,
                                         String notes) {
    }
}
