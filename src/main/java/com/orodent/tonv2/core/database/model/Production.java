package com.orodent.tonv2.core.database.model;

import java.time.LocalDate;

public record Production(
        int id,
        int itemId,
        int compositionId,
        int producedQty,
        LocalDate productionDate,
        String notes
) {}
