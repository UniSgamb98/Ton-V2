package com.orodent.tonv2.features.inventory.importing.service;

import com.orodent.tonv2.core.csv.CsvMappingException;
import com.orodent.tonv2.core.csv.CsvRow;
import com.orodent.tonv2.core.csv.CsvRowMapper;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;

import java.math.BigDecimal;
import java.util.Set;

public final class InventorySnapshotCsvMapper implements CsvRowMapper<InventorySnapshotRow> {
    public static final String ARTICLE = "Articolo";
    public static final String DESCRIPTION = "Descrizione";
    public static final String CURRENT_QUANTITY = "Quantità Attuale";
    public static final String CUSTOMER_COMMITTED = "ImpDaCli";
    public static final String SOLD_QUANTITY = "Qta Venduta";
    public static final String SOLD_VALUE = "Valore Venduto";
    public static final String DEPOT = "Deposito";

    @Override
    public Set<String> requiredColumns() {
        return Set.of(ARTICLE, DESCRIPTION, CURRENT_QUANTITY, CUSTOMER_COMMITTED,
                SOLD_QUANTITY, SOLD_VALUE, DEPOT);
    }

    @Override
    public InventorySnapshotRow map(CsvRow row) {
        return new InventorySnapshotRow(
                row.requiredValue(ARTICLE),
                row.requiredValue(DESCRIPTION),
                parseInteger(row, CURRENT_QUANTITY),
                parseInteger(row, CUSTOMER_COMMITTED),
                parseInteger(row, SOLD_QUANTITY),
                parseDecimal(row, SOLD_VALUE),
                row.requiredValue(DEPOT)
        );
    }

    private int parseInteger(CsvRow row, String column) {
        String raw = row.requiredValue(column);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new CsvMappingException(column, raw, "Il valore deve essere un numero intero.");
        }
    }

    private BigDecimal parseDecimal(CsvRow row, String column) {
        String raw = row.requiredValue(column);
        try {
            return new BigDecimal(raw.replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new CsvMappingException(column, raw, "Il valore deve essere un importo valido.");
        }
    }
}
