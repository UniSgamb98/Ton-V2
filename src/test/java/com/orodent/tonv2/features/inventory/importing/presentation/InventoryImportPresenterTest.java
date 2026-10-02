package com.orodent.tonv2.features.inventory.importing.presentation;

import com.orodent.tonv2.core.csv.CsvImportResult;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InventoryImportPresenterTest {
    @Test
    void summarizesRowsAndFormatsErrors() {
        CsvImportResult<InventorySnapshotRow> result = new CsvImportResult<>(
                3,
                List.of(),
                List.of(new CsvImportResult.RowError(4, "Qta Venduta", "x", "Intero non valido")),
                List.of(new CsvImportResult.DocumentError(1, "Intestazione non valida"))
        );

        InventoryImportViewState state = new InventoryImportPresenter().present(result);

        assertFalse(state.valid());
        assertEquals("3", state.totalRowsText());
        assertEquals("2", state.errorRowsText());
        assertEquals("Riga 4 · Qta Venduta · Intero non valido", state.errors().get(1));
    }
}
