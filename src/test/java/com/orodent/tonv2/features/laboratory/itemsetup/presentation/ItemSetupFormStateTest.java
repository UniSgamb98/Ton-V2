package com.orodent.tonv2.features.laboratory.itemsetup.presentation;

import com.orodent.tonv2.core.database.model.Product;
import com.orodent.tonv2.features.laboratory.itemsetup.model.ProductSetupStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSetupFormStateTest {
    private final ItemSetupFormState formState = new ItemSetupFormState();

    @Test
    void formIsReadyOnlyWhenEveryPrerequisiteIsAvailable() {
        ItemSetupViewState state = formState.evaluate(
                new Product(1, "PROD-1", "Product"),
                new ProductSetupStatus(10, "BM-98"),
                "ITEM-18",
                "18,5"
        );

        assertTrue(state.ready());
    }

    @Test
    void formIsNotReadyWithoutActiveComposition() {
        ItemSetupViewState state = formState.evaluate(
                new Product(1, "PROD-1", "Product"),
                ProductSetupStatus.empty(),
                "ITEM-18",
                "18.5"
        );

        assertFalse(state.ready());
    }
}
