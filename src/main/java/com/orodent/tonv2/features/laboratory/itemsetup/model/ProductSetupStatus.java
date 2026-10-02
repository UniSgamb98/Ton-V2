package com.orodent.tonv2.features.laboratory.itemsetup.model;

public record ProductSetupStatus(Integer activeCompositionId, String blankModelCode) {
    public static ProductSetupStatus empty() {
        return new ProductSetupStatus(null, null);
    }

    public boolean hasActiveComposition() {
        return activeCompositionId != null;
    }

    public boolean hasBlankModel() {
        return blankModelCode != null && !blankModelCode.isBlank();
    }
}
