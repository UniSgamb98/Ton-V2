package com.orodent.tonv2.features.production.model;

import java.time.LocalDateTime;

public record Composition(
        int id,
        int itemId,
        int lotId,
        LocalDateTime createdAt,
        String notes
) {}
