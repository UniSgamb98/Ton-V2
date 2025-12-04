package com.orodent.tonv2.features.inventory.database.repository;

import com.orodent.tonv2.features.inventory.database.model.Lot;

public interface LotRepository {
    Lot findByCodeAndItem(String lotCode, int itemId);
    Lot insert(String lotCode, int itemId);
}
