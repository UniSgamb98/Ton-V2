package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Depot;

import java.util.List;

public interface DepotRepository {
    Depot findByName(String name);
    List<Depot> findAll();
    Depot insert(String name);
    Depot findById(int id);
}

