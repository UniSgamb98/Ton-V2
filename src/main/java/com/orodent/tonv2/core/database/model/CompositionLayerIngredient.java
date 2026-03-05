package com.orodent.tonv2.core.database.model;

public record CompositionLayerIngredient(
        int compositionId,
        int layerNumber,
        int powderId,
        double percentage
) {}
