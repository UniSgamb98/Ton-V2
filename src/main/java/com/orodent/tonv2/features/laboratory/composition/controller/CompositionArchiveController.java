package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.view.CompositionArchiveView;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class CompositionArchiveController {

    private final CompositionArchiveView view;
    private final LaboratoryNavigator navigator;
    private final CompositionArchiveService service;
    private final Executor backgroundExecutor;
    private final PauseTransition filterDebounce = new PauseTransition(Duration.millis(300));

    private Task<List<CompositionArchiveView.CompositionRow>> activeLoad;
    private long loadGeneration;
    private String pendingFilter = "";

    public CompositionArchiveController(CompositionArchiveView view,
                                        LaboratoryNavigator navigator,
                                        CompositionArchiveService service,
                                        Executor backgroundExecutor) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.backgroundExecutor = backgroundExecutor;

        setupActions();
    }

    private void setupActions() {
        filterDebounce.setOnFinished(event -> loadCompositions(pendingFilter, loadGeneration));
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> scheduleFilter(newValue));
        view.getCompositionsTable().setOnMouseClicked(event -> {
            CompositionArchiveView.CompositionRow selected = view.getCompositionsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateComposition(selected.id());
        });
    }

    public void loadInitialData() {
        long generation = beginLoad();
        loadCompositions("", generation);
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

    private void loadCompositions(String nameFilter, long generation) {
        Task<List<CompositionArchiveView.CompositionRow>> task = new Task<>() {
            @Override
            protected List<CompositionArchiveView.CompositionRow> call() {
                return service.searchProductsWithCompositions(nameFilter).stream()
                        .map(product -> new CompositionArchiveView.CompositionRow(product.id(), product.code()))
                        .toList();
            }
        };

        activeLoad = task;
        task.setOnSucceeded(event -> {
            if (generation == loadGeneration) {
                view.showCompositions(task.getValue());
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
