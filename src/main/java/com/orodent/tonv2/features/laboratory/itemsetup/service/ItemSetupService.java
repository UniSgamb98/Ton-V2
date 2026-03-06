package com.orodent.tonv2.features.laboratory.itemsetup.service;

import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.repository.CompositionRepository;
import com.orodent.tonv2.core.database.repository.ItemRepository;

import java.util.Optional;

public class ItemSetupService {

    public int activateLatestComposition(int productId, CompositionRepository compositionRepo) {
        Optional<Composition> latest = compositionRepo.findLatestByProduct(productId);
        if (latest.isEmpty()) {
            throw new IllegalArgumentException("Nessuna composizione trovata per il prodotto selezionato.");
        }

        compositionRepo.setActiveComposition(productId, latest.get().id());
        return latest.get().id();
    }

    public Item createItemForActiveComposition(int productId,
                                               double heightMm,
                                               ItemRepository itemRepo,
                                               CompositionRepository compositionRepo) {
        if (heightMm <= 0) {
            throw new IllegalArgumentException("L'altezza deve essere maggiore di zero.");
        }

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

        return itemRepo.insert(productId, blankModelId.get(), heightMm);
    }
}
