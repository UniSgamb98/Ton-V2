package com.orodent.tonv2.features.inventory.importing.service;

import com.orodent.tonv2.core.csv.CsvImportResult;
import com.orodent.tonv2.core.csv.CsvImporter;
import com.orodent.tonv2.core.csv.CsvReadOptions;
import com.orodent.tonv2.core.csv.CsvReader;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;

import java.io.Reader;
import java.nio.file.Path;

public final class InventorySnapshotImportService {
    private final CsvImporter<InventorySnapshotRow> importer;

    public InventorySnapshotImportService() {
        importer = new CsvImporter<>(new CsvReader(), new InventorySnapshotCsvMapper(),
                new InventorySnapshotRowValidator());
    }

    public CsvImportResult<InventorySnapshotRow> analyze(Path path) {
        return importer.importFile(path, CsvReadOptions.semicolonSeparated());
    }

    public CsvImportResult<InventorySnapshotRow> analyze(Reader reader) {
        return importer.importReader(reader, CsvReadOptions.semicolonSeparated());
    }
}
