package com.orodent.tonv2.features.laboratory.composition.presentation;

import com.orodent.tonv2.core.database.model.Powder;
import com.orodent.tonv2.core.ui.draft.IngredientDraft;
import com.orodent.tonv2.core.ui.draft.LayerDraft;
import com.orodent.tonv2.features.laboratory.composition.service.LayerMetricsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositionLayerPresenterTest {

    @Test
    void buildsPresentationStateFromLayerDraft() {
        Powder powder = new Powder(1, "P1", "Powder", 1, 1000.0, 0.5, 4, null);
        LayerDraft layer = new LayerDraft(2);
        layer.ingredients().add(new IngredientDraft(1, 100));

        CompositionLayerViewState state = new CompositionLayerPresenter(new LayerMetricsService())
                .present(layer, List.of(powder));

        assertEquals(2, state.layerNumber());
        assertEquals(1, state.ingredientCount());
        assertTrue(state.valid());
        assertTrue(state.metricsText().contains("Resistenza: 1000 MPa"));
    }
}
