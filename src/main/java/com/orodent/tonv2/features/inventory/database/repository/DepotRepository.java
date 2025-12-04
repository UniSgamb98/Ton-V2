package com.orodent.tonv2.features.inventory.database.repository;

import com.orodent.tonv2.features.inventory.database.model.Depot;

public interface DepotRepository {
    Depot findByName(String name);
    Depot insert(String name);
}

