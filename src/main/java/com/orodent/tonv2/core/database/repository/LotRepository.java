package com.orodent.tonv2.core.database.repository;

import com.orodent.tonv2.core.database.model.Lot;

import java.util.List;

public interface LotRepository {
    Lot findByCodeAndItem(String lotCode, int itemId);
    Lot insert(String lotCode, int firingId);
    List<Lot> findByItem(int itemId);
}
