package com.orodent.tonv2.core.database.model;

import java.time.LocalDateTime;

public record Composition(
        int id,
        int itemId,
        int version,
        boolean active,
        LocalDateTime createdAt,
        String notes
) {}
