package com.orodent.tonv2.core.database.model;

public record CompositionLayerIngredient(
        int id,
        int layerId,
        int powderId,
        double percentage
) {}
