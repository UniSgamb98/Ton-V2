package com.orodent.tonv2.features.inventory.database.repository;

import com.orodent.tonv2.features.inventory.database.model.Item;

import java.util.List;

public interface ItemRepository {
    Item findByCode(String code);
    Item insert(String code);
    List<Item> findAll();
    List<Item> findByDepot(String depotName);

}

