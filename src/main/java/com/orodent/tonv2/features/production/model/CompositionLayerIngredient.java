package com.orodent.tonv2.features.production.model;

public record CompositionLayerIngredient(
        int id,
        int layerId,
        int powderId,
        double percentage
) {}
