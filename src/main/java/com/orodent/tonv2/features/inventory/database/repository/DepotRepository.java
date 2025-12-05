package com.orodent.tonv2.features.inventory.database.repository;

import com.orodent.tonv2.features.inventory.database.model.Depot;

import java.util.List;

public interface DepotRepository {
    Depot findByName(String name);
    List<Depot> findAll();
    Depot insert(String name);
    Depot findById(int id);
}

