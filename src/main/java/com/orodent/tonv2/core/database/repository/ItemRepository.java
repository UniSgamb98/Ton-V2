package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Item;

import java.util.List;

public interface ItemRepository {
    Item findById(int id);
    Item findByCode(String code);
    Item findByProductAndHeight(int productId, double heightMm);
    Item insert(String code, int productId, int blankModelId, double heightMm);
    List<Item> findAll();
    List<Item> findByProduct(int productId);
    List<Item> findByCodePrefix(String codePrefix, int limit);
    List<Item> findByLotCodePrefix(String lotCodePrefix, int limit);
    List<Item> findByDepot(String depotName);
}
