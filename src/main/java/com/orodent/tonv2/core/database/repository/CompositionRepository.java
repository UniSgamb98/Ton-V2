package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Composition;
import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;

import java.util.List;
import java.util.Optional;

public interface CompositionRepository {
    Optional<Integer> findMaxVersionByProduct(int productId);
    Optional<Composition> findLatestByProduct(int productId);
    Optional<Composition> findById(int compositionId);
    Optional<Integer> findActiveCompositionId(int productId);
    Optional<Integer> findBlankModelIdByCompositionId(int compositionId);
    void setActiveComposition(int productId, int compositionId);
    int insert(Composition composition);
    void createVersionWithModelAndActivate(Composition composition, int blankModelId, List<CompositionLayerIngredient> ingredients);
}
