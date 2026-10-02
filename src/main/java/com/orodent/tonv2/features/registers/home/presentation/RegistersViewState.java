package com.orodent.tonv2.features.registers.home.presentation;

import java.util.List;

public record RegistersViewState(
        boolean success,
        String message,
        IdentityViewState identity,
        CompositionViewState composition,
        FiringViewState firing,
        List<DocumentViewState> documents
) {
    public record IdentityViewState(
            String itemCode,
            String lotCode,
            String compositionText,
            String firingText
    ) {}

    public record CompositionViewState(
            String versionText,
            String modelText,
            String heightText,
            String notice,
            List<CompositionLayerViewState> layers
    ) {}

    public record CompositionLayerViewState(
            String title,
            String diskPercentageText,
            List<IngredientViewState> ingredients
    ) {}

    public record IngredientViewState(String name, String percentageText) {}

    public record FiringViewState(
            String dateText,
            String furnaceText,
            String temperatureText,
            String totalText,
            String notice,
            List<FiringItemViewState> items
    ) {}

    public record FiringItemViewState(String itemCode, String quantityText) {}

    public record DocumentViewState(String name, String detailsText) {}
}
