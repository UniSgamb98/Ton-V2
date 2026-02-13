package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.CompositionLayer;

import java.util.List;

public interface CompositionLayerRepository {
    int insert(CompositionLayer layer);
    List<CompositionLayer> findByCompositionId(int compositionId);
}
