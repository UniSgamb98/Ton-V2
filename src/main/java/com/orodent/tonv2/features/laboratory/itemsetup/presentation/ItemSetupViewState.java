package com.orodent.tonv2.features.laboratory.itemsetup.presentation;

public record ItemSetupViewState(
        String productText,
        String compositionText,
        String blankModelText,
        String itemCodeText,
        String heightText,
        String compositionStateTitle,
        String compositionStateDetails,
        String readinessText,
        boolean ready
) {
}
