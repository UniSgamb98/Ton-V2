package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Stock;

import java.util.List;

public interface StockRepository {
    Stock find(int lotId, int depotId);
    Stock upsert(int lotId, int depotId, int quantity);
    int getQuantityByItemAndDepot(int itemId, int depotId);
    List<Stock> findByLotAll(int lotId);
}