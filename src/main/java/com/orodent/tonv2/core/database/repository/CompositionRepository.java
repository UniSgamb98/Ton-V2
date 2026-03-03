package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Composition;

import java.util.Optional;

public interface CompositionRepository {
    Optional<Integer> findMaxVersionByProduct(int productId);
    Optional<Composition> findLatestByProduct(int productId);
    Optional<Integer> findActiveCompositionId(int productId);
    void setActiveComposition(int productId, int compositionId);
    int insert(Composition composition);
}
