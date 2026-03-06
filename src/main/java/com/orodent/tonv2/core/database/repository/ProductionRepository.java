package com.orodent.tonv2.core.database.repository;

import java.util.List;

public interface ProductionRepository {

    List<ProducedDiskRow> findProducedDiskRows();

    record ProducedDiskRow(int itemId, String itemCode, int totalQuantity) {}
}
