package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Composition;

import java.util.Optional;

public interface CompositionRepository {
    void deactivateActiveByProduct(int itemId);
    Optional<Integer> findMaxVersionByProduct(int itemId);
    int insert(Composition composition);
}
