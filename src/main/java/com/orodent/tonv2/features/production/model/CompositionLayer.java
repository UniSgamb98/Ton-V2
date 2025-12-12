package com.orodent.tonv2.features.production.model;

public record CompositionLayer(
        int id,
        int compositionId,
        int layerNumber,
        String notes
) {}
