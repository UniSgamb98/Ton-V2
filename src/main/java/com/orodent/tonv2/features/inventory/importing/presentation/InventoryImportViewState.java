package com.orodent.tonv2.features.inventory.importing.presentation;

import java.util.List;

public record InventoryImportViewState(
        String totalRowsText,
        String validRowsText,
        String errorRowsText,
        String statusText,
        boolean valid,
        List<String> errors
) {
    public InventoryImportViewState {
        errors = List.copyOf(errors);
    }
}
