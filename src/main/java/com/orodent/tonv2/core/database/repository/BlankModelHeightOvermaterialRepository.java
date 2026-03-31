package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.BlankModelHeightOvermaterial;

import java.util.List;

public interface BlankModelHeightOvermaterialRepository {
    int insert(BlankModelHeightOvermaterial range);
    List<BlankModelHeightOvermaterial> findByBlankModelId(int blankModelId);
}
