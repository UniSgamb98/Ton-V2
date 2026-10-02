package com.orodent.tonv2.features.inventory.importing.service;

import com.orodent.tonv2.core.csv.ModelValidator;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;

import java.util.ArrayList;
import java.util.List;

public final class InventorySnapshotRowValidator implements ModelValidator<InventorySnapshotRow> {
    @Override
    public List<ValidationError> validate(InventorySnapshotRow row) {
        List<ValidationError> errors = new ArrayList<>();
        requireText(errors, InventorySnapshotCsvMapper.ARTICLE, row.articleCode(), "Il codice articolo è obbligatorio.");
        requireText(errors, InventorySnapshotCsvMapper.DESCRIPTION, row.description(), "La descrizione è obbligatoria.");
        requireText(errors, InventorySnapshotCsvMapper.DEPOT, row.depotCode(), "Il deposito è obbligatorio.");
        if (row.currentQuantity() < 0) {
            errors.add(new ValidationError(InventorySnapshotCsvMapper.CURRENT_QUANTITY,
                    "La quantità attuale non può essere negativa."));
        }
        if (row.customerCommittedQuantity() < 0) {
            errors.add(new ValidationError(InventorySnapshotCsvMapper.CUSTOMER_COMMITTED,
                    "La quantità impegnata da cliente non può essere negativa."));
        }
        return List.copyOf(errors);
    }

    private void requireText(List<ValidationError> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) errors.add(new ValidationError(field, message));
    }
}
