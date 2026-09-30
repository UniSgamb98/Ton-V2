package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.DiskModelArchiveView;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class DiskModelArchiveController {

    private final DiskModelArchiveView view;
    private final LaboratoryNavigator navigator;
    private final DiskModelArchiveService service;
    private final DebouncedTaskRunner<List<DiskModelArchiveView.DiskModelRow>> loader;

    public DiskModelArchiveController(DiskModelArchiveView view,
                                      LaboratoryNavigator navigator,
                                      DiskModelArchiveService service,
                                      Executor backgroundExecutor) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.loader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(300));

        setupActions();
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadDebounced(newValue));
        view.getDiskModelsTable().setOnMouseClicked(event -> {
            DiskModelArchiveView.DiskModelRow selected = view.getDiskModelsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateDiskModel(selected.id());
        });
    }

    public void loadInitialData() {
        loader.runNow(
                () -> searchDiskModels(""),
                view::showLoading,
                view::showDiskModels,
                error -> view.showLoadError()
        );
    }

    private void loadDebounced(String nameFilter) {
        loader.runDebounced(
                () -> searchDiskModels(nameFilter),
                view::showLoading,
                view::showDiskModels,
                error -> view.showLoadError()
        );
    }

    private List<DiskModelArchiveView.DiskModelRow> searchDiskModels(String nameFilter) {
        return service.searchDiskModels(nameFilter).stream()
                .map(model -> new DiskModelArchiveView.DiskModelRow(
                        model.id(),
                        model.code() + " (v" + model.version() + ")"
                ))
                .toList();
    }

    public void dispose() {
        loader.cancel();
    }
}
