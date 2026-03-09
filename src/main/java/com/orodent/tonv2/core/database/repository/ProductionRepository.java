package com.orodent.tonv2.core.database.repository;

import java.time.LocalDate;
import java.util.List;

public interface ProductionRepository {

    int insertProductionOrder(int productId, int compositionId, int blankModelId, LocalDate productionDate, String notes);

    void insertProductionOrderLine(int productionOrderId, int itemId, int quantity);

    List<ProducedDiskRow> findProducedDiskRows();

    record ProducedDiskRow(int itemId, String itemCode, int totalQuantity) {}
}
