package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.PowderRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.time.LocalDateTime;
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
    private final BlankModelRepository blankModelRepo;

    public CreateCompositionService(PowderRepository powderRepo,
                                    CompositionRepository compositionRepo,
                                    CompositionLayerIngredientRepository compositionLayerIngredientRepo,
                                    ProductRepository productRepo,
                                    BlankModelRepository blankModelRepo) {
        this.powderRepo = powderRepo;
        this.compositionRepo = compositionRepo;
        this.compositionLayerIngredientRepo = compositionLayerIngredientRepo;
        this.productRepo = productRepo;
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

    public Product createProduct(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("Inserisci un codice prodotto valido per continuare.");
        }

        return productRepo.insert(productCode.trim(), null);
    }

    public Optional<LatestCompositionData> loadLatestComposition(int productId) {
        Optional<Composition> latestComposition = compositionRepo.findLatestByProduct(productId);
        if (latestComposition.isEmpty()) {
            return Optional.empty();
        }

        Composition composition = latestComposition.get();
        Integer blankModelId = compositionRepo.findBlankModelIdByCompositionId(composition.id()).orElse(null);

        List<LayerDraft> layerDrafts = new ArrayList<>();
        Map<Integer, LayerDraft> byLayer = new TreeMap<>();

        for (CompositionLayerIngredient ingredient : compositionLayerIngredientRepo.findByCompositionId(composition.id())) {
            LayerDraft draft = byLayer.computeIfAbsent(ingredient.layerNumber(), LayerDraft::new);
            draft.ingredients().add(new IngredientDraft(
                    ingredient.powderId(),
                    ingredient.percentage()
            ));
        }

        layerDrafts.addAll(byLayer.values());
        return Optional.of(new LatestCompositionData(composition.notes(), blankModelId, layerDrafts));
    }

    public void saveComposition(SaveCompositionRequest request) {
        validateRequest(request);

        int newVersion = compositionRepo
                .findMaxVersionByProduct(request.product().id())
                .map(v -> v + 1)
                .orElse(1);

        Composition composition = new Composition(
                0,
                request.product().id(),
                newVersion,
                request.layers().size(),
                LocalDateTime.now(),
                request.notes()
        );

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

        compositionRepo.createVersionWithModelAndActivate(composition, request.blankModel().id(), ingredients);
    }

    private void validateRequest(SaveCompositionRequest request) {
        if (request.product() == null) {
            throw new IllegalArgumentException("Seleziona o crea un prodotto prima di salvare la composizione.");
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
    }

    public record LatestCompositionData(String notes,
                                        Integer blankModelId,
                                        List<LayerDraft> layerDrafts) {
    }

    public record SaveCompositionRequest(Product product,
                                         BlankModel blankModel,
                                         List<LayerDraft> layers,
                                         String notes) {
    }
}
