package com.orodent.tonv2.features.laboratory.itemsetup.presentation;

import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.itemsetup.model.ProductSetupStatus;

public final class ItemSetupFormState {

    public ItemSetupViewState evaluate(
            Product product,
            ProductSetupStatus setupStatus,
            String rawCode,
            String rawHeight
    ) {
        ProductSetupStatus status = setupStatus == null ? ProductSetupStatus.empty() : setupStatus;
        String code = normalize(rawCode);
        String height = normalize(rawHeight);

        String compositionTitle;
        String compositionDetails;
        if (product == null) {
            compositionTitle = "Seleziona un prodotto";
            compositionDetails = "La configurazione verrà verificata automaticamente.";
        } else if (!status.hasActiveComposition()) {
            compositionTitle = "Nessuna composizione attiva";
            compositionDetails = "Puoi rendere attiva l'ultima composizione disponibile.";
        } else if (!status.hasBlankModel()) {
            compositionTitle = "Composizione #" + status.activeCompositionId();
            compositionDetails = "La composizione attiva non ha un modello disco associato.";
        } else {
            compositionTitle = "✓ Composizione #" + status.activeCompositionId();
            compositionDetails = "Modello disco: " + status.blankModelCode();
        }

        String readiness;
        boolean ready;
        if (product == null) {
            readiness = "Seleziona un prodotto";
            ready = false;
        } else if (!status.hasActiveComposition()) {
            readiness = "Attiva una composizione";
            ready = false;
        } else if (!status.hasBlankModel()) {
            readiness = "Modello disco mancante";
            ready = false;
        } else if (code.isEmpty()) {
            readiness = "Inserisci il codice";
            ready = false;
        } else if (!isPositiveNumber(height)) {
            readiness = "Inserisci un'altezza valida";
            ready = false;
        } else {
            readiness = "Pronto per la creazione  ✓";
            ready = true;
        }

        return new ItemSetupViewState(
                product == null ? "Non selezionato" : product.code(),
                status.hasActiveComposition() ? "#" + status.activeCompositionId() + "  ✓" : "Non disponibile",
                status.hasBlankModel() ? status.blankModelCode() + "  ✓" : "Non disponibile",
                code.isEmpty() ? "—" : code,
                height.isEmpty() ? "—" : height + " mm",
                compositionTitle,
                compositionDetails,
                readiness,
                ready
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isPositiveNumber(String value) {
        try {
            return Double.parseDouble(value.replace(',', '.')) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
