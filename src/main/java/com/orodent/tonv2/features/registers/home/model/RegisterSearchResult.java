package com.orodent.tonv2.features.registers.home.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RegisterSearchResult(
        boolean success,
        FailureReason failureReason,
        String failureReference,
        RegisterIdentity identity,
        CompositionDetails composition,
        FiringDetails firing,
        List<DocumentDetails> documents
) {
    public static RegisterSearchResult success(
            RegisterIdentity identity,
            CompositionDetails composition,
            FiringDetails firing,
            List<DocumentDetails> documents
    ) {
        return new RegisterSearchResult(true, null, null, identity, composition, firing, List.copyOf(documents));
    }

    public static RegisterSearchResult failure(FailureReason reason, String reference) {
        return new RegisterSearchResult(false, reason, reference, null, null, null, List.of());
    }

    public enum FailureReason { INCOMPLETE_CRITERIA, ITEM_NOT_FOUND, LOT_NOT_FOUND }

    public record RegisterIdentity(String itemCode, String lotCode, Integer compositionVersion, int firingId) {}

    public record CompositionDetails(
            Integer version,
            int blankModelId,
            double heightMm,
            List<CompositionLayerDetails> layers,
            CompositionStatus status
    ) {}

    public enum CompositionStatus {
        AVAILABLE,
        NO_ACTIVE_COMPOSITION,
        NO_MODEL_LAYERS
    }

    public record CompositionLayerDetails(
            int layerNumber,
            double diskPercentage,
            List<IngredientDetails> ingredients
    ) {}

    public record IngredientDetails(String name, double percentage) {}

    public record FiringDetails(
            int id,
            LocalDate date,
            String furnace,
            Integer maxTemperature,
            List<FiringItemDetails> items,
            FiringStatus status
    ) {}

    public enum FiringStatus {
        AVAILABLE,
        NOT_FOUND,
        NO_ITEMS
    }

    public record FiringItemDetails(String itemCode, int quantity) {}

    public record DocumentDetails(String name, String presetCode, Instant savedAt) {}
}
