package com.orodent.tonv2.features.laboratory.composition.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.BlankModelRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.CompositionLayerIngredientRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.CompositionRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.LineRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.PowderRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ProductRepositoryImpl;
import com.orodent.tonv2.core.database.model.BlankModel;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;
import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.BlankModelRepository;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class CreateCompositionService {
    private final ConnectionProvider connectionProvider;

    public CreateCompositionService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public InitialData loadInitialData() {
        return connectionProvider.withConnection(connection -> new InitialData(
                new ProductRepositoryImpl(connection).findAll(),
                latestBlankModels(new BlankModelRepositoryImpl(connection)),
                new PowderRepositoryImpl(connection).findAll(),
                new LineRepositoryImpl(connection).findDistinctNames()
        ));
    }

    private List<BlankModel> latestBlankModels(BlankModelRepository blankModelRepo) {
        java.util.LinkedHashMap<String, BlankModel> latestByCode = new java.util.LinkedHashMap<>();
        for (BlankModel model : blankModelRepo.findAll()) {
            latestByCode.putIfAbsent(model.code(), model);
        }
        return new java.util.ArrayList<>(latestByCode.values());
    }

    public List<String> findLineNamesByProductId(int productId) {
        return connectionProvider.withConnection(connection -> new LineRepositoryImpl(connection).findByProductId(productId).stream()
                .map(com.orodent.tonv2.core.database.model.Line::name)
                .distinct()
                .toList());
    }

    public Optional<LatestCompositionData> loadLatestComposition(int productId) {
        return connectionProvider.withConnection(connection -> {
            CompositionRepository compositionRepo = new CompositionRepositoryImpl(connection);
            Optional<Composition> latestComposition = compositionRepo.findLatestByProduct(productId);
            if (latestComposition.isEmpty()) return Optional.empty();
            Composition composition = latestComposition.get();
            Integer blankModelId = compositionRepo.findBlankModelIdByCompositionId(composition.id()).orElse(null);
            Map<Integer, LayerDraft> byLayer = new TreeMap<>();
            for (CompositionLayerIngredient ingredient : new CompositionLayerIngredientRepositoryImpl(connection)
                    .findByCompositionId(composition.id())) {
                LayerDraft draft = byLayer.computeIfAbsent(ingredient.layerNumber(), LayerDraft::new);
                draft.ingredients().add(new IngredientDraft(ingredient.powderId(), ingredient.percentage()));
            }
            return Optional.of(new LatestCompositionData(composition.notes(), blankModelId, new ArrayList<>(byLayer.values())));
        });
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
        connectionProvider.withConnection(connection -> {
            new CompositionRepositoryImpl(connection).createVersionWithModelAndActivateForLine(
                existingProductId,
                newProductCode,
                request.lineName().trim(),
                request.blankModel().id(),
                request.layers().size(),
                request.notes(),
                ingredients);
            return null;
        });
    }

    private void validateRequest(SaveCompositionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dati composizione mancanti.");
        }
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

    public record InitialData(List<Product> products,
                              List<BlankModel> blankModels,
                              List<Powder> powders,
                              List<String> lineNames) {
    }

    public record SaveCompositionRequest(Product product,
                                         String newProductCode,
                                         String lineName,
                                         BlankModel blankModel,
                                         List<LayerDraft> layers,
                                         String notes) {
    }
}
