package com.orodent.tonv2.core.database.model;

public record BlankModelLayer(
        int id,
        int blankModelId,
        int layerNumber,
        double occupiedSpacePercent
) {}
