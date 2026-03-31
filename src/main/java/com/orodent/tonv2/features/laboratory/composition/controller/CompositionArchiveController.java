package com.orodent.tonv2.features.laboratory.composition.controller;

import com.orodent.tonv2.app.navigation.LaboratoryNavigator;
import com.orodent.tonv2.features.laboratory.composition.service.CompositionArchiveService;
import com.orodent.tonv2.features.laboratory.composition.view.CompositionArchiveView;

import java.util.List;

public class CompositionArchiveController {

    private final CompositionArchiveView view;
    private final LaboratoryNavigator navigator;
    private final CompositionArchiveService service;

    public CompositionArchiveController(CompositionArchiveView view,
                                        LaboratoryNavigator navigator,
                                        CompositionArchiveService service) {
        this.view = view;
        this.navigator = navigator;
        this.service = service;

        setupActions();
        loadCompositions("");
    }

    private void setupActions() {
        view.getFilterNameField().textProperty().addListener((obs, oldValue, newValue) -> loadCompositions(newValue));
        view.getCompositionsTable().setOnMouseClicked(event -> {
            CompositionArchiveView.CompositionRow selected = view.getCompositionsTable().getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            navigator.showCreateComposition(selected.id());
        });
    }

    private void loadCompositions(String nameFilter) {
        List<CompositionArchiveView.CompositionRow> rows = service.searchProductsWithCompositions(nameFilter).stream()
                .map(product -> new CompositionArchiveView.CompositionRow(product.id(), product.code()))
                .toList();

        view.setCompositions(rows);
    }
}
