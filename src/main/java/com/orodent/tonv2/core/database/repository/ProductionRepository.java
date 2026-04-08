package com.orodent.tonv2.core.database.repository;

import java.time.LocalDate;
import java.util.List;

public interface ProductionRepository {

    int insertProductionOrder(int productId, int compositionId, int blankModelId, LocalDate productionDate, String notes);

    void insertProductionOrderLine(int productionOrderId, int itemId, int quantity);

    List<ProducedDiskRow> findProducedDiskRows();
    List<CompositionRankingRow> findCompositionRankingRows();
    List<FurnaceItemSuggestionRow> findFurnaceItemSuggestionRows(String furnaceValue, String furnaceDisplayValue);

    record ProducedDiskRow(int itemId, String itemCode, int totalQuantity) {}
    record CompositionRankingRow(int compositionId, int availableQuantity, int distinctFurnacesUsed, int totalFirings) {}
    record FurnaceItemSuggestionRow(int itemId,
                                    String itemCode,
                                    int compositionId,
                                    int availableQuantity,
                                    Integer suggestedTemperature,
                                    Integer compositionAverageTemperature) {}
}
