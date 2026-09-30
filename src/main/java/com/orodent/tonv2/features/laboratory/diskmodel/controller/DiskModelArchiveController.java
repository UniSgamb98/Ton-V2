package com.orodent.tonv2.features.laboratory.diskmodel.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.diskmodel.service.DiskModelArchiveService;
import com.orodent.tonv2.features.laboratory.diskmodel.view.DiskModelArchiveView;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class DiskModelArchiveController {

    private final DiskModelArchiveView view;
    private final LaboratoryNavigator navigator;
    private final DiskModelArchiveService service;
    private final Executor backgroundExecutor;
    private final PauseTransition filterDebounce = new PauseTransition(Duration.millis(300));

    private Task<List<DiskModelArchiveView.DiskModelRow>> activeLoad;
    private long loadGeneration;
    private String pendingFilter = "";

    public DiskModelArchiveController(DiskModelArchiveView view,
                                      LaboratoryNavigator navigator,
                                      DiskModelArchiveService service,
                                      Executor backgroundExecutor) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.backgroundExecutor = backgroundExecutor;

        setupActions();
    }

    private void setupActions() {
        filterDebounce.setOnFinished(event -> loadDiskModels(pendingFilter, loadGeneration));
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> scheduleFilter(newValue));
        view.getDiskModelsTable().setOnMouseClicked(event -> {
            DiskModelArchiveView.DiskModelRow selected = view.getDiskModelsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateDiskModel(selected.id());
        });
    }

    public void loadInitialData() {
        long generation = beginLoad();
        loadDiskModels("", generation);
    }

    private void scheduleFilter(String nameFilter) {
        pendingFilter = nameFilter == null ? "" : nameFilter;
        beginLoad();
        filterDebounce.playFromStart();
    }

    private long beginLoad() {
        loadGeneration++;
        if (activeLoad != null) {
            activeLoad.cancel();
        }
        view.showLoading();
        return loadGeneration;
    }

    private void loadDiskModels(String nameFilter, long generation) {
        Task<List<DiskModelArchiveView.DiskModelRow>> task = new Task<>() {
            @Override
            protected List<DiskModelArchiveView.DiskModelRow> call() {
                return service.searchDiskModels(nameFilter).stream()
                        .map(model -> new DiskModelArchiveView.DiskModelRow(
                                model.id(),
                                model.code() + " (v" + model.version() + ")"
                        ))
                        .toList();
            }
        };

        activeLoad = task;
        task.setOnSucceeded(event -> {
            if (generation == loadGeneration) {
                view.showDiskModels(task.getValue());
                activeLoad = null;
            }
        });
        task.setOnFailed(event -> {
            if (generation == loadGeneration) {
                view.showLoadError();
                activeLoad = null;
            }
        });
        task.setOnCancelled(event -> {
            if (generation == loadGeneration) {
                activeLoad = null;
            }
        });

        backgroundExecutor.execute(task);
    }
}
