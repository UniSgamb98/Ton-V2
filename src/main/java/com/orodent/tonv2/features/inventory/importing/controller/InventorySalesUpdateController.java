package com.orodent.tonv2.features.inventory.importing.controller;

import com.orodent.tonv2.core.csv.CsvImportResult;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.inventory.importing.model.InventorySnapshotRow;
import com.orodent.tonv2.features.inventory.importing.presentation.InventoryImportPresenter;
import com.orodent.tonv2.features.inventory.importing.service.InventorySnapshotImportService;
import com.orodent.tonv2.features.inventory.importing.view.InventorySalesUpdateView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.concurrent.Executor;

public final class InventorySalesUpdateController {
    private final InventorySalesUpdateView view;
    private final InventorySnapshotImportService service;
    private final InventoryImportPresenter presenter;
    private final DebouncedTaskRunner<CsvImportResult<InventorySnapshotRow>> importRunner;

    public InventorySalesUpdateController(
            InventorySalesUpdateView view,
            InventorySnapshotImportService service,
            InventoryImportPresenter presenter,
            Executor backgroundExecutor
    ) {
        this.view = view;
        this.service = service;
        this.presenter = presenter;
        this.importRunner = new DebouncedTaskRunner<>(backgroundExecutor, Duration.ZERO);
        view.getSelectFileButton().setOnAction(event -> selectAndAnalyzeFile());
    }

    private void selectAndAnalyzeFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona il riepilogo vendite CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File CSV", "*.csv"));
        File file = chooser.showOpenDialog(view.getScene().getWindow());
        if (file == null) return;

        view.showSelectedFile(file.getName());
        importRunner.runNow(
                () -> service.analyze(file.toPath()),
                view::showLoading,
                result -> view.render(presenter.present(result)),
                error -> view.showError("Errore imprevisto durante l'analisi del file CSV.")
        );
    }

    public void dispose() {
        importRunner.cancel();
    }
}
