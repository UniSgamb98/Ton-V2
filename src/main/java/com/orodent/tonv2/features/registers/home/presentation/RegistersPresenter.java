package com.orodent.tonv2.features.registers.home.presentation;

import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class RegistersPresenter {
    private static final DateTimeFormatter DOCUMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    public RegistersViewState present(RegisterSearchResult result) {
        if (!result.success()) {
            return new RegistersViewState(false, failureMessage(result), null, null, null, List.of());
        }

        RegisterSearchResult.RegisterIdentity identity = result.identity();
        RegistersViewState.IdentityViewState identityState = new RegistersViewState.IdentityViewState(
                identity.itemCode(),
                identity.lotCode(),
                identity.compositionVersion() == null
                        ? "Composizione non disponibile"
                        : "Composizione v." + identity.compositionVersion(),
                "Firing #" + identity.firingId()
        );

        return new RegistersViewState(
                true,
                "Registro caricato correttamente.",
                identityState,
                presentComposition(result.composition()),
                presentFiring(result.firing()),
                result.documents().stream().map(this::presentDocument).toList()
        );
    }

    private String failureMessage(RegisterSearchResult result) {
        return switch (result.failureReason()) {
            case INCOMPLETE_CRITERIA -> "Inserisci sia Articolo che Lotto per avviare la ricerca.";
            case ITEM_NOT_FOUND -> "Articolo non trovato: " + result.failureReference();
            case LOT_NOT_FOUND -> "Lotto non trovato per l'articolo selezionato: " + result.failureReference();
        };
    }

    private RegistersViewState.CompositionViewState presentComposition(
            RegisterSearchResult.CompositionDetails details
    ) {
        String notice = switch (details.status()) {
            case AVAILABLE -> null;
            case NO_ACTIVE_COMPOSITION -> "Nessuna composizione attiva trovata.";
            case NO_MODEL_LAYERS -> "Nessuno strato del modello disco trovato.";
        };
        List<RegistersViewState.CompositionLayerViewState> layers = details.layers().stream()
                .map(layer -> new RegistersViewState.CompositionLayerViewState(
                        "Strato " + layer.layerNumber(),
                        formatNumber(layer.diskPercentage()) + "% del disco",
                        layer.ingredients().stream()
                                .map(ingredient -> new RegistersViewState.IngredientViewState(
                                        ingredient.name(), formatNumber(ingredient.percentage()) + "%"))
                                .toList()))
                .toList();
        return new RegistersViewState.CompositionViewState(
                details.version() == null ? "Non disponibile" : "Versione " + details.version(),
                "Modello #" + details.blankModelId(),
                formatNumber(details.heightMm()) + " mm",
                notice,
                layers
        );
    }

    private RegistersViewState.FiringViewState presentFiring(RegisterSearchResult.FiringDetails details) {
        String notice = switch (details.status()) {
            case AVAILABLE -> null;
            case NOT_FOUND -> "Dettagli del ciclo di sinterizzazione non trovati.";
            case NO_ITEMS -> "Nessun articolo trovato nel ciclo.";
        };
        int total = details.items().stream().mapToInt(RegisterSearchResult.FiringItemDetails::quantity).sum();
        return new RegistersViewState.FiringViewState(
                details.date() == null ? "Non disponibile" : details.date().toString(),
                details.furnace() == null || details.furnace().isBlank() ? "Non disponibile" : details.furnace(),
                details.maxTemperature() == null ? "Non disponibile" : details.maxTemperature() + " °C",
                total + " pezzi",
                notice,
                details.items().stream()
                        .map(item -> new RegistersViewState.FiringItemViewState(
                                item.itemCode(), Integer.toString(item.quantity())))
                        .toList()
        );
    }

    private RegistersViewState.DocumentViewState presentDocument(RegisterSearchResult.DocumentDetails document) {
        String preset = document.presetCode() == null || document.presetCode().isBlank()
                ? "Nessun preset"
                : document.presetCode();
        return new RegistersViewState.DocumentViewState(
                document.name(), preset + " · " + DOCUMENT_DATE_FORMAT.format(document.savedAt()));
    }

    private String formatNumber(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }
}
