package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.CompositionLayerIngredient;

import java.util.List;

public interface CompositionLayerIngredientRepository {
    void insert(CompositionLayerIngredient cli);
    List<CompositionLayerIngredient> findByLayerId(int layerId);
}
