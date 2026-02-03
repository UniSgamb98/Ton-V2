package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Item;

import java.util.List;

public interface ItemRepository {
    Item findByCode(String code);
    Item insert(String code);
    List<Item> findAll();
    List<Item> findByDepot(String depotName);

}

