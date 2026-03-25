package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.BlankModelLayer;

import java.util.List;

public interface BlankModelLayerRepository {
    void insert(BlankModelLayer layer);
    List<BlankModelLayer> findByBlankModelId(int blankModelId);
}
