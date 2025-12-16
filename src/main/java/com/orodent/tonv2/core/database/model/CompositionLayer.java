package com.orodent.tonv2.core.database.model;

public record CompositionLayer(
        int id,
        int compositionId,
        int layerNumber,
        String notes
) {}
