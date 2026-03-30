package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Line;

import java.util.List;

public interface LineRepository {
    List<Line> findAll();
    List<Line> findByProductId(int productId);
    List<String> findDistinctNames();
}
