package com.orodent.tonv2.features.inventory.importing.presentation;

import com.orodent.tonv2.core.csv.CsvImportResult;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;

import java.util.ArrayList;
import java.util.List;

public final class InventoryImportPresenter {
    public InventoryImportViewState present(CsvImportResult<InventorySnapshotRow> result) {
        List<String> errors = new ArrayList<>();
        result.documentErrors().forEach(error -> errors.add(
                "Riga " + error.lineNumber() + " · " + error.message()));
        result.rowErrors().forEach(error -> errors.add(
                "Riga " + error.lineNumber() + " · " + error.field() + " · " + error.message()));

        String status = result.valid()
                ? "File valido e pronto per l'aggiornamento."
                : "Il file contiene errori da correggere.";
        return new InventoryImportViewState(
                Integer.toString(result.totalRows()),
                Integer.toString(result.validRows().size()),
                Integer.toString(result.rowErrors().size() + result.documentErrors().size()),
                status,
                result.valid(),
                errors
        );
    }
}
