package com.orodent.tonv2.features.inventory.importing.model;

import java.math.BigDecimal;

public record InventorySnapshotRow(
        String articleCode,
        String description,
        int currentQuantity,
        int customerCommittedQuantity,
        int soldQuantity,
        BigDecimal soldValue,
        String depotCode
) {
}
