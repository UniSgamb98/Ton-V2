package com.orodent.tonv2.features.registers.home.presentation;

import com.orodent.tonv2.features.registers.home.model.RegisterSearchResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistersPresenterTest {
    private final RegistersPresenter presenter = new RegistersPresenter();

    @Test
    void formatsSuccessfulRegisterForTheView() {
        RegisterSearchResult result = RegisterSearchResult.success(
                new RegisterSearchResult.RegisterIdentity("ART-1", "LOT-1", 4, 12),
                new RegisterSearchResult.CompositionDetails(
                        4, 3, 18,
                        List.of(new RegisterSearchResult.CompositionLayerDetails(
                                1, 25, List.of(new RegisterSearchResult.IngredientDetails("Polvere bianca", 100)))),
                        RegisterSearchResult.CompositionStatus.AVAILABLE),
                new RegisterSearchResult.FiringDetails(
                        12, LocalDate.of(2026, 10, 2), "Forno 2", 1530,
                        List.of(new RegisterSearchResult.FiringItemDetails("ART-1", 8)),
                        RegisterSearchResult.FiringStatus.AVAILABLE),
                List.of(new RegisterSearchResult.DocumentDetails("Produzione", "PRODUCTION", Instant.EPOCH))
        );

        RegistersViewState state = presenter.present(result);

        assertTrue(state.success());
        assertEquals("Composizione v.4", state.identity().compositionText());
        assertEquals("18 mm", state.composition().heightText());
        assertEquals("25% del disco", state.composition().layers().getFirst().diskPercentageText());
        assertEquals("8 pezzi", state.firing().totalText());
        assertNull(state.composition().notice());
    }

    @Test
    void mapsSemanticMissingStatesToUserMessages() {
        RegisterSearchResult result = RegisterSearchResult.success(
                new RegisterSearchResult.RegisterIdentity("ART-1", "LOT-1", null, 12),
                new RegisterSearchResult.CompositionDetails(
                        null, 3, 18, List.of(), RegisterSearchResult.CompositionStatus.NO_ACTIVE_COMPOSITION),
                new RegisterSearchResult.FiringDetails(
                        12, null, null, null, List.of(), RegisterSearchResult.FiringStatus.NOT_FOUND),
                List.of()
        );

        RegistersViewState state = presenter.present(result);

        assertEquals("Nessuna composizione attiva trovata.", state.composition().notice());
        assertEquals("Dettagli del ciclo di sinterizzazione non trovati.", state.firing().notice());
    }

    @Test
    void keepsFailureStateFreeFromResultDetails() {
        RegistersViewState state = presenter.present(RegisterSearchResult.failure(
                RegisterSearchResult.FailureReason.INCOMPLETE_CRITERIA, null));

        assertFalse(state.success());
        assertEquals("Inserisci sia Articolo che Lotto per avviare la ricerca.", state.message());
        assertNull(state.identity());
    }
}
