package com.orodent.tonv2.features.laboratory.production.presentation;

import com.orodent.tonv2.core.database.model.Item;
import com.orodent.tonv2.core.database.model.Line;
import com.orodent.tonv2.core.database.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchProductionFormStateTest {

    @Test
    void becomesReadyAndCalculatesTotalsWhenAQuantityIsConfigured() {
        BatchProductionFormState state = configuredState();
        Item first = state.itemQuantities().getFirst().item();

        state.setQuantity(first, 12);

        BatchProductionViewState viewState = state.toViewState();
        assertTrue(viewState.ready());
        assertEquals(1, viewState.configuredItems());
        assertEquals(12, viewState.totalQuantity());
        assertEquals("Pronto per produrre", viewState.readinessText());
    }

    @Test
    void selectingAnotherLineClearsProductAndQuantities() {
        BatchProductionFormState state = configuredState();
        state.setQuantity(state.itemQuantities().getFirst().item(), 5);

        state.selectLine(new Line(2, "Linea 2", 2));

        BatchProductionViewState viewState = state.toViewState();
        assertFalse(viewState.ready());
        assertEquals("Non selezionato", viewState.productText());
        assertEquals(0, viewState.totalQuantity());
        assertEquals("Seleziona un prodotto", viewState.readinessText());
    }

    @Test
    void clearingQuantitiesKeepsItemsButDisablesProduction() {
        BatchProductionFormState state = configuredState();
        state.setQuantity(state.itemQuantities().getFirst().item(), 5);

        state.clearQuantities();

        assertEquals(2, state.itemQuantities().size());
        assertFalse(state.toViewState().ready());
        assertEquals("Inserisci almeno una quantità", state.toViewState().readinessText());
    }

    private BatchProductionFormState configuredState() {
        BatchProductionFormState state = new BatchProductionFormState();
        state.selectLine(new Line(1, "Linea 1", 1));
        state.selectProduct(new Product(1, "PROD", "Prodotto"));
        state.setItems(List.of(
                new Item(1, "ART-1", 1, 1, 14),
                new Item(2, "ART-2", 1, 1, 18)
        ));
        return state;
    }
}
