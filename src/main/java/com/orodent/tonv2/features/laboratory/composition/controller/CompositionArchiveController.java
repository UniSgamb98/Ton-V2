package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.core.ui.async.DebouncedTaskRunner;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.view.CompositionArchiveView;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.Executor;

public class CompositionArchiveController {

    private final CompositionArchiveView view;
    private final LaboratoryNavigator navigator;
    private final CompositionArchiveService service;
    private final DebouncedTaskRunner<List<CompositionArchiveView.CompositionRow>> loader;

    public CompositionArchiveController(CompositionArchiveView view,
                                        LaboratoryNavigator navigator,
                                        CompositionArchiveService service,
                                        Executor backgroundExecutor) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;
        this.loader = new DebouncedTaskRunner<>(backgroundExecutor, Duration.millis(300));

        setupActions();
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadDebounced(newValue));
        view.getCompositionsTable().setOnMouseClicked(event -> {
            CompositionArchiveView.CompositionRow selected = view.getCompositionsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateComposition(selected.id());
        });
    }

    public void loadInitialData() {
        loader.runNow(
                () -> searchCompositions(""),
                view::showLoading,
                view::showCompositions,
                error -> view.showLoadError()
        );
    }

    private void loadDebounced(String nameFilter) {
        loader.runDebounced(
                () -> searchCompositions(nameFilter),
                view::showLoading,
                view::showCompositions,
                error -> view.showLoadError()
        );
    }

    private List<CompositionArchiveView.CompositionRow> searchCompositions(String nameFilter) {
        return service.searchProductsWithCompositions(nameFilter).stream()
                .map(product -> new CompositionArchiveView.CompositionRow(product.id(), product.code()))
                .toList();
    }

    public void dispose() {
        loader.cancel();
    }
}
