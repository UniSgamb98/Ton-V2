package com.orodent.tonv2.features.inventory.importing.service;

import com.orodent.tonv2.core.csv.CsvImportResult;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySnapshotImportServiceTest {
    private static final String HEADER =
            "Articolo;Descrizione;Quantità Attuale;ImpDaCli;Qta Venduta;Valore Venduto;Deposito\n";

    private final InventorySnapshotImportService service = new InventorySnapshotImportService();

    @Test
    void mapsItalianInventoryCsvIncludingDecimalCommasAndNegativeSales() {
        String csv = HEADER
                + "ZR9512MSTA2;ORODENT THOR 95x12 COLOR: A2;35;0;16;1111,59;MAGC\n"
                + "ZR9812MSB1;ORODENT EOS 98X12 COLOR: B1;0;0;-5;-413,25;MAGC\n";

        CsvImportResult<InventorySnapshotRow> result = service.analyze(new StringReader(csv));

        assertTrue(result.valid());
        assertEquals(2, result.validRows().size());
        InventorySnapshotRow first = result.validRows().getFirst().value();
        assertEquals(35, first.currentQuantity());
        assertEquals(0, first.customerCommittedQuantity());
        assertEquals(new BigDecimal("1111.59"), first.soldValue());
        InventorySnapshotRow second = result.validRows().get(1).value();
        assertEquals(-5, second.soldQuantity());
        assertEquals(new BigDecimal("-413.25"), second.soldValue());
    }

    @Test
    void reportsMappingAndValidationErrorsWithoutDiscardingValidRows() {
        String csv = HEADER
                + "OK;Descrizione;2;0;1;10,5;MAGC\n"
                + "BAD-NUMBER;Descrizione;x;0;0;0;MAGC\n"
                + ";Descrizione;1;0;0;0;MAGC\n";

        CsvImportResult<InventorySnapshotRow> result = service.analyze(new StringReader(csv));

        assertFalse(result.valid());
        assertEquals(3, result.totalRows());
        assertEquals(1, result.validRows().size());
        assertEquals(2, result.rowErrors().size());
        assertEquals(3, result.rowErrors().getFirst().lineNumber());
        assertEquals(InventorySnapshotCsvMapper.CURRENT_QUANTITY, result.rowErrors().getFirst().field());
    }

    @Test
    void reportsMissingRequiredHeadersAsDocumentError() {
        CsvImportResult<InventorySnapshotRow> result = service.analyze(
                new StringReader("Articolo;Descrizione\nA1;Test\n"));

        assertFalse(result.valid());
        assertEquals(1, result.documentErrors().size());
        assertTrue(result.documentErrors().getFirst().message().contains("Quantità Attuale"));
    }
}
