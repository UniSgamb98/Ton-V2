package com.orodent.tonv2.features.inventory.database.repository;

import com.orodent.tonv2.features.inventory.database.model.Stock;

public interface StockRepository {
    Stock find(int lotId, int depotId);
    Stock upsert(int lotId, int depotId, int quantity);
}