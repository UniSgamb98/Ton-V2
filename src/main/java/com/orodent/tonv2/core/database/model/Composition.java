package com.orodent.tonv2.core.database.model;

import java.time.LocalDateTime;

public record Composition(
        int id,
        int productId,
        int version,
        int numLayers,
        LocalDateTime createdAt,
        String notes
) {}
