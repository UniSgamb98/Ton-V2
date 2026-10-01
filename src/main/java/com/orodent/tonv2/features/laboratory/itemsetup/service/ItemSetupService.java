package com.orodent.tonv2.features.laboratory.itemsetup.service;

import com.orodent.tonv2.core.database.ConnectionProvider;
import com.orodent.tonv2.core.database.implementation.BlankModelRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.CompositionRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ItemRepositoryImpl;
import com.orodent.tonv2.core.database.implementation.ProductRepositoryImpl;
import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

public class ItemSetupService {
    private final ConnectionProvider connectionProvider;

    public ItemSetupService(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public int activateLatestComposition(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("Prodotto non valido.");
        }
        return connectionProvider.withTransaction(connection -> {
            CompositionRepository compositionRepo = new CompositionRepositoryImpl(connection);
            Optional<Composition> latest = compositionRepo.findLatestByProduct(productId);
            if (latest.isEmpty()) {
                throw new IllegalArgumentException("Nessuna composizione trovata per il prodotto selezionato.");
            }
            compositionRepo.setActiveComposition(productId, latest.get().id());
            return latest.get().id();
        });
    }

    public ProductSetupStatus findProductSetupStatus(int productId) {
        if (productId <= 0) {
            return ProductSetupStatus.empty();
        }
        return connectionProvider.withConnection(connection -> {
            CompositionRepository compositionRepo = new CompositionRepositoryImpl(connection);
            Optional<Integer> activeCompositionId = compositionRepo.findActiveCompositionId(productId);
            if (activeCompositionId.isEmpty()) {
                return ProductSetupStatus.empty();
            }

            Optional<Integer> blankModelId = compositionRepo.findBlankModelIdByCompositionId(activeCompositionId.get());
            if (blankModelId.isEmpty()) {
                return new ProductSetupStatus(activeCompositionId.get(), null);
            }

            var blankModel = new BlankModelRepositoryImpl(connection).findById(blankModelId.get());
            return new ProductSetupStatus(
                    activeCompositionId.get(),
                    blankModel == null ? null : blankModel.code()
            );
        });
    }

    public Item createItemForActiveComposition(String itemCode,
                                               int productId,
                                               double heightMm) {
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Codice item obbligatorio.");
        }

        if (heightMm <= 0) {
            throw new IllegalArgumentException("L'altezza deve essere maggiore di zero.");
        }
        if (productId <= 0) {
            throw new IllegalArgumentException("Prodotto non valido.");
        }
        return connectionProvider.withTransaction(connection -> {
            CompositionRepository compositionRepo = new CompositionRepositoryImpl(connection);
            ItemRepository itemRepo = new ItemRepositoryImpl(connection);
            Optional<Integer> activeCompositionId = compositionRepo.findActiveCompositionId(productId);
            if (activeCompositionId.isEmpty()) {
                throw new IllegalArgumentException("Imposta prima una composizione attiva per questo prodotto.");
            }

            Optional<Integer> blankModelId = compositionRepo.findBlankModelIdByCompositionId(activeCompositionId.get());
            if (blankModelId.isEmpty()) {
                throw new IllegalArgumentException("La composizione attiva non ha un blank model associato.");
            }

            Item existing = itemRepo.findByProductAndHeight(productId, heightMm);
            if (existing != null) {
                return existing;
            }
            return itemRepo.insert(itemCode.trim(), productId, blankModelId.get(), heightMm);
        });
    }

    public List<Product> findAllProduct() {
        return connectionProvider.withConnection(connection ->
                new ProductRepositoryImpl(connection).findAll());
    }

    public record ProductSetupStatus(Integer activeCompositionId, String blankModelCode) {
        public static ProductSetupStatus empty() {
            return new ProductSetupStatus(null, null);
        }

        public boolean hasActiveComposition() {
            return activeCompositionId != null;
        }

        public boolean hasBlankModel() {
            return blankModelCode != null && !blankModelCode.isBlank();
        }
    }
}
