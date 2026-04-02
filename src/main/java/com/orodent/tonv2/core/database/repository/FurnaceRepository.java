package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Furnace;

import java.util.List;

public interface FurnaceRepository {

    List<Furnace> findAll();
}
