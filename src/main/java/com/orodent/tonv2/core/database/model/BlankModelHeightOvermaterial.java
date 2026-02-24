package com.orodent.tonv2.core.database.model;

public record BlankModelHeightOvermaterial(
        int id,
        int blankModelId,
        double minHeightMm,
        double maxHeightMm,
        double superiorOvermaterialMm,
        double inferiorOvermaterialMm
) {}

