package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionLayerIngredientRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.LineRepository;
import com.orodent.tonv2.core.database.repository.ProductRepository;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class CompositionArchiveService {

    private final ProductRepository productRepository;
    private final CompositionRepository compositionRepository;
    private final CompositionLayerIngredientRepository compositionLayerIngredientRepository;
    private final LineRepository lineRepository;

    public CompositionArchiveService(ProductRepository productRepository,
                                     CompositionRepository compositionRepository,
                                     CompositionLayerIngredientRepository compositionLayerIngredientRepository,
                                     LineRepository lineRepository) {
        this.productRepository = productRepository;
        this.compositionRepository = compositionRepository;
        this.compositionLayerIngredientRepository = compositionLayerIngredientRepository;
        this.lineRepository = lineRepository;
    }

    public List<Product> searchProductsWithCompositions(String codeFilter) {
        String normalized = codeFilter == null ? "" : codeFilter.trim().toLowerCase();

        return productRepository.findAll().stream()
                .filter(product -> normalized.isBlank() || product.code().toLowerCase().contains(normalized))
                .filter(product -> compositionRepository.findLatestByProduct(product.id()).isPresent())
                .toList();
    }

    public Optional<CompositionSnapshot> loadCompositionSnapshot(int productId) {
        Optional<Composition> latestComposition = compositionRepository.findLatestByProduct(productId);
        if (latestComposition.isEmpty()) {
            return Optional.empty();
        }

        Composition composition = latestComposition.get();
        Integer blankModelId = compositionRepository.findBlankModelIdByCompositionId(composition.id()).orElse(null);
        String lineName = lineRepository.findByProductId(productId).stream()
                .map(Line::name)
                .findFirst()
                .orElse(null);

        Map<Integer, LayerDraft> byLayer = new TreeMap<>();
        for (CompositionLayerIngredient ingredient : compositionLayerIngredientRepository.findByCompositionId(composition.id())) {
            LayerDraft draft = byLayer.computeIfAbsent(ingredient.layerNumber(), LayerDraft::new);
            draft.ingredients().add(new IngredientDraft(
                    ingredient.powderId(),
                    ingredient.percentage()
            ));
        }

        List<LayerDraft> layerDrafts = new ArrayList<>(byLayer.values());
        return Optional.of(new CompositionSnapshot(productId, lineName, composition.notes(), blankModelId, layerDrafts));
    }

    public record CompositionSnapshot(int productId,
                                      String lineName,
                                      String notes,
                                      Integer blankModelId,
                                      List<LayerDraft> layerDrafts) {
    }
}
